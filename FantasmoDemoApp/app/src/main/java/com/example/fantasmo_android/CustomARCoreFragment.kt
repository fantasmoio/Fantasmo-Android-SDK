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
import com.fantasmo.sdk.FMResultConfidence
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.views.FMParkingView
import com.fantasmo.sdk.views.FMParkingViewProtocol
import com.fantasmo.sdk.views.FMQRScanningViewProtocol
import com.fantasmo.sdk.views.FMLocalizingViewProtocol

import com.google.android.gms.location.*
import com.google.android.gms.maps.MapView
import com.google.ar.core.*
import java.util.*

/**
 * Fragment to show AR camera image and make use of the Fantasmo SDK localization feature.
 */
class CustomARCoreFragment : Fragment() {
    private val TAG = CustomARCoreFragment::class.java.simpleName

    private lateinit var currentView: View

    private lateinit var controlsLayout: ConstraintLayout

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var simulationModeToggle: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showDebugStatsToggle: Switch

    private lateinit var endRideButton: Button
    private lateinit var exitButton: Button

    // Buttons and Views from the QRView
    private lateinit var fmQRView: ConstraintLayout
    private lateinit var qrCodeResultTv: TextView

    // Buttons and Views from the LocalizeView
    private lateinit var fmLocalizeView: ConstraintLayout
    private lateinit var filterRejectionTv: TextView
    private lateinit var mapButton: Button
    private lateinit var googleMapView: MapView
    private lateinit var googleMapsManager: GoogleMapsManager

    private var behaviorReceived = 0L
    private var n2s = 1_000_000_000L
    private val behaviorThreshold = 1L
    private var firstBehavior = false

    // Host App location Manager to exemplify how to set Location
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: android.location.Location = android.location.Location("")
    private val locationInterval = 300L

    // Control variables for the FMParkingView
    private lateinit var fmParkingView: FMParkingView
    private val usesInternalLocationManager = true
    private val accessToken = "API_KEY"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        currentView = inflater.inflate(R.layout.custom_arcore_fragment, container, false)

        controlsLayout = currentView.findViewById(R.id.controlsLayout)

        fmParkingView = currentView.findViewById(R.id.fmParkingView)
        fmParkingView.fmParkingViewController = fmParkingViewController
        fmParkingView.appSessionId = UUID.randomUUID().toString()
        fmParkingView.accessToken = accessToken

        fmParkingView.registerQRScanningViewController(fmQrScanningViewController)
        fmParkingView.registerLocalizingViewController(fmLocalizingViewController)

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

        // Enable FMParkingView internal Location Manager
        fmParkingView.usesInternalLocationManager = usesInternalLocationManager

        handleQRView()
        handleLocalizeView(savedInstanceState)
        handleExitButton()

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

