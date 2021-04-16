//
//  FMLocationManager.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.PermissionChecker
import com.fantasmo.sdk.filters.FMBehaviorRequest
import com.fantasmo.sdk.filters.FMFrameSequenceGuard
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.network.FMApi
import com.fantasmo.sdk.network.FMNetworkManager
import com.google.android.gms.location.*
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The methods that you use to receive events from an associated
 * location manager object.
 */
interface FMLocationListener {

    /**
     * Tells the listener that new location data is available.
     * @param location: Location of the device (or anchor if set)
     * @param zones: Semantic zone corresponding to the location
     */
    fun locationManager(location: Location, zones: List<FMZone>?)

    /**
     * Tells the listener that an error has occurred.
     * @param error: The error reported.
     * @param metadata: Metadata related to the error.
     */
    fun locationManager(error: ErrorResponse, metadata: Any?)

    /**
     * Tells the listener that a request behavior has occurred.
     * @param didRequestBehavior: The behavior reported.
     */
    fun locationManager(didRequestBehavior: FMBehaviorRequest)
}

class FMLocationManager(private val context: Context) {
    private val TAG = "FMLocationManager"

    enum class State {
        // doing nothing
        STOPPED,

        // localizing
        LOCALIZING,

        // uploading image while localizing
        UPLOADING
    }

    private val fmNetworkManager = FMNetworkManager(FMConfiguration.getServerURL(), context)
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var fmApi: FMApi

    var state = State.STOPPED
    private lateinit var qualityFilter : FMFrameSequenceGuard
    var anchorFrame: Frame? = null
    var currentLocation: android.location.Location = android.location.Location("")

    private var fmLocationListener: FMLocationListener? = null
    private var token: String = ""

    /// When in simulation mode, mock data is used from the assets directory instead of the live camera feed.
    /// This mode is useful for implementation and debugging.
    var isSimulation = false

    var isConnected = false

    /**
     * Connect to the location service.
     *
     * @param accessToken: Token for service authorization.
     * @param callback: FMLocationListener
     */
    fun connect(
        accessToken: String,
        callback: FMLocationListener
    ) {
        Log.d(TAG, "connect: $callback")

        this.token = accessToken
        this.fmLocationListener = callback
        fmApi = FMApi(fmNetworkManager, this, context, token)
        qualityFilter = FMFrameSequenceGuard(this.fmLocationListener!!, context)
    }

    /**
     * Starts the generation of updates that report the user’s current location.
     */
    fun startUpdatingLocation() {
        Log.d(TAG, "startUpdatingLocation")

        this.isConnected = true
        this.state = State.LOCALIZING
        this.qualityFilter.startFiltering()

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context)
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                getLocation()
            } else {
                Log.e(TAG, "GPS is disabled")
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Can't instantiate FusedLocationProviderClient: ${exception.message}")
        }
    }

    /**
     * Stops the generation of location updates.
     */
    fun stopUpdatingLocation() {
        Log.d(TAG, "stopUpdatingLocation")

        this.state = State.STOPPED
    }

    /**
     * Set an anchor point. All location updates will now report the
     * location of the anchor instead of the camera.
     * @param [arFrame] an AR Frame to use as anchor.
     */
    fun setAnchor(arFrame: Frame) {
        Log.d(TAG, "setAnchor")

        this.anchorFrame = arFrame
    }

    /**
     * Unset the anchor point. All location updates will now report the
     * location of the camera.
     */
    fun unsetAnchor() {
        Log.d(TAG, "unsetAnchor")

        this.anchorFrame = null
    }

    /**
     * Gets system location through the app context
     * Then checks if it has permission to ACCESS_FINE_LOCATION
     * Also includes Callback for Location updates.
     * Updates the [currentLocation] coordinates being used to localize.
     */
    private fun getLocation() {
        if ((context.let {
                PermissionChecker.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED) &&
            (context.let {
                PermissionChecker.checkSelfPermission(
                    it,
                    Manifest.permission.CAMERA
                )
            } != PackageManager.PERMISSION_GRANTED)) {
            Log.w(TAG, "Location permission needs to be granted.")
        } else {
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.smallestDisplacement = 1f
            locationRequest.fastestInterval = 300
            locationRequest.interval = 300

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    currentLocation = locationResult.lastLocation
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
     * Localize the image frame. It triggers a network request that
     * provides a response via the callback [FMLocationListener].
     * @param arFrame an AR Frame to localize
     */
    fun localize(arFrame: Frame) {
        if (!shouldLocalize(arFrame)) {
            return
        }

        Log.d(TAG,"localize: isSimulation $isSimulation")
        CoroutineScope(Dispatchers.IO).launch {
            state = State.UPLOADING

            fmApi.sendLocalizeRequest(
                arFrame,
                { localizeResponse, fmZones ->
                    Log.d(TAG,"localize: $localizeResponse, Zones $fmZones")
                    fmLocationListener?.locationManager(
                        localizeResponse,
                        fmZones
                    )

                    updateStateAfterLocalization()
                },
                {
                    Log.e(TAG,"localize: $it")
                    fmLocationListener?.locationManager(it, null)

                    updateStateAfterLocalization()
                })
        }
    }

    /**
     * Update the state back to LOCALIZING is not STOPPED.
     */
    private fun updateStateAfterLocalization() {
        if (state != State.STOPPED) {
            state = State.LOCALIZING
        }
    }

    /**
     * Method to check whether the SDK is ready to localize a frame or not.
     * @return true if it can localize the ARFrame and false otherwise.
     */
    private fun shouldLocalize(arFrame: Frame): Boolean {
        return isConnected
                && currentLocation.latitude > 0.0
                && arFrame.camera.trackingState == TrackingState.TRACKING
                && qualityFilter.accepts(arFrame)
    }

    /**
     * Check to see if a given zone is in the provided radius
     * @param zone: zone to search for
     * @param radius: search radius in meters
     * @param onCompletion: closure that consumes boolean server result
     */
    fun isZoneInRadius(zone: FMZone.ZoneType, radius: Int, onCompletion: (Boolean) -> Unit) {
        if (!isConnected) {
            return
        }
        Log.d(TAG,"isZoneInRadius")
        CoroutineScope(Dispatchers.IO).launch {
            fmApi.sendZoneInRadiusRequest(
                radius,
                onCompletion
            )
        }
    }
}
