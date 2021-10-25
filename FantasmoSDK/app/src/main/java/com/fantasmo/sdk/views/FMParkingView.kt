package com.fantasmo.sdk.views

import android.app.Activity
import android.content.Context
import android.location.Location
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout

import com.fantasmo.sdk.*
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMPose
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.fantasmo.sdk.network.FMApi
import com.fantasmo.sdk.utilities.DeviceLocationListener
import com.fantasmo.sdk.utilities.DeviceLocationManager
import com.fantasmo.sdk.utilities.QRCodeScanner
import com.fantasmo.sdk.utilities.QRCodeScannerListener
import com.fantasmo.sdk.views.debug.FMStatisticsView

import com.google.ar.core.Frame
import com.google.ar.core.TrackingState

class FMParkingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val TAG = "FMParkingView"
    private var arLayout: CoordinatorLayout
    private var fmARCoreView: FMARCoreView

    lateinit var fmParkingViewController: FMParkingViewProtocol

    var showStatistics = false
    var isSimulation = false
    /**
     * Controls whether this class uses its own internal LocationManager to automatically receive location updates. Default is true.
     * When set to false it is expected that location updates will be manually provided via the updateLocation() method.
     */
    var usesInternalLocationManager = true

    private var connected = false
    lateinit var appSessionId: String
    lateinit var accessToken: String

    private lateinit var fmLocationManager: FMLocationManager
    private var fmStatisticsView: FMStatisticsView

    //Default UI for the QR view
    private lateinit var fmQRScanningView: FMQRScanningView
    private lateinit var qrCodeReader: QRCodeScanner

    //Default UI for the Localizing view
    private lateinit var fmLocalizingView: FMLocalizingView

    private var currentLocation: Location = Location("")

    // Internal Device Location Manager to set Location
    private lateinit var deviceLocationManager: DeviceLocationManager

    // Default radius in meters used when checking parking availability via `isParkingAvailable()`.
    private var defaultParkingAvailabilityRadius: Int = 50

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.fmparkingview, this, true)
        arLayout = getChildAt(0) as CoordinatorLayout
        fmARCoreView = FMARCoreView(arLayout, context)
        fmARCoreView.setupARSession()
        fmStatisticsView = FMStatisticsView(arLayout)
        setupFMLocationManager()
    }

    private fun setupFMLocationManager() {
        fmLocationManager = FMLocationManager(context)
    }

    /**
     * Check if there's an available parking space near a supplied CLLocation.
     * @param latitude: the latitude of the Location to check
     * @param longitude: the longitude of the Location to check
     * @param onCompletion: block with a boolean result
     * This method should be used to determine whether or not you should try to park and localize with Fantasmo.
     * The boolean value passed to the completion block tells you if there is an available parking space within the
     * acceptable radius of the supplied location. If `true`, you should construct a `FMParkingView` and
     * attempt to localize. If `false` you should resort to other options.
     */
    fun isParkingAvailable(
        latitude: Double,
        longitude: Double,
        onCompletion: (Boolean) -> Unit
    ) {
        val radius = defaultParkingAvailabilityRadius
        val fmApi = FMApi(fmLocationManager, context, accessToken)
        fmApi.sendZoneInRadiusRequest(latitude, longitude, radius, onCompletion)
    }

    fun present() {
        connected = true

        fmQRScanningView = FMQRScanningView(
            arLayout.findViewById(R.id.fmQRView),
            arLayout.findViewById(R.id.qrCodeResultTextView)
        )

        fmLocalizingView = FMLocalizingView(
            arLayout.findViewById(R.id.fmLocalizeView),
            arLayout.findViewById(R.id.filterRejectionTextView)
        )

        qrCodeReader = QRCodeScanner(
            fmParkingViewController,
            fmQrScanningViewController,
            qrCodeScannerListener
        )

        fmLocationManager.isSimulation = isSimulation

        if (usesInternalLocationManager) {
            deviceLocationManager = DeviceLocationManager(context, deviceLocationListener)
        }

        // Connect the FMLocationManager from Fantasmo SDK
        if (fmLocationListener == null) {
            Log.d(TAG, "LocationListener is null")
        } else {
            fmLocationManager.connect(
                accessToken,
                fmLocationListener
            )
        }
        fmARCoreView.arSessionListener = arSessionListener
        fmARCoreView.connected = true

        val statistics = arLayout.findViewWithTag<ConstraintLayout>("StatisticsView")
        if (showStatistics) {
            statistics.visibility = View.VISIBLE
        } else {
            statistics.visibility = View.GONE
        }

        startQRScanning()
    }

    /**
     * Allows host apps to manually provide a location update.
     * @param latitude: the device's current latitude.
     * @param longitude: the device's current longitude.
     * This method can only be used when usesInternalLocationManager is set to false.
     */
    fun updateLocation(latitude: Double, longitude: Double) {
        if(!usesInternalLocationManager){
            // Prevents fmLocationManager lateinit property not initialized
            if (this::fmLocationManager.isInitialized) {
                //Set SDK Location
                fmLocationManager.setLocation(
                    latitude,
                    longitude
                )
                fmStatisticsView.updateLocation(latitude, longitude)
            } else {
                Log.e(
                    TAG,
                    "FMLocationManager not initialized: Please make sure present() was invoked before updateLocation"
                )
            }
        }
    }

    fun disconnect() {
        if (connected) {
            connected = false
            if (fmARCoreView.localizing) {
                fmARCoreView.connected = false
                fmLocationManager.stopUpdatingLocation()
            }
            if (fmARCoreView.anchored) {
                fmARCoreView.anchored = false
                fmLocationManager.unsetAnchor()
            }
            fmQRScanningView.hide()
            fmLocalizingView.hide()
        }
    }

    fun onResume() {
        fmARCoreView.onResume()
    }

    fun onPause() {
        fmARCoreView.onPause()
    }

    fun onDestroy() {
        fmARCoreView.onDestroy()
    }

    /**
     * Default Listener for the Fantasmo QR Scanning View.
     */
    private var fmQrScanningViewController: FMQRScanningViewProtocol =
        object : FMQRScanningViewProtocol {
            override fun didStartQRScanning() {
                fmQRScanningView.display()
            }

            override fun didScanQRCode(result: String) {
                fmQRScanningView.displayQRCodeResult(result)
            }

            override fun didStopQRScanning() {
                fmQRScanningView.hide()
            }
        }

    /**
     * Registers a custom view controller class to present and use when scanning QR codes.
     * @param customQRScanningView: custom FMQRScanningViewProtocol class
     */
    fun registerQRScanningViewController(customQRScanningView: FMQRScanningViewProtocol) {
        Log.d(TAG, "QRScanningView Registered")
        this.fmQrScanningViewController = customQRScanningView
    }

    /**
     * Presents the default or custom registered QR scanning view controller and starts observing QR codes in the ARSession.
     */
    private fun startQRScanning() {
        fmARCoreView.anchorIsChecked = true
        fmARCoreView.anchored = false
        qrCodeReader.qrCodeReaderEnabled = true
        qrCodeReader.state = QRCodeScanner.State.IDLE
        fmQrScanningViewController.didStartQRScanning()
        fmParkingViewController.fmParkingViewDidStartQRScanning()
    }

    /**
     * Default Listener for the Fantasmo Localizing View.
     */
    private var fmLocalizingViewController: FMLocalizingViewProtocol =
        object : FMLocalizingViewProtocol {
            override fun didStartLocalizing() {
                fmLocalizingView.display()
            }

            override fun didRequestLocalizationBehavior(behavior: FMBehaviorRequest) {
                fmLocalizingView.displayFilterResult(behavior)
            }

            override fun didReceiveLocalizationResult(result: FMLocationResult) {}

            override fun didReceiveLocalizationError(error: ErrorResponse, errorMetadata: Any?) {}
        }

    /**
     * Registers a custom view controller type to present and use when localizing.
     * @param customLocalizingView: custom FMLocalizingViewProtocol class
     */
    fun registerLocalizingViewController(customLocalizingView: FMLocalizingViewProtocol) {
        Log.d(TAG, "LocalizingView Registered")
        this.fmLocalizingViewController = customLocalizingView
    }

    /**
     * Presents the default or custom registered localizing view controller and starts the localization process.
     */
    private fun startLocalizing() {
        fmQrScanningViewController.didStopQRScanning()
        fmParkingViewController.fmParkingViewDidStopQRScanning()

        fmARCoreView.localizing = true
        // Start getting location updates
        fmLocationManager.startUpdatingLocation(appSessionId, true)

        fmLocalizingViewController.didStartLocalizing()
        fmParkingViewController.fmParkingViewDidStartLocalizing()
    }

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(result: FMLocationResult) {
                fmParkingViewController.fmParkingView(result)
                fmLocalizingViewController.didReceiveLocalizationResult(result)
                (context as Activity).runOnUiThread {
                    fmStatisticsView.updateResult(result)
                }
            }

            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
                fmParkingViewController.fmParkingView(didRequestBehavior)
                fmLocalizingViewController.didRequestLocalizationBehavior(didRequestBehavior)
            }

            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                fmParkingViewController.fmParkingView(error, metadata)
                fmLocalizingViewController.didReceiveLocalizationError(error, metadata)
            }

            override fun locationManager(didChangeState: FMLocationManager.State) {
                (context as Activity).runOnUiThread {
                    fmStatisticsView.updateState(didChangeState)
                }
            }

            override fun locationManager(
                didUpdateFrame: Frame,
                info: AccumulatedARCoreInfo,
                rejections: FrameFilterRejectionStatistics
            ) {
                (context as Activity).runOnUiThread {
                    fmStatisticsView.updateStats(didUpdateFrame, info, rejections)
                }
            }
        }

    /**
     * Listener for the QR Code results.
     */
    private var qrCodeScannerListener: QRCodeScannerListener =
        object : QRCodeScannerListener {
            override fun deployLocalizing() {
                startLocalizing()
            }

            override fun deployQRScanning() {
                startQRScanning()
            }
        }

    /**
     * Listener of the ARSession.
     */
    private var arSessionListener: FMARSessionListener =
        object : FMARSessionListener {
            override fun localize(frame: Frame) {
                // Localize current frame if not already localizing
                if (fmLocationManager.state == FMLocationManager.State.LOCALIZING) {
                    fmLocationManager.localize(frame)
                }
            }

            override fun anchored(frame: Frame): Boolean {
                var anchored = false
                frame.let {
                    if (frame.camera.trackingState == TrackingState.TRACKING) {
                        fmLocationManager.setAnchor(it)
                        anchored = true
                    } else {
                        Toast.makeText(
                            context.applicationContext,
                            "Anchor can't be set because tracking state is not correct, please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return anchored
            }

            override fun qrCodeScannerState(): Boolean {
                return qrCodeReader.qrCodeReaderEnabled
            }

            override fun qrCodeScan(frame: Frame) {
                // Only read frame if the qrCodeReader is enabled and only if qrCodeReader is in reading mode
                if (qrCodeReader.qrCodeReaderEnabled && qrCodeReader.state == QRCodeScanner.State.IDLE) {
                    Log.d(TAG, "QR Code Scanning")
                    frame.let { qrCodeReader.processImage(it) }
                }
            }

            override fun anchorDelta(frame: Frame): FMPose? {
                return frame.let { frame2 ->
                    fmLocationManager.anchorFrame?.let { anchorFrame ->
                        FMUtility.anchorDeltaPoseForFrame(
                            frame2,
                            anchorFrame
                        )
                    }
                }
            }
        }

    /**
     * Listener of the internal Location Updates.
     */
    private val deviceLocationListener: DeviceLocationListener =
        object : DeviceLocationListener {
            override fun onLocationUpdate(locationResult: Location) {
                currentLocation = locationResult
                //Set SDK Location
                fmLocationManager.setLocation(
                    currentLocation.latitude,
                    currentLocation.longitude
                )
                fmStatisticsView.updateLocation(
                    currentLocation.latitude,
                    currentLocation.longitude
                )
            }
        }
}