        return currentView
    }

    private fun startParkingFlow() {
        if (fmParkingView.visibility == View.GONE) {
            mapButton.visibility = View.VISIBLE
            fmParkingView.visibility = View.VISIBLE
            // Present the FMParkingView
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

    private fun handleQRView() {
        qrCodeResultTv = currentView.findViewById(R.id.qrCodeResultTextView)
        fmQRView = currentView.findViewById(R.id.fmQRview) as ConstraintLayout
    }

    private fun handleLocalizeView(savedInstanceState: Bundle?) {
        fmLocalizeView = currentView.findViewById(R.id.custom_fmLocalizeView)
        googleMapView = currentView.findViewById(R.id.mapView)
        googleMapsManager = GoogleMapsManager(requireActivity(), googleMapView)
        googleMapsManager.initGoogleMap(savedInstanceState)

        filterRejectionTv = currentView.findViewById(R.id.custom_filterRejectionTextView)
        mapButton = currentView.findViewById(R.id.mapButton)
        mapButton.setOnClickListener {
            if (googleMapView.visibility == View.VISIBLE) {
                googleMapView.visibility = View.GONE
            } else {
                googleMapView.visibility = View.VISIBLE
            }
        }
    }

    private fun handleExitButton() {
        exitButton = currentView.findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            if (fmParkingView.visibility == View.VISIBLE) {
                fmParkingView.visibility = View.GONE
                // When exit Button is clicked close the session
                fmParkingView.disconnect()
                exitButton.visibility = View.GONE
                controlsLayout.visibility = View.VISIBLE
                fmLocalizeView.visibility = View.GONE
                googleMapsManager.unsetAnchor()
                Log.d(TAG, "END SESSION")
            }
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
     * Gets system location through the app context
     * Then checks if it has permission to ACCESS_FINE_LOCATION
     * Also includes Callback for Location updates.
     * Sets the FMParkingView currentLocation coordinates used to localize.
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

    /**
     * Listener for the FMParkingView.
     */
    private val fmParkingViewController: FMParkingViewProtocol =
        object : FMParkingViewProtocol {
            override fun fmParkingViewDidStartQRScanning() {
                Log.d(TAG, "fmParkingViewDidStartQRScanning")
            }

            override fun fmParkingViewDidStopQRScanning(){
                Log.d(TAG, "fmParkingViewDidStopQRScanning")
            }
            override fun fmParkingView(qrCode: String, shouldContinue: (Boolean) -> Unit){
                Log.d(TAG, "fmParkingView ShouldContinue")
                // Optional validation of the QR code can be done here
                // Note: If you choose to implement this method, you must call the `shouldApprove` with the validation result
                // show dialogue to accept or refuse
                shouldContinue(true)
            }

            override fun fmParkingViewDidStartLocalizing(){
                Log.d(TAG,"fmParkingViewDidStartLocalizing")
                if(fmLocalizeView.visibility == View.GONE){
                    fmLocalizeView.visibility = View.VISIBLE
                }
            }
            override fun fmParkingView(behavior: FMBehaviorRequest){
                Log.d(TAG,"fmParkingView Behavior")
            }

            override fun fmParkingView(result: FMLocationResult){
                Log.d(TAG,"fmParkingView Result")
                // Got a localization result
                // Localization will continue until you dismiss the view
                // You should decide on acceptable criteria for a result, one way is by checking the `confidence` value
                if(result.confidence == FMResultConfidence.LOW){
                    Log.d(TAG,"Low Confidence Result")
                }
                googleMapsManager.addCorrespondingMarkersToMap(result)
            }
            override fun fmParkingView(error: ErrorResponse, metadata: Any?){}
        }

    /**
     * Custom Listener for the Fantasmo Localizing View.
     */
    private var fmLocalizingViewController: FMLocalizingViewProtocol =
        object : FMLocalizingViewProtocol {
            override fun didStartLocalizing() {
                Log.d(TAG,"didStartLocalizing")
            }

            override fun didRequestLocalizationBehavior(behavior: FMBehaviorRequest) {
                Log.d(TAG,"didRequestLocalizationBehavior")
                behaviorReceived = System.nanoTime()
                val stringResult = behavior.displayName
                filterRejectionTv.text = stringResult
                if (filterRejectionTv.visibility == View.GONE) {
                    filterRejectionTv.visibility = View.VISIBLE
                }
            }

            override fun didReceiveLocalizationResult(result: FMLocationResult) {
                Log.d(TAG,"didReceiveLocalizationResult")
            }

            override fun didReceiveLocalizationError(error: ErrorResponse, errorMetadata: Any?) {
                Log.d(TAG,"didReceiveLocalizationError")
            }
        }

    /**
     * Custom Listener for the Fantasmo QR Scanning View.
     */
    private var fmQrScanningViewController: FMQRScanningViewProtocol =
        object : FMQRScanningViewProtocol {
            override fun didStartQRScanning() {
                Log.d(TAG,"didStartQRScanning")
                googleMapsManager.updateAnchor(true)

                if (fmQRView.visibility == View.GONE) {
                    fmQRView.visibility = View.VISIBLE
                }
            }

            override fun didScanQRCode(result: String) {
                Log.d(TAG,"didScanQRCode")
                qrCodeResultTv.text = result
                if (fmQRView.visibility == View.VISIBLE) {
                    fmQRView.visibility = View.GONE
                }
            }

            override fun didStopQRScanning() {
                Log.d(TAG,"didStopQRScanning")
                if (qrCodeResultTv.visibility == View.VISIBLE) {
                    qrCodeResultTv.visibility = View.GONE
                }
                if (fmQRView.visibility == View.VISIBLE) {
                    fmQRView.visibility = View.GONE
                }
            }
        }
}