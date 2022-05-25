package com.fantasmo.sdk.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.*

/**
 * Internal Location Manager.
 *
 * When host app doesn't specify whether to use a provided Location manager this,
 * starts to get the location updates.
 */
internal class DeviceLocationManager(val context: Context?, val deviceLocationListener: DeviceLocationListener) {

    companion object{
        fun isValidLatLng(latitude: Double, longitude: Double): Boolean {
            if (latitude > 90.0 || latitude < -90.0) {
                return false
            } else if (longitude > 180.0 || longitude < -180.0) {
                return false
            }
            return true
        }
    }
    private val TAG = DeviceLocationManager::class.java.simpleName
    private var locationManager: LocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
    private var currentLocation: Location = Location("")
    private val locationInterval = 300L
    private lateinit var locationCallback : LocationCallback

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

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    currentLocation = locationResult.lastLocation
                    //Set SDK Location
                    deviceLocationListener.onLocationUpdate(currentLocation)
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

    fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

interface DeviceLocationListener {
    fun onLocationUpdate(locationResult: Location)
}