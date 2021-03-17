//
//  FMLocationManager.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk

import android.content.Context
import android.util.Log
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.network.FMNetworkManager
import com.google.ar.core.Frame
import com.google.gson.Gson
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
    var state = State.STOPPED

    private var anchorFrame: Frame? = null
    var anchorDelta: Array<Array<Float>>? = null

    private var fmLocationListener: FMLocationListener? = null
    private var token: String? = null

    /// When in simulation mode, mock data is used from the assets directory instead of the live camera feed.
    /// This mode is useful for implementation and debugging.
    var isSimulation = false

    /// The zone that will be simulated.
    var simulationZone = FMZone.ZoneType.parking
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
     */
    fun setAnchor() {
        Log.d(TAG, "FMLocationManager:setAnchor")
        //this.anchorFrame = ARSession.lastFrame
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
     * Localize the image frame. It triggers a network request that
     * provides a response via the callback [FMLocationListener].
     * @param arFrame an AR Frame to localize
     */
    fun localize(arFrame: Frame) {
        if (!isConnected) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            fmNetworkManager.uploadImage(
                FMUtility.getImageDataFromARFrame(arFrame.acquireCameraImage()),
                getLocalizeParams(),
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
                    }
                },
                {
                    fmLocationListener?.locationManager(it, null)
                })
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
            fmNetworkManager.zoneInRadiusRequest(getZoneInRadiusParams(radius), token!!, onCompletion)
        }
    }

    /**
     * Generate the localize HTTP request parameters. Can fail if the jpeg
     * conversion throws an exception.
     * @param frame: Frame to localize
     * @param deviceOrientation: Current device orientation for computing intrinsics
     * @param interfaceOrientation: Current interface orientation for computing intrinsics
     * @param currentLocation: Current geo location for coarse estimate
     * @return an HashMap with all the localization parameters.
     */
    private fun getLocalizeParams(): HashMap<String, String> {
        val params = hashMapOf<String, String>()

        params["capturedAt"] = "1615487312.571168"
        params["gravity"] =
            "{\"y\":0.92625105381011963,\"w\":0.27762770652770996,\"z\":0.25091192126274109,\"x\":-0.044999953359365463}"
        params["uuid"] = "30989AC2-B7C7-4619-B078-04E669A13937"
        params["coordinate"] =
            "{\"longitude\" : 2.371750713292894, \"latitude\": 48.848138681935886}"
        params["intrinsics"] =
            "{\"cx\":481.0465087890625,\"fy\":1083.401611328125,\"fx\":1083.401611328125,\"cy\":629.142822265625}"

        return params
    }

    /**
     * Generate the zoneInRadius HTTP request parameters.
     * @param radius: search radius in meters
     * @return an HashMap with all the localization parameters.
     */
    private fun getZoneInRadiusParams(radius: Int): HashMap<String, String> {
        val params = hashMapOf<String, String>()

        val coordinate = Coordinate(48.848138681935886, 2.371750713292894)

        params["radius"] = radius.toString()
        params["coordinate"] = Gson().toJson(coordinate)

        return params
    }
}
