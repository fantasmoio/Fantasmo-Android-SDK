package com.fantasmo.sdk.views

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.PermissionChecker
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.fantasmo.sdk.network.FMApi
import com.fantasmo.sdk.utilities.QRCodeScanner
import com.fantasmo.sdk.views.debug.FMStatisticsView
import com.google.android.gms.location.*
import com.google.ar.core.Frame

class FMParkingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val TAG = "FMParkingView"
    private lateinit var arLayout: CoordinatorLayout
    private lateinit var fmARCoreView: FMARCoreView
    private lateinit var qrCodeReader: QRCodeScanner

    lateinit var fmParkingViewController: FMParkingViewProtocol
    var showStatistics = false
    var isSimulation = false
    var usesInternalLocationManager = false
    private var connected = false
    private lateinit var appSessionId: String

    private lateinit var filterRejectionTv: TextView

    private lateinit var checkParkingButton: Button

    private lateinit var fmLocationManager: FMLocationManager

    lateinit var fmStatisticsView: FMStatisticsView

    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: android.location.Location = android.location.Location("")
    private val locationInterval = 300L

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        setUpAREnvironmentOpenGL(inflater)
    }

    private fun setUpAREnvironmentOpenGL(inflater: LayoutInflater) {
        inflater.inflate(R.layout.fmparkingview, this, true)
        arLayout = getChildAt(0) as CoordinatorLayout
        fmARCoreView = FMARCoreView(arLayout, context)
        fmARCoreView.setupARSession()

        fmStatisticsView = FMStatisticsView(arLayout)
        fmLocationManager = FMLocationManager(context)

        filterRejectionTv = arLayout.findViewById(R.id.filterRejectionTextView)
    }

    fun isParkingAvailable(latitude: Double, longitude: Double, accessToken: String, onCompletion: (Boolean) -> Unit) {
        fmLocationManager.setLocation(latitude,longitude)
        val fmApi = FMApi(fmLocationManager,context,accessToken)
        fmApi.sendZoneInRadiusRequest(10, onCompletion)
        //fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING,10, onCompletion)
    }

    fun connect(accessToken: String, appSessionId: String) {
        this.appSessionId = appSessionId
        connected = true

        qrCodeReader = QRCodeScanner(fmParkingViewController,fmQrScanningViewController,fmLocalizingViewProtocol)

        fmLocationManager.isSimulation = isSimulation

        if (usesInternalLocationManager) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                getLocation()
            } else {
                Log.e(TAG, "Your GPS seems to be disabled")
            }
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
        fmARCoreView.fmLocationManager = fmLocationManager
        fmARCoreView.qrCodeReader = qrCodeReader
        fmARCoreView.filterRejectionTv = filterRejectionTv

        val statistics = arLayout.findViewWithTag<ConstraintLayout>("StatisticsView")
        if (showStatistics) {
            statistics.visibility = View.VISIBLE
        } else {
            statistics.visibility = View.GONE
        }

        checkParkingButton = arLayout.findViewById(R.id.checkParkingButton)
        checkParkingButton.setOnClickListener {
            Log.d(TAG, "CheckPark Pressed")

            fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, 10) {
                Toast.makeText(
                    context.applicationContext,
                    "Is Zone In Radius Response: $it",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        startQRScanning()
    }

    private fun startQRScanning() {
        fmQrScanningViewController.didStartQRScanning()
        fmParkingViewController.fmParkingViewDidStartQRScanning()
    }

    /**
     * Gets system location through the app context
     * Then checks if it has permission to ACCESS_FINE_LOCATION
     * Also includes Callback for Location updates.
     * Sets the [fmLocationManager.currentLocation] coordinates used to localize.
     */
    private fun getLocation() {
        if ((context.let {
                PermissionChecker.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED)) {
            Log.e(TAG, "Location permission needs to be granted.")
        } else {
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.smallestDisplacement = 1f
            locationRequest.fastestInterval = locationInterval
            locationRequest.interval = locationInterval

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    currentLocation = locationResult.lastLocation
                    //Set SDK Location
                    fmLocationManager.setLocation(
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
                    fmStatisticsView.updateLocation(
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
                    Log.d(TAG, "onLocationResult: ${locationResult.lastLocation}")
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()!!
            )
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
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
                "FMLocationManager not initialized: Please make sure connect() was invoked before updateLocation"
            )
        }
    }

    fun disconnect() {
        if (connected) {
            connected = false
            if (fmARCoreView.localizing) {
                fmLocationManager.stopUpdatingLocation()
            }
            if (fmARCoreView.anchored) {
                fmLocationManager.unsetAnchor()
            }
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


    private val fmQrScanningViewController: FMQRScanningViewProtocol =
        object : FMQRScanningViewProtocol {
            override fun didStartQRScanning() {
                fmARCoreView.anchorIsChecked = true
                qrCodeReader.qrCodeReaderEnabled = true
                qrCodeReader.state = QRCodeScanner.State.IDLE
            }

            override fun didScanQRCode(result: String) {
                // Display result and approve or not the QR Code Scanned

            }

            override fun didStopQRScanning(){

            }
        }

    private val fmLocalizingViewProtocol: FMLocalizingViewProtocol =
        object : FMLocalizingViewProtocol {
            override fun didStartLocalizing() {
                Log.d(TAG, "LocalizeToggle Enabled")
                fmARCoreView.localizing = true
                // Start getting location updates
                fmLocationManager.startUpdatingLocation(appSessionId, true)
            }
        }

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                fmParkingViewController.fmParkingView(error, metadata)
                Log.d(TAG, error.message.toString())
                /*
                (context as Activity).runOnUiThread {
                    lastResultTv.text = error.message.toString()
                }
                 */
            }

            override fun locationManager(result: FMLocationResult) {
                fmParkingViewController.fmParkingView(result)
                Log.d(TAG, result.confidence.toString())
                Log.d(TAG, result.location.toString())
                (context as Activity).runOnUiThread {
                    fmStatisticsView.updateResult(result)
                }
            }

            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
                fmParkingViewController.fmParkingView(didRequestBehavior)
                //behaviorReceived = System.nanoTime()
                Log.d(TAG, "FrameFilterResult " + didRequestBehavior.displayName)
                val stringResult = didRequestBehavior.displayName
                (context as Activity).runOnUiThread {
                    filterRejectionTv.text = stringResult
                    if (filterRejectionTv.visibility == View.GONE) {
                        filterRejectionTv.visibility = View.VISIBLE
                    }
                }
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
}