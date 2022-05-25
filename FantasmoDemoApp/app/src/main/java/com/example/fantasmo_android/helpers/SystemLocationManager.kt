package com.example.fantasmo_android.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.*

class SystemLocationManager(
    private val context: Context?,
    val systemLocationListener: SystemLocationListener
) {

    private val TAG = SystemLocationManager::class.java.simpleName
    private var locationManager: LocationManager =
        context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context!!)
    private var currentLocation: Location = Location("")
    private val locationInterval = 300L
    private var firstLocation = false

    init {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation()
        } else {
            Log.e(TAG, "Your GPS seems to be disabled")
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
                    if(!firstLocation){
                        systemLocationListener.hasLocation()
                        firstLocation = true
                    }
                    currentLocation = locationResult.lastLocation
                    //Set SDK Location
                    systemLocationListener.onLocationUpdate(currentLocation)
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

interface SystemLocationListener {
    fun onLocationUpdate(currentLocation: Location)
    fun hasLocation() {}
}