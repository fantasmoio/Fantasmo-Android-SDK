//
//  FMLocationManager.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk

import android.content.Context
import android.util.Log
import com.fantasmo.sdk.filters.FMBehaviorRequest
import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterResult
import com.fantasmo.sdk.filters.FMCompoundFrameQualityFilter
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.fantasmo.sdk.network.FMApi
import com.fantasmo.sdk.network.FMNetworkManager
import com.fantasmo.sdk.utilities.FrameFailureThrottler
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import kotlinx.coroutines.*
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

    /**
     * Tells the listener that a request behavior has occurred.
     * @param didRequestBehavior: The behavior reported.
     */
    fun locationManager(didRequestBehavior: FMBehaviorRequest){}
}

class FMLocationManager(private val context: Context) {
    private val TAG = "FMLocationManager"

    private val locationInterval = 300L

    enum class State {
        // doing nothing
        STOPPED,

        // localizing
        LOCALIZING,

        // uploading image while localizing
        UPLOADING
    }

    var fmNetworkManager = FMNetworkManager(FMConfiguration.getServerURL(), context)
    var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    lateinit var fmApi: FMApi

    var state = State.STOPPED

    var anchorFrame: Frame? = null
    var currentLocation: android.location.Location = android.location.Location("")

    private var fmLocationListener: FMLocationListener? = null
    private var token: String = ""

    /// When in simulation mode, mock data is used from the assets directory instead of the live camera feed.
    /// This mode is useful for implementation and debugging.
    var isSimulation = false

    var isConnected = false

    private var enableFilters = false

    // Used to validate frame for sufficient quality before sending to API.
    lateinit var compoundFrameFilter: FMCompoundFrameQualityFilter
    // Throttler for invalid frames.
    private lateinit var frameFailureThrottler: FrameFailureThrottler

    // Localization Session Id generated on each startUpdatingLocation call
    private lateinit var localizationSessionId: String
    // App Session Id supplied by the SDK client
    private lateinit var appSessionId: String
    private var frameRejectionStatisticsAccumulator = FrameFilterRejectionStatistics()
    private var accumulatedARCoreInfo = AccumulatedARCoreInfo()

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
        compoundFrameFilter = FMCompoundFrameQualityFilter(context)
        frameFailureThrottler = FrameFailureThrottler()
    }

    /**
     * Sets currentLocation with values given by the client application.
     *
     * @param latitude: Location latitude.
     * @param longitude: Location longitude.
     * */
    fun setLocation(latitude: Double, longitude: Double) {
        this.currentLocation.latitude = latitude
        this.currentLocation.longitude = longitude
        Log.d(TAG, "SetLocation: $currentLocation")
    }

    /**
     * Starts the generation of updates that report the user’s current location.
     * @param appSessionId: appSessionId supplied by the SDK client and used for billing and tracking an entire parking session
     */
    fun startUpdatingLocation(appSessionId: String) {
        localizationSessionId = UUID.randomUUID().toString()
        this.appSessionId = appSessionId
        Log.d(TAG, "startUpdatingLocation with AppSessionId:$appSessionId and LocalizationSessionId:$localizationSessionId")

        this.isConnected = true
        this.state = State.LOCALIZING
        enableFilters = false
        accumulatedARCoreInfo.reset()
    }

    /**
     * Starts the generation of updates that report the user’s current location
     * enabling FrameFiltering
     * @param appSessionId: appSessionId supplied by the SDK client and used for billing and tracking an entire parking session
     * @param filtersEnabled: flag that it enables frame filtering
     */
    fun startUpdatingLocation(appSessionId: String, filtersEnabled : Boolean) {
        localizationSessionId = UUID.randomUUID().toString()
        this.appSessionId = appSessionId
        Log.d(TAG, "startUpdatingLocation with AppSessionId:$appSessionId and LocalizationSessionId:$localizationSessionId")

        this.isConnected = true
        this.state = State.LOCALIZING
        enableFilters = filtersEnabled
        this.compoundFrameFilter.prepareForNewFrameSequence()
        this.frameFailureThrottler.restart()
        frameRejectionStatisticsAccumulator.reset()
        accumulatedARCoreInfo.reset()
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
     * Localize the image frame. It triggers a network request that
     * provides a response via the callback [FMLocationListener].
     * @param arFrame an AR Frame to localize
     */
    fun localize(arFrame: Frame) {
        if (!shouldLocalize(arFrame)) {
            return
        }

        Log.d(TAG, "localize: isSimulation $isSimulation")
        coroutineScope.launch {
            state = State.UPLOADING

            fmApi.sendLocalizeRequest(
                arFrame,
                { localizeResponse, fmZones ->
                    Log.d(TAG, "localize: $localizeResponse, Zones $fmZones")
                    fmLocationListener?.locationManager(
                        localizeResponse,
                        fmZones
                    )

                    updateStateAfterLocalization()
                },
                {
                    Log.e(TAG, "localize: $it")
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
    fun shouldLocalize(arFrame: Frame): Boolean {
        accumulatedARCoreInfo.update(arFrame)
        if (isConnected
            && currentLocation.latitude > 0.0
            && arFrame.camera.trackingState == TrackingState.TRACKING
        ) {
            return if(enableFilters){
                val result = compoundFrameFilter.accepts(arFrame)
                if (result.first == FMFrameFilterResult.ACCEPTED) {
                    fmLocationListener?.locationManager(frameFailureThrottler.handler(result.second))
                    frameFailureThrottler.restart()
                    true
                } else {
                    frameRejectionStatisticsAccumulator.accumulate(result.second)
                    frameFailureThrottler.onNext(result.second)
                    fmLocationListener?.locationManager(frameFailureThrottler.handler(result.second))
                    false
                }
            }else{
                true
            }
        }
        return false
    }

    /**
     * Check to see if a given zone is in the provided radius
     * @param zone: zone to search for
     * @param radius: search radius in meters
     * @param onCompletion: closure that consumes boolean server result
     */
    fun isZoneInRadius(zone: FMZone.ZoneType, radius: Int, onCompletion: (Boolean) -> Unit) {
        Log.d(TAG, "isZoneInRadius")
        val timeOut = 10000
        coroutineScope.launch {
            // If it's not in simulation mode
            if (!isSimulation) {
                // Wait on First Location Update if it isn't already available
                val start = System.currentTimeMillis()
                while (currentLocation.latitude == 0.0) {
                    delay(locationInterval)
                    if (System.currentTimeMillis() - start > timeOut) {
                        // When timeout is reached, isZoneInRadius sends empty coordinates field
                        Log.d(TAG, "isZoneInRadius Timeout Reached")
                        break
                    }
                }
            }

            fmApi.sendZoneInRadiusRequest(
                radius,
                onCompletion
            )
        }
    }
}
