package com.fantasmo.sdk.views

import android.Manifest
import android.annotation.SuppressLint
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
import com.fantasmo.sdk.utilities.QRCodeScanner
import com.fantasmo.sdk.views.debug.FMStatisticsView
import com.google.android.gms.location.*
import com.google.android.gms.maps.MapView
import com.google.ar.core.Frame

class FMParkingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val TAG = "FMParkingView"
    private lateinit var arLayout: CoordinatorLayout
    private lateinit var googleMapsView: GoogleMapsView
    private lateinit var fmARCoreView: FMARCoreView
    private lateinit var qrCodeReader: QRCodeScanner

    lateinit var fmQrScanningViewController: FMQRScanningViewProtocol
    var showStatistics = false
    var isSimulation = false
    var usesInternalLocationManager = false
    private var connected = false

    private lateinit var filterRejectionTv: TextView

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var localizeToggleButton: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var anchorToggleButton: Switch

    private lateinit var checkParkingButton: Button

    private lateinit var fmLocationManager: FMLocationManager

    lateinit var fmStatisticsView: FMStatisticsView

    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: android.location.Location = android.location.Location("")
    private val locationInterval = 300L

    lateinit var googleMapView: MapView
    private lateinit var mapButton: Button

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

        filterRejectionTv = arLayout.findViewById(R.id.filterRejectionTextView)
    }

    fun connect(accessToken: String, appSessionId: String) {
        connected = true
        googleMapsView = GoogleMapsView(context)
        qrCodeReader = QRCodeScanner(fmQrScanningViewController)

        fmLocationManager = FMLocationManager(context)
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

        localizeToggleButton = arLayout.findViewById(R.id.localizeToggle)
        localizeToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d(TAG, "LocalizeToggle Enabled")
                fmARCoreView.localizing = true
                // Start getting location updates
                fmLocationManager.startUpdatingLocation(appSessionId, true)
            } else {
                Log.d(TAG, "LocalizeToggle Disabled")
                fmARCoreView.localizing = false
                // Stop getting location updates
                fmLocationManager.stopUpdatingLocation()
            }
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

        googleMapView = arLayout.findViewById(R.id.mapView)
        googleMapsView.googleMapView = googleMapView
        mapButton = arLayout.findViewById(R.id.mapButton)
        mapButton.setOnClickListener {
            if (googleMapView.visibility == View.VISIBLE) {
                googleMapView.visibility = View.GONE
            } else {
                googleMapView.visibility = View.VISIBLE
            }
        }

        anchorToggleButton = arLayout.findViewById(R.id.anchorToggle)
        anchorToggleButton.setOnCheckedChangeListener { _, isChecked ->
            fmARCoreView.anchorIsChecked = isChecked
            googleMapsView.updateAnchor(fmARCoreView.anchorIsChecked)
            if (!isChecked) {
                fmARCoreView.anchored = false
                Log.d(TAG, "AnchorToggle Disabled")
                //anchorDeltaTv.visibility = View.GONE
                fmLocationManager.unsetAnchor()
                googleMapsView.unsetAnchor()
            }
        }

        startQRScanning()
    }

    private fun startQRScanning() {
        fmARCoreView.anchorIsChecked = true
        googleMapsView.updateAnchor(fmARCoreView.anchorIsChecked)
        qrCodeReader.qrCodeReaderEnabled = true
        fmQrScanningViewController.didStartQRScanning()
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
                localizeToggleButton.isChecked = false
                fmLocationManager.stopUpdatingLocation()
            }
            if (fmARCoreView.anchored) {
                anchorToggleButton.isChecked = false
                googleMapsView.unsetAnchor()
                fmLocationManager.unsetAnchor()
            }
        }
    }

    fun onResume() {
        fmARCoreView.onResume()
        if (connected) {
            googleMapsView.onResume()
        }
    }

    fun onPause() {
        googleMapsView.onPause()
        fmARCoreView.onPause()
    }

    fun onDestroy() {
        fmARCoreView.onDestroy()
        googleMapsView.onDestroy()
    }

    fun onStart() {
        googleMapsView.onStart()
    }

    fun onStop() {
        googleMapsView.onStop()
    }

    fun onLowMemory() {
        googleMapsView.onLowMemory()
    }

    fun initGoogleMap(savedInstanceState: Bundle?) {
        googleMapsView.initGoogleMap(savedInstanceState)
        googleMapsView.onStart()
    }

    fun onSaveInstanceStateApp(outState: Bundle) {
        googleMapsView.onSaveInstanceState(outState)
    }


     /*=
        object : FMQRScanningViewProtocol {
            override fun didStartQRScanning() {
                Log.d(TAG, "QR Code Reader Enabled")
                if (qrCodeResultTv.visibility == View.GONE) {
                    qrCodeResultTv.visibility = View.VISIBLE
                }
                if (qrOverlay.visibility == View.GONE) {
                    qrOverlay.visibility = View.VISIBLE
                }
            }

            override fun didScanQRCode(result: String) {
                // Display result and approve or not the QR Code Scanned
                qrCodeResultTv.text = result
                if(qrOverlay.visibility == View.VISIBLE){
                    qrOverlay.visibility = View.GONE
                }
            }

            override fun didStopQRScanning(){
                // Unset Anchor and begin Localizing
                fmARCoreView.anchored = false
                Log.d(TAG, "Anchor Disabled Start Localizing")
                // anchorDeltaTv.visibility = View.GONE
                fmLocationManager.unsetAnchor()
                googleMapsView.unsetAnchor()
            }
        }
        */

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                Log.d(TAG, error.message.toString())
                /*
                (context as Activity).runOnUiThread {
                    lastResultTv.text = error.message.toString()
                }
                 */
            }

            override fun locationManager(result: FMLocationResult) {
                Log.d(TAG, result.confidence.toString())
                Log.d(TAG, result.location.toString())
                (context as Activity).runOnUiThread {
                    fmStatisticsView.updateResult(result)
                    googleMapsView.addCorrespondingMarkersToMap(result)
                }
            }

            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
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