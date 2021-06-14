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
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.google.android.gms.location.*

class NoARCoreFragment : Fragment() {

    private val TAG = "NoARCoreFragment"

    private lateinit var currentView: View

    private lateinit var serverCoorTv: TextView
    private lateinit var checkParkingButton: Button

    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: android.location.Location = android.location.Location("")

    private lateinit var fmLocationManager: FMLocationManager

    private val locationInterval = 300L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        currentView = inflater.inflate(R.layout.noarcore_fragment, container, false)

        checkParkingButton = currentView.findViewById(R.id.checkParkingButton)
        serverCoorTv = currentView.findViewById(R.id.serverCoordsText)

        fmLocationManager = context?.let { FMLocationManager(it.applicationContext) }!!
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation()
        } else {
            Log.e(TAG, "Your GPS seems to be disabled")
        }
        return currentView
    }

    /**
     * onStart lifecycle establishes listeners for the buttons and SDK calls
     * */
    override fun onStart() {
        super.onStart()

        // Enable simulation mode to test purposes with specific location
        // depending on which SDK flavor it's being used (Paris, Munich, Miami)
        //fmLocationManager.isSimulation = true

        // Connect the FMLocationManager from Fantasmo SDK
        fmLocationManager.connect(
            "API_KEY",
            fmLocationListener
        )

        checkParkingButton.setOnClickListener {
            Log.d(TAG, "CheckPark Pressed")

            fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, 23) {
                Toast.makeText(
                    activity?.applicationContext,
                    "Is Zone In Radius Response: $it",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
                Log.d(TAG, error.message.toString())
                serverCoorTv.text = error.message.toString()
            }

            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
            }

            @SuppressLint("SetTextI18n")
            override fun locationManager(location: Location, zones: List<FMZone>?) {
                Log.d(TAG, location.toString())
                serverCoorTv.text =
                    "Server Lat: ${location.coordinate.latitude}, Long: ${location.coordinate.longitude}"
            }
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
                    it!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED) &&
            (context.let {
                PermissionChecker.checkSelfPermission(
                    it!!,
                    Manifest.permission.CAMERA
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
                    Log.d(TAG, "onLocationResult: ${locationResult.lastLocation}")
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        }
    }
}