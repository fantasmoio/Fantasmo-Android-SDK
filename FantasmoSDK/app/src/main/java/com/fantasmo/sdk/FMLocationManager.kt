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
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.PermissionChecker
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.network.FMNetworkManager
import com.google.android.gms.location.*
import com.google.ar.core.Frame
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


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

    var state = State.STOPPED

    private var anchorFrame: Frame? = null
    var anchorDelta: Array<Array<Float>>? = null

    private var currentLocation: android.location.Location = android.location.Location("")

    private var fmLocationListener: FMLocationListener? = null
    private var token: String? = null

    /// When in simulation mode, mock data is used from the assets directory instead of the live camera feed.
    /// This mode is useful for implementation and debugging.
    var isSimulation = false

    /// The zone that will be simulated.
    var simulationZone = FMZone.ZoneType.PARKING
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
        Log.d(TAG, "FMLocationManager connected with")

        this.token = accessToken
        this.fmLocationListener = callback
    }

    /**
     * Starts the generation of updates that report the user’s current location.
     */
    fun startUpdatingLocation() {
        Log.d(TAG, "FMLocationManager:startUpdatingLocation")

        this.isConnected = true
        this.state = State.LOCALIZING

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context)
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation()
        } else {
            Log.e(TAG, "Your GPS seems to be disabled")
        }
    }

    /**
     * Stops the generation of location updates.
     */
    fun stopUpdatingLocation() {
        Log.d(TAG, "FMLocationManager:stopUpdatingLocation")

        this.state = State.STOPPED
    }

    /**
     * Set an anchor point. All location updates will now report the
     * location of the anchor instead of the camera.
     * @param [arFrame] an AR Frame to use as anchor.
     */
    fun setAnchor(arFrame: Frame) {
        Log.d(TAG, "FMLocationManager:setAnchor")
        this.anchorFrame = arFrame
    }

    /**
     * Unset the anchor point. All location updates will now report the
     * location of the camera.
     */
    fun unsetAnchor() {
        Log.d(TAG, "FMLocationManager:unsetAnchor")

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
            Log.e(TAG, "Location permission needs to be granted.")
        } else {
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.smallestDisplacement = 1f
            locationRequest.fastestInterval = 300
            locationRequest.interval = 300

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    currentLocation = locationResult.lastLocation
                    Log.d(TAG, "Location: ${locationResult.lastLocation}")
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        }
    }

    /**
     * Localize the image frame. It triggers a network request that
     * provides a response via the callback [FMLocationListener].
     * @param arFrame an AR Frame to localize
     */
    fun localize(arFrame: Frame) {
        if (!isConnected || currentLocation.latitude == 0.0) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            state = State.UPLOADING

            try {
                fmNetworkManager.uploadImage(
                    FMUtility.getImageDataFromARFrame(context, arFrame),
                    getLocalizeParams(arFrame),
                    token!!,
                    {
                        val location = it.location
                        val geofences = it.geofences

                        val fmZones = mutableListOf<FMZone>()
                        if (geofences != null && geofences.isNotEmpty()) {
                            for (geofence in geofences) {
                                val fmZone = FMZone(
                                    FMZone.ZoneType.valueOf(geofence.elementType),
                                    geofence.elementID.toString()
                                )
                                fmZones.add(fmZone)
                            }
                        }
                        location?.let { localizeResponse ->
                            fmLocationListener?.locationManager(
                                localizeResponse,
                                fmZones
                            )

                            if (state != State.STOPPED) {
                                state = State.LOCALIZING
                            }
                        }
                    },
                    {
                        fmLocationListener?.locationManager(it, null)

                        if (state != State.STOPPED) {
                            state = State.LOCALIZING
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
                if (state != State.STOPPED) {
                    state = State.LOCALIZING
                }
            }
        }
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

        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://api.fantasmo.io/v1/parking.in.radius"
            fmNetworkManager.zoneInRadiusRequest(
                url,
                getZoneInRadiusParams(radius),
                token!!,
                onCompletion
            )
        }
    }

    /**
     * Generate the localize HTTP request parameters. Can fail if the jpeg
     * conversion throws an exception.
     * @param frame: Frame to localize
     * @return an HashMap with all the localization parameters.
     */
    private fun getLocalizeParams(frame: Frame): HashMap<String, String> {
        val pose = FMUtility.getPoseBasedOnDeviceOrientation(context, frame)

        val coordinates = if (isSimulation) {
            val simulationLocation = FMConfiguration.getConfigLocation()
            Coordinate(simulationLocation.latitude, simulationLocation.longitude)
        } else {
            Coordinate(currentLocation.latitude, currentLocation.longitude)
        }

        val focalLength = frame.camera.imageIntrinsics.focalLength
        val principalPoint = frame.camera.imageIntrinsics.principalPoint
        val intrinsics = FMIntrinsics(
            focalLength.component1(),
            focalLength.component2(),
            principalPoint.component2(),
            principalPoint.component1()
        )

        val params = hashMapOf<String, String>()
        val gson = Gson()
        params["capturedAt"] = System.currentTimeMillis().toString()
        params["gravity"] = gson.toJson(pose.orientation)
        params["uuid"] = UUID.randomUUID().toString()
        params["coordinate"] = gson.toJson(coordinates)
        params["intrinsics"] = gson.toJson(intrinsics)

        // calculate and send reference frame if anchoring
        if (anchorFrame != null) {
            params["referenceFrame"] = gson.toJson(anchorDeltaPoseForFrame(frame))
        }

        return params
    }

    /**
     * Generate the zoneInRadius HTTP request parameters.
     * @param radius: search radius in meters
     * @return an HashMap with all the localization parameters.
     *
     * Only works with PARKING zones currently
     */
    private fun getZoneInRadiusParams(radius: Int): HashMap<String, String> {
        val params = hashMapOf<String, String>()

        val coordinates = if (isSimulation) {
            val simulationLocation = FMConfiguration.getConfigLocation()
            Coordinate(simulationLocation.latitude, simulationLocation.longitude)
        } else {
            Coordinate(currentLocation.latitude, currentLocation.longitude)
        }

        params["radius"] = radius.toString()
        params["coordinate"] = Gson().toJson(coordinates)

        return params
    }

    /**
     * Calculate the FMPose difference of the anchor frame with respect to the given frame.
     * @param arFrame the current AR Frame.
     */
    private fun anchorDeltaPoseForFrame(arFrame: Frame): FMPose {
        return if (anchorFrame != null) {
            val poseARFrame = arFrame.androidSensorPose.extractRotation()
            val poseAnchor = anchorFrame!!.androidSensorPose.extractRotation()
            FMPose.diffPose(poseAnchor, poseARFrame)
        } else {
            FMPose()
        }
    }
}
