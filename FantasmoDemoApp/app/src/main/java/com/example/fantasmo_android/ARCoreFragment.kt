package com.example.fantasmo_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.views.FMParkingView
import com.fantasmo.sdk.views.FMParkingViewProtocol

import com.google.android.gms.location.*
import com.google.android.gms.maps.MapView
import com.google.ar.core.*
import java.util.*

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class ARCoreFragment : Fragment() {
    private val TAG = "ARCoreFragment"

    private lateinit var currentView: View

    private lateinit var fmParkingView: FMParkingView

    private lateinit var controlsLayout: ConstraintLayout

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var simulationModeToggle: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showDebugStatsToggle: Switch

    private lateinit var endRideButton: Button
    private lateinit var exitButton: Button

    private lateinit var mapButton: Button
    private lateinit var googleMapView: MapView
    private lateinit var googleMapsManager: GoogleMapsManager

    private lateinit var qrCodeResultTv: TextView
    private lateinit var qrOverlay: ConstraintLayout

    private lateinit var filterRejectionTv: TextView

    private var behaviorReceived = 0L
    private var n2s = 1_000_000_000L
    private val behaviorThreshold = 1L
    private var firstBehavior = false

    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: android.location.Location = android.location.Location("")
    private val locationInterval = 300L

    private val usesInternalLocationManager = true

    private val accessToken = "API_KEY"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.arcore_fragment, container, false)

        controlsLayout = currentView.findViewById(R.id.controlsLayout)

        fmParkingView = currentView.findViewById(R.id.fmParkingView)
        fmParkingView.fmParkingViewController = fmParkingViewController
        fmParkingView.appSessionId = UUID.randomUUID().toString()
        fmParkingView.accessToken = accessToken

        // Enable simulation mode to test purposes with specific location
        // depending on which SDK flavor it's being used (Paris, Munich, Miami)
        simulationModeToggle = currentView.findViewById(R.id.simulationModeToggle)
        simulationModeToggle.setOnCheckedChangeListener { _, checked ->
            fmParkingView.isSimulation = checked
        }

        // Enable Debug Mode to display session statistics
        showDebugStatsToggle = currentView.findViewById(R.id.showDebugStatsToggle)
        showDebugStatsToggle.setOnCheckedChangeListener { _, checked ->
            fmParkingView.showStatistics = checked
        }

        // Enable internal Location Manager
        fmParkingView.usesInternalLocationManager = usesInternalLocationManager

        filterRejectionTv = currentView.findViewById(R.id.filterRejectionTextView)

        googleMapView = currentView.findViewById(R.id.mapView)
        googleMapsManager = GoogleMapsManager(requireActivity(), googleMapView)
        googleMapsManager.initGoogleMap(savedInstanceState)

        mapButton = currentView.findViewById(R.id.mapButton)
        mapButton.setOnClickListener {
            if (googleMapView.visibility == View.VISIBLE) {
                googleMapView.visibility = View.GONE
            } else {
                googleMapView.visibility = View.VISIBLE
            }
        }

        exitButton = currentView.findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            handleExitButton()
        }

        val latitude = 52.50578283943285
        val longitude = 13.378954977173915
        endRideButton = currentView.findViewById(R.id.endRideButton)
        endRideButton.setOnClickListener {
            fmParkingView.isParkingAvailable(latitude, longitude) {
                if (it) {
                    startParkingFlow()
                } else {
                    Toast.makeText(
                        context?.applicationContext,
                        "Parking not available near your location.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        qrCodeResultTv = currentView.findViewById(R.id.qrCodeResultTextView)
        qrOverlay = currentView.findViewById(R.id.qrOverlay) as ConstraintLayout

        return currentView
    }

    private fun startParkingFlow() {
        if (fmParkingView.visibility == View.GONE) {
            mapButton.visibility = View.VISIBLE
            fmParkingView.visibility = View.VISIBLE
            fmParkingView.present()
            useOwnLocationProvider()
            controlsLayout.visibility = View.GONE
            exitButton.visibility = View.VISIBLE
        }
    }

    /**
     * Example on how to override the internal location Manager
     */
    private fun useOwnLocationProvider() {
        if(!usesInternalLocationManager){
            locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                getLocation()
            } else {
                Log.e(TAG, "Your GPS seems to be disabled")
            }
        }
    }

    private fun handleExitButton() {
        if (fmParkingView.visibility == View.VISIBLE) {
            fmParkingView.visibility = View.GONE
            fmParkingView.disconnect()
            exitButton.visibility = View.GONE
            controlsLayout.visibility = View.VISIBLE
            mapButton.visibility = View.GONE
            googleMapView.visibility = View.GONE
            Log.d(TAG, "END SESSION")
        }
    }

    /**
     * Android lifecycle methods
     */
    override fun onStart() {
        super.onStart()
        googleMapView.onStart()
    }

    /**
     * Release heap allocation of the AR session
     */
    override fun onDestroy() {
        super.onDestroy()
        fmParkingView.onDestroy()
    }

    /**
     * Updates the state of the AR session
     */
    override fun onResume() {
        super.onResume()
        fmParkingView.onResume()
        googleMapView.onResume()
    }

    /**
     * Stops the AR session
     */
    override fun onStop() {
        super.onStop()
        googleMapView.onStop()
    }

    /**
     * Pauses the AR session
     */
    override fun onPause() {
        super.onPause()
        fmParkingView.onPause()
        googleMapView.onPause()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        googleMapsManager.onSaveInstanceState(outState)
    }

    /**
     * Makes sure GoogleMaps View follows every Android lifecycle
     * https://developers.google.com/android/reference/com/google/android/gms/maps/MapView#developer-guide
     */
    override fun onLowMemory() {
        super.onLowMemory()
        googleMapView.onLowMemory()
    }

    private fun manageBehaviorMessage(){
        if(!firstBehavior){
            firstBehavior = true
        }

        if(firstBehavior){
            val currentTime = System.nanoTime()
            if ((currentTime - behaviorReceived) / n2s > behaviorThreshold) {
                val clearText = "FrameFilterResult"
                filterRejectionTv.text = clearText
                filterRejectionTv.visibility = View.GONE
            }
        }
    }

    /**
     * Listener for the FMParkingView.
     */
    private val fmParkingViewController: FMParkingViewProtocol =
        object : FMParkingViewProtocol {
            override fun fmParkingViewDidStartQRScanning() {
                Log.d(TAG, "QR Code Reader Enabled")
                googleMapsManager.updateAnchor(true)
                if (qrCodeResultTv.visibility == View.GONE) {
                    qrCodeResultTv.visibility = View.VISIBLE
                }
                if (qrOverlay.visibility == View.GONE) {
                    qrOverlay.visibility = View.VISIBLE
                }
            }

            override fun fmParkingViewDidStopQRScanning(){}
            override fun fmParkingView(qrCode: String, shouldContinue: (Boolean) -> Unit){
                // Display result and approve or not the QR Code Scanned
                qrCodeResultTv.text = qrCode
                if (qrOverlay.visibility == View.VISIBLE) {
                    qrOverlay.visibility = View.GONE
                }
                shouldContinue(true)
                // show dialogue to accept or refuse
            }

            override fun fmParkingViewDidStartLocalizing(){
                Log.d(TAG,"BEGINNING LOCALIZING")
            }
            override fun fmParkingView(behavior: FMBehaviorRequest){
                behaviorReceived = System.nanoTime()
                val stringResult = behavior.displayName
                filterRejectionTv.text = stringResult
                if (filterRejectionTv.visibility == View.GONE) {
                    filterRejectionTv.visibility = View.VISIBLE
                }
            }
            override fun fmParkingView(result: FMLocationResult){
                googleMapsManager.addCorrespondingMarkersToMap(result)
            }
            override fun fmParkingView(error: ErrorResponse, metadata: Any?){}
        }

    /**
     * Gets system location through the app context
     * Then checks if it has permission to ACCESS_FINE_LOCATION
     * Also includes Callback for Location updates.
     * Sets the currentLocation coordinates used to localize.
     */
    private fun getLocation() {
        if ((context.let {
                PermissionChecker.checkSelfPermission(
                    it!!,
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
                    fmParkingView.updateLocation(currentLocation.latitude,currentLocation.longitude)

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
}