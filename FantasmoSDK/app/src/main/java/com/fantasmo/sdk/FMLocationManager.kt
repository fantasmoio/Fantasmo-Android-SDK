//
//  FMLocationManager.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk

import android.content.Context
import android.os.Build
import android.util.Log
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.filters.FMFrameFilterChain
import com.fantasmo.sdk.filters.FMFrameFilterResult
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.MotionManager
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.fantasmo.sdk.network.*
import com.fantasmo.sdk.filters.BehaviorRequester
import com.fantasmo.sdk.filters.FMImageQualityFilter
import com.fantasmo.sdk.utilities.LocationFuser
import com.google.ar.core.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class FMLocationManager(private val context: Context) {
    private val TAG = "FMLocationManager"

    enum class State {
        // doing nothing
        STOPPED,

        // localizing
        LOCALIZING,

        // uploading image while localizing
        UPLOADING,

        // paused
        PAUSED
    }

    private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private var locationFuser = LocationFuser()

    lateinit var fmApi: FMApi

    var state = State.STOPPED

    var anchorFrame: Frame? = null
    var currentLocation: android.location.Location = android.location.Location("")

    private var fmLocationListener: FMLocationListener? = null
    private var token: String = ""

    /// When in simulation mode, mock data is used from the assets directory instead of the live camera feed.
    /// This mode is useful for implementation and debugging.
    var isSimulation = false

    private var isConnected = false

    private var enableFilters = false

    // Used to validate frame for sufficient quality before sending to API.
    private var frameFilterChain = FMFrameFilterChain(context, RemoteConfig())

    // Throttler for invalid frames.
    private var behaviorRequester = BehaviorRequester {
        fmLocationListener?.locationManager(didRequestBehavior = it)
    }

    private var motionManager = MotionManager(context)

    // Localization Session Id generated on each startUpdatingLocation call
    private lateinit var localizationSessionId: String

    // App Session Id supplied by the SDK client
    private lateinit var appSessionId: String
    private var frameEventAccumulator = FrameFilterRejectionStatistics()
    private var accumulatedARCoreInfo = AccumulatedARCoreInfo()

    /**
     * Connect to the location service.
     *
     * @param accessToken Token for service authorization.
     * @param callback `FMLocationListener`
     */
    fun connect(
        accessToken: String,
        callback: FMLocationListener
    ) {
        Log.d(TAG, "connect: $callback")
        this.token = accessToken
        this.fmLocationListener = callback
        fmApi = FMApi(context, token)
        fmLocationListener?.locationManager(state)
    }

    /**
     * Sets currentLocation with values given by the client application.
     *
     * @param latitude Location latitude.
     * @param longitude Location longitude.
     */
    fun setLocation(latitude: Double, longitude: Double) {
        this.currentLocation.latitude = latitude
        this.currentLocation.longitude = longitude
        Log.d(TAG, "SetLocation: $currentLocation")
    }

    /**
     * Starts the generation of updates that report the user’s current location.
     * @param appSessionId appSessionId supplied by the SDK client and used for billing and tracking an entire parking session
     */
    fun startUpdatingLocation(appSessionId: String) {
        localizationSessionId = UUID.randomUUID().toString()
        this.appSessionId = appSessionId
        Log.d(
            TAG,
            "startUpdatingLocation with AppSessionId:$appSessionId and LocalizationSessionId:$localizationSessionId"
        )

        this.isConnected = true
        this.state = State.LOCALIZING
        fmLocationListener?.locationManager(state)
        enableFilters = false
        motionManager.restart()
        accumulatedARCoreInfo.reset()
        this.locationFuser.reset()
    }

    /**
     * Starts the generation of updates that report the user’s current location
     * enabling FrameFiltering
     * @param appSessionId appSessionId supplied by the SDK client and used for billing and tracking an entire parking session
     * @param filtersEnabled flag that enables/disables frame filtering
     */
    fun startUpdatingLocation(appSessionId: String, filtersEnabled: Boolean) {
        localizationSessionId = UUID.randomUUID().toString()
        this.appSessionId = appSessionId
        Log.d(
            TAG,
            "startUpdatingLocation with AppSessionId:$appSessionId and LocalizationSessionId:$localizationSessionId"
        )

        this.isConnected = true
        this.state = State.LOCALIZING
        fmLocationListener?.locationManager(state)
        enableFilters = filtersEnabled
        motionManager.restart()
        accumulatedARCoreInfo.reset()
        this.frameFilterChain.restart()
        this.behaviorRequester.restart()
        this.locationFuser.reset()
        frameEventAccumulator.reset()
    }

    /**
     * Stops the generation of location updates.
     */
    fun stopUpdatingLocation() {
        Log.d(TAG, "stopUpdatingLocation")
        motionManager.stop()
        this.state = State.STOPPED
        fmLocationListener?.locationManager(state)
    }

    /**
     * Set an anchor point. All location updates will now report the
     * location of the anchor instead of the camera.
     * @param [arFrame] an AR Frame to use as anchor.
     */
    fun setAnchor(arFrame: Frame) {
        Log.d(TAG, "setAnchor")

        this.anchorFrame = arFrame
        locationFuser.reset()
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
    private fun localize(arFrame: Frame) {
        if (!isConnected
            && currentLocation.latitude > 0.0
        ) {
            return
        }
        Log.d(TAG, "localize: isSimulation $isSimulation")
        coroutineScope.launch {
            state = State.UPLOADING
            fmLocationListener?.locationManager(state)
            val localizeRequest = createLocalizationRequest(arFrame)
            fmApi.sendLocalizeRequest(
                arFrame,
                localizeRequest,
                { localizeResponse, fmZones ->
                    Log.d(TAG, "localize: $localizeResponse, Zones $fmZones")
                    val result = locationFuser.fusedResult(localizeResponse, fmZones)
                    fmLocationListener?.locationManager(
                        result
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
     * Gather all the information needed to assemble a LocalizationRequest.
     */
    private fun createLocalizationRequest(frame: Frame): FMLocalizationRequest {
        val frameEvents = FMFrameEvent(
            (frameEventAccumulator.excessiveTiltFrameCount + frameEventAccumulator.insufficientTiltFrameCount),
            frameEventAccumulator.excessiveBlurFrameCount,
            frameEventAccumulator.excessiveMotionFrameCount,
            frameEventAccumulator.insufficientFeatures,
            (accumulatedARCoreInfo.trackingStateFrameStatistics.framesWithLimitedTrackingState
                    + accumulatedARCoreInfo.trackingStateFrameStatistics.framesWithNotAvailableTracking
                    ),
            accumulatedARCoreInfo.elapsedFrames
        )
        val rotationSpread = FMRotationSpread(
            accumulatedARCoreInfo.rotationAccumulator.pitch[2],
            accumulatedARCoreInfo.rotationAccumulator.yaw[2],
            accumulatedARCoreInfo.rotationAccumulator.roll[2]
        )
        val frameAnalytics = FMLocalizationAnalytics(
            appSessionId,
            localizationSessionId,
            frameEvents,
            rotationSpread,
            accumulatedARCoreInfo.translationAccumulator.totalTranslation,
            motionManager.magneticField
        )
        val openCVRelativeAnchorPose = anchorFrame?.let { anchorFrame ->
            FMUtility.anchorDeltaPoseForFrame(
                frame,
                anchorFrame
            )
        }

        return FMLocalizationRequest(
            isSimulation,
            FMZone.ZoneType.PARKING,
            Coordinate(
                currentLocation.latitude,
                currentLocation.longitude
            ),
            openCVRelativeAnchorPose,
            frameAnalytics
        )
    }

    /**
     * Update the state back to LOCALIZING it is not STOPPED.
     */
    private fun updateStateAfterLocalization() {
        if (state != State.STOPPED) {
            state = State.LOCALIZING
            fmLocationListener?.locationManager(state)
        }
    }

    private var isEvaluatingFrame = false

    /**
     * Method to check whether the SDK is ready to localize a frame or not.
     */
    fun session(arFrame: Frame) {
        if (state != State.STOPPED
            && !isEvaluatingFrame
        ) {
            // run the frame through the configured filters
            isEvaluatingFrame = true
            frameFilterChain.evaluateAsync(arFrame) { filterResult ->
                processFrame(arFrame, filterResult)
                isEvaluatingFrame = false
            }
        }
    }

    private fun processFrame(arFrame: Frame, filterResult: FMFrameFilterResult) {
        behaviorRequester.processResult(filterResult)
        accumulatedARCoreInfo.update(arFrame)
        if (filterResult == FMFrameFilterResult.Accepted) {
            if (state == State.LOCALIZING) {
                localize(arFrame)
            }
        } else {
            frameEventAccumulator.accumulate(filterResult.getRejectedReason()!!)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (frameFilterChain.rc!!.isImageQualityFilterEnabled) {
                val filter = frameFilterChain.filters.last() as FMImageQualityFilter
                accumulatedARCoreInfo.lastImageQualityScore = filter.lastImageQualityScore
                accumulatedARCoreInfo.scoreThreshold = filter.scoreThreshold
                accumulatedARCoreInfo.modelVersion = filter.modelVersion
            }
        }
        fmLocationListener?.locationManager(arFrame, accumulatedARCoreInfo, frameEventAccumulator)
    }
}
