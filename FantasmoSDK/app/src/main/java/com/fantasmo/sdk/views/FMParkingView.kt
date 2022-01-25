package com.fantasmo.sdk.views

import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.*
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMPose
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.fantasmo.sdk.network.FMApi
import com.fantasmo.sdk.utilities.DeviceLocationListener
import com.fantasmo.sdk.utilities.DeviceLocationManager
import com.fantasmo.sdk.utilities.QRCodeScanner
import com.fantasmo.sdk.utilities.QRCodeScannerListener
import com.google.ar.core.Frame

/**
 * Manager of the ARCore session. Provides a camera preview with AR capabilities when not connected.
 * When connected with `connect(sessionId: String)`, starts a QRScanning session and after when a QR Code
 * is accepted, starts a Localization session.
 */
class FMParkingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val TAG = "FMParkingView"
    private var arLayout: CoordinatorLayout
    private var fmARCoreView: FMARCoreView

    lateinit var fmParkingViewController: FMParkingViewProtocol

    /**
     * Controls the debug UI. When set to `true`, the display will show multiple statistics about the session
     */
    var showStatistics = false

    /**
     * Controls whether the Session uses simulation Mode or real Mode.
     * Default behavior is to be set on `false`
     */
    var isSimulation = false

    /**
     * Controls QR code scanner. When set to `true` enables a QR code scanning session.
     * Default behavior is to be set on `false`
     */
    var enableQRCodeScanner = false

    /**
     * Controls whether this class uses its own internal LocationManager to automatically receive location updates. Default is `true`.
     * When set to `false` it is expected that location updates will be manually provided via the `updateLocation()` method.
     */
    var usesInternalLocationManager = true

    /**
     * Token used to provide access to the Fantasmo API
     */
    lateinit var accessToken: String

    // App Session Id supplied by the SDK client
    private lateinit var appSessionId: String
    // App Session Tags supplied by the SDK client
    private var appSessionTags: List<String>? = null

    private lateinit var fmLocationManager: FMLocationManager
    private var fmSessionStatisticsView: FMSessionStatisticsView

    // Default UI for the QR view
    private lateinit var fmQRScanningView: FMQRScanningView
    private lateinit var qrCodeReader: QRCodeScanner

    // Default UI for the Localizing view
    private lateinit var fmLocalizingView: FMLocalizingView

    // Internal Device Location Manager to set Location
    private lateinit var internalLocationManager: DeviceLocationManager

    /**
     * AR view initializer
     * This is where the ARCore is instantiated and creates the camera preview and all the logics
     * behind ARCore session
     */
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.fmparkingview, this, true)
        arLayout = getChildAt(0) as CoordinatorLayout
        fmARCoreView = FMARCoreView(arLayout, context)
        fmARCoreView.setupARSession()
        fmSessionStatisticsView = FMSessionStatisticsView(arLayout)
        setupFMLocationManager()
    }

    private fun setupFMLocationManager() {
        fmLocationManager = FMLocationManager(context)
    }

    /**
     * Check if there's an available parking space near a supplied Location.
     *
     * This method should be used to determine whether or not you should try to park and localize with Fantasmo.
     * The boolean value passed to the completion block tells you if there is an available parking space within the
     * acceptable radius of the supplied location. If `true`, you should construct a `FMParkingView` and
     * attempt to localize. If `false` you should resort to other options.
     *
     * @param location the Location to check
     * @param onCompletion block with a boolean result
     */
    fun isParkingAvailable(
        location: Location,
        onCompletion: (Boolean) -> Unit
    ) {
        if (!DeviceLocationManager.isValidLatLng(location.latitude, location.longitude)) {
            onCompletion(false)
            Log.e(TAG, "Invalid Coordinates")
            return
        }

        val verticalAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            location.verticalAccuracyMeters
        } else {
            0.0f
        }
        val locationFantasmo =
            com.fantasmo.sdk.models.Location(
                location.altitude,
                location.time,
                location.accuracy,
                verticalAccuracy,
                Coordinate(location.latitude, location.longitude)
            )
        val fmApi = FMApi(context, accessToken)
        fmApi.sendInitializationRequest(locationFantasmo, onCompletion) {
            if (it.message != null) {
                Log.e(TAG, it.message)
            }
            onCompletion(false)
        }
    }

    enum class State {
        IDLE,
        QRSCANNING,
        LOCALIZING
    }

    private var state = State.IDLE

    /**
     * Designated initializer.
     * The `sessionId` parameter allows you to associate localization results with your own session identifier.
     * Typically this would be a UUID string, but it can also follow your own format. For example, a scooter parking
     * session might involve multiple localization attempts. For analytics and billing purposes this identifier allows
     * you to link a set of attempts with a single parking session.
     *
     * The `sessionTags` parameter is an optional list of tags for the parking session. This parameter can be used to
     * label and group parking sessions that have something in common. For example parking sessions that take place
     * in the same city might have the city's name as a tag. These are used for analytics purposes only and will be
     * included in usage reports. Each tag must be a string and there is no limit to the number of tags a session can have.
     *
     * @param sessionId an identifier for the parking session
     * @param sessionTags an optional list of tags for the parking session
     */
    fun connect(sessionId: String, sessionTags: List<String>?) {
        this.visibility = View.VISIBLE

        appSessionId = sessionId

        appSessionTags = sessionTags

        fmQRScanningView = FMQRScanningView(context, arLayout, this)

        fmLocalizingView = FMLocalizingView(arLayout, this)

        qrCodeReader = QRCodeScanner(
            fmParkingViewController,
            fmQrScanningViewController,
            qrCodeScannerListener
        )

        fmLocationManager.isSimulation = isSimulation

        fmARCoreView.arSessionListener = arSessionListener
        fmARCoreView.connected = true

        val statistics = arLayout.findViewWithTag<ConstraintLayout>("StatisticsView")
        if (showStatistics) {
            statistics.visibility = View.VISIBLE
            fmSessionStatisticsView.reset()
        } else {
            statistics.visibility = View.GONE
        }

        if (enableQRCodeScanner){
            startQRScanning()
        }
        else {
            state = State.QRSCANNING
            startLocalizing()
        }
    }

    /**
     * Resets the session to a normal ARSession.
     * Closes Localizing and QR Scanning Sessions.
     * Stops Location Updates if internal Location Manager is in use
     */
    fun dismiss() {
        if (state == State.LOCALIZING || state == State.QRSCANNING) {
            state = State.IDLE

            fmARCoreView.connected = false
            fmLocationManager.stopUpdatingLocation()
            if (usesInternalLocationManager) {
                if (this::internalLocationManager.isInitialized) {
                    internalLocationManager.stopLocationUpdates()
                }
            }

            if (fmARCoreView.isAnchored()) {
                fmARCoreView.unsetAnchor()
                fmLocationManager.unsetAnchor()
            }
            fmQRScanningView.hide()
            fmLocalizingView.hide()
        }
        fmSessionStatisticsView.reset()
        this.visibility = View.GONE
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
     * Listener for the Default Fantasmo QR Scanning View.
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
     * @param customQRScanningView custom FMQRScanningViewProtocol class
     */
    fun registerQRScanningViewController(customQRScanningView: FMQRScanningViewProtocol) {
        Log.d(TAG, "QRScanningView Registered")
        this.fmQrScanningViewController = customQRScanningView
    }

    /**
     * Presents the default or custom registered QR scanning view controller and starts observing QR codes in the ARSession.
     * This method is only intended to be called while idle.
     */
    private fun startQRScanning() {
        if (state != State.IDLE) {
            return
        }
        state = State.QRSCANNING
        FMUtility.setFalse()
        qrCodeReader.startQRScanner()
        fmQrScanningViewController.didStartQRScanning()
        fmParkingViewController.fmParkingViewDidStartQRScanning()
    }

    /**
     * Listener for the Default Fantasmo Localizing View.
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
     * @param customLocalizingView custom FMLocalizingViewProtocol class
     */
    fun registerLocalizingViewController(customLocalizingView: FMLocalizingViewProtocol) {
        Log.d(TAG, "LocalizingView Registered")
        this.fmLocalizingViewController = customLocalizingView
    }

    /**
     * Presents the default or custom registered localizing view controller and starts the localization process.
     * This method is only intended to be called while QR scanning, it performs transition to the localization view.
     */
    private fun startLocalizing() {
        if (state != State.QRSCANNING) {
            return
        }
        fmQrScanningViewController.didStopQRScanning()
        fmParkingViewController.fmParkingViewDidStopQRScanning()

        state = State.LOCALIZING

        if (usesInternalLocationManager) {
            internalLocationManager = DeviceLocationManager(context, deviceLocationListener)
        }

        // Connect the FMLocationManager to Fantasmo SDK
        fmLocationManager.connect(accessToken, fmLocationListener)
        // Start getting location updates
        fmLocationManager.startUpdatingLocation(appSessionId, appSessionTags)

        fmLocalizingViewController.didStartLocalizing()
        fmParkingViewController.fmParkingViewDidStartLocalizing()
    }

    /**
     * Provide a manually-entered QR code string and proceed to localization.
     *
     * If validation of the entered string is needed, it should be done in `fmparkingView(_:didEnterQRCodeString:continueBlock:)`
     * of your `FMParkingView`. This method does nothing if the QR-code scanner is inactive, or if another code is being
     * validated.
     */
    fun enterQRCode(string: String) {
        if (state != State.QRSCANNING) {
            return
        }
        // Set an AR anchor now
        fmARCoreView.startAnchor()
        fmParkingViewController.fmParkingView(string){
            if (it) {
                Log.d(TAG, "QR CODE ACCEPTED")
                qrCodeScannerListener.deployLocalizing()
            } else {
                Log.d(TAG, "QR CODE REFUSED")
                qrCodeScannerListener.deployQRScanning()
            }
        }
    }

    /**
     * Allows host apps to manually provide a location update.
     * This method can only be used when usesInternalLocationManager is set to false.
     * @param location the device current location.
     */
    fun updateLocation(location: Location) {
        if (!usesInternalLocationManager) {
            // Prevents fmLocationManager lateinit property not initialized
            if (this::fmLocationManager.isInitialized) {
                //Set SDK Location
                fmLocationManager.setLocation(
                    location
                )
                fmSessionStatisticsView.updateLocation(location.latitude, location.longitude)
            } else {
                Log.e(
                    TAG,
                    "FMLocationManager not initialized: Please make sure connect() was invoked before updateLocation"
                )
            }
        }
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
                    fmSessionStatisticsView.updateResult(result)
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
                    fmSessionStatisticsView.updateState(didChangeState)
                }
            }

            override fun locationManager(
                didUpdateFrame: Frame,
                info: AccumulatedARCoreInfo,
                rejections: FrameFilterRejectionStatistics
            ) {
                (context as Activity).runOnUiThread {
                    fmSessionStatisticsView.updateStats(didUpdateFrame, info, rejections)
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

            override fun qrCodeScanned() {
                fmARCoreView.startAnchor()
            }

            override fun deployQRScanning() {
                state = State.IDLE
                fmLocationManager.unsetAnchor()
                fmARCoreView.unsetAnchor()
                startQRScanning()
            }
        }

    /**
     * Listener of the ARSession.
     */
    private var arSessionListener: FMARSessionListener =
        object : FMARSessionListener {
            override fun localize(frame: Frame) {
                // If localizing, pass the current AR frame to the location manager
                if (state == State.LOCALIZING) {
                    fmLocationManager.session(frame)
                }
            }

            override fun anchored(frame: Frame): Boolean {
                fmLocationManager.setAnchor(frame)
                return true
            }

            override fun qrCodeScan(frame: Frame) {
                // If qrScanning, pass the current AR frame to the qrCode reader
                if (state == State.QRSCANNING) {
                    qrCodeReader.processImage(frame)
                }
            }

            override fun anchorDelta(frame: Frame): FMPose? {
                return fmLocationManager.anchorFrame?.let { anchorFrame ->
                    FMUtility.anchorDeltaPoseForFrame(
                        frame,
                        anchorFrame
                    )
                }
            }
        }

    /**
     * Listener of the internal Location Updates.
     */
    private val deviceLocationListener: DeviceLocationListener =
        object : DeviceLocationListener {
            override fun onLocationUpdate(locationResult: Location) {
                //Set SDK Location
                fmLocationManager.setLocation(
                    locationResult
                )
                fmSessionStatisticsView.updateLocation(
                    locationResult.latitude,
                    locationResult.longitude
                )
            }
        }
}