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
import com.fantasmo.sdk.evaluators.FMFrameEvaluationType
import com.fantasmo.sdk.evaluators.FMFrameEvaluatorChain
import com.fantasmo.sdk.evaluators.FMFrameEvaluatorChainListener
import com.fantasmo.sdk.filters.BehaviorRequester
import com.fantasmo.sdk.filters.FMFrameFilterRejectionReason
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FMFrameEvaluationStatistics
import com.fantasmo.sdk.models.analytics.MotionManager
import com.fantasmo.sdk.network.*
import com.fantasmo.sdk.utilities.DeviceLocationManager
import com.fantasmo.sdk.utilities.LocationFuser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class FMLocationManager(private val context: Context) : FMFrameEvaluatorChainListener{
    private val TAG = "FMLocationManager"

    enum class State {
        // doing nothing
        STOPPED,

        // localizing
        LOCALIZING
    }

    private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private var locationFuser = LocationFuser()

    lateinit var fmApi: FMApi

    var state = State.STOPPED

    var anchorFrame: FMFrame? = null
    var currentLocation: Location = Location()

    private var fmLocationListener: FMLocationListener? = null
    private var token: String = ""

    /// When in simulation mode, mock data is used from the assets directory instead of the live camera feed.
    /// This mode is useful for implementation and debugging.
    var isSimulation = false

    private var isConnected = false

    var activeUploads: MutableList<FMFrame> = mutableListOf()
    private set

    // Used to validate frame for sufficient quality before sending to API.
    private lateinit var frameEvaluatorChain: FMFrameEvaluatorChain

    // Throttler for invalid frames.
    private lateinit var behaviorRequester: BehaviorRequester

    private var motionManager = MotionManager(context)

    // Localization Session Id generated on each startUpdatingLocation call
    private lateinit var localizationSessionId: String

    // App Session Id supplied by the SDK client
    private lateinit var appSessionId: String

    // App Session Tags supplied by the SDK client
    private var appSessionTags : List<String>? = null

    private var frameEvaluationStatistics = FMFrameEvaluationStatistics(FMFrameEvaluationType.IMAGE_QUALITY_ESTIMATION)
    private var accumulatedARCoreInfo = AccumulatedARCoreInfo()

    private lateinit var rc: RemoteConfig.Config

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
        rc = RemoteConfig.remoteConfig
        frameEvaluatorChain = FMFrameEvaluatorChain(rc, context)
        if (rc.isBehaviorRequesterEnabled) {
            behaviorRequester = BehaviorRequester {
                fmLocationListener?.didRequestBehavior(behavior = it)
            }
        }
        fmLocationListener?.didChangeState(state)
    }

    /**
     * Sets currentLocation with values given by the client application.
     *
     * @param location Android Location Object.
     */
    fun setLocation(location: android.location.Location) {
        val coordinate = Coordinate(location.latitude, location.longitude)
        this.currentLocation.coordinate = coordinate
        this.currentLocation.altitude = location.altitude
        this.currentLocation.timestamp = location.time.toDouble() / 1000.0
        this.currentLocation.horizontalAccuracy = location.accuracy

        this.currentLocation.verticalAccuracy =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                location.verticalAccuracyMeters
            } else {
                0.0f
            }

        Log.d(TAG, "SetLocation: $currentLocation")
    }

    /**
     * Starts the generation of updates that report the user’s current location
     * enabling FrameFiltering
     * @param appSessionId sessionId supplied by the SDK client and used for billing and tracking an entire parking session
     * @param appSessionTags sessionTags supplied by the SDK client and used to label and group parking sessions that have something in common
     */
    fun startUpdatingLocation(appSessionId: String, appSessionTags: List<String>?) {
        localizationSessionId = UUID.randomUUID().toString()
        this.appSessionId = appSessionId
        this.appSessionTags = appSessionTags
        Log.d(
            TAG,
            "startUpdatingLocation with AppSessionId:$appSessionId, AppSessionTags:$appSessionTags and LocalizationSessionId:$localizationSessionId"
        )

        this.isConnected = true
        this.state = State.LOCALIZING
        fmLocationListener?.didChangeState(state)
        motionManager.restart()
        accumulatedARCoreInfo.reset()

        frameEvaluatorChain.listener = this
        frameEvaluatorChain.resetWindow()

        if (rc.isBehaviorRequesterEnabled) {
            this.behaviorRequester.restart()
        }
        this.locationFuser.reset()
        frameEvaluationStatistics.reset()
    }

    /**
     * Stops the generation of location updates.
     */
    fun stopUpdatingLocation() {
        Log.d(TAG, "stopUpdatingLocation")
        motionManager.stop()
        this.state = State.STOPPED
        fmLocationListener?.didChangeState(state)
    }

    /**
     * Set an anchor point. All location updates will now report the
     * location of the anchor instead of the camera.
     * @param [fmFrame] an AR Frame to use as anchor.
     */
    fun setAnchor(fmFrame: FMFrame) {
        Log.d(TAG, "setAnchor")

        this.anchorFrame = fmFrame
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
     * @param fmFrame an FMFrame to localize
     */
    private fun localize(fmFrame: FMFrame) {
        if (!isConnected) {
            return
        }
        if (!DeviceLocationManager.isValidLatLng(
                currentLocation.coordinate.latitude,
                currentLocation.coordinate.longitude
            )
        ) {
            val error = ErrorResponse(0, "Invalid Coordinates")
            fmLocationListener?.didFailWithError(error, null)
            Log.e(TAG, "Invalid Coordinates")
            return
        }
        Log.d(TAG, "localize: isSimulation $isSimulation")
        coroutineScope.launch {
            fmLocationListener?.didChangeState(state)
            val localizeRequest = createLocalizationRequest(fmFrame)
            fmLocationListener?.didBeginUpload(fmFrame)
            activeUploads.add(fmFrame)
            fmApi.sendLocalizeRequest(
                fmFrame,
                localizeRequest,
                { localizeResponse, fmZones ->
                    Log.d(TAG, "localize: $localizeResponse, Zones $fmZones")
                    val result = locationFuser.fusedResult(localizeResponse, fmZones)
                    activeUploads.removeAll { it == fmFrame }
                    fmLocationListener?.didUpdateLocation(
                        result
                    )
                    updateStateAfterLocalization()
                },
                { error ->
                    Log.e(TAG, "localize: $error")
                    activeUploads.removeAll { it == fmFrame }
                    fmLocationListener?.didFailWithError(error, null)
                    updateStateAfterLocalization()
                })

        }
    }

    /**
     * Gather all the information needed to assemble a LocalizationRequest.
     */
    private fun createLocalizationRequest(fmFrame: FMFrame): FMLocalizationRequest {
        val frameEvents = FMFrameEvent(
            frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.PITCH_TOO_LOW] ?: 0
                    + (frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.PITCH_TOO_HIGH] ?: 0),
            frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.IMAGE_TOO_BLURRY] ?: 0,
            frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.MOVING_TOO_FAST] ?: 0,
            frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.INSUFFICIENT_FEATURES] ?: 0,
            (accumulatedARCoreInfo.trackingStateFrameStatistics.framesWithLimitedTrackingState
                    + accumulatedARCoreInfo.trackingStateFrameStatistics.framesWithNotAvailableTracking
                    ),
            accumulatedARCoreInfo.elapsedFrames
        )
        val rotationSpread = FMRotationSpread(
            accumulatedARCoreInfo.rotationAccumulator.pitch[2],
            accumulatedARCoreInfo.rotationAccumulator.roll[2],
            accumulatedARCoreInfo.rotationAccumulator.yaw[2]
        )

        val gamma = fmFrame.enhancedImageGamma
        val imageEnhancementInfo: FMImageEnhancementInfo? = if (gamma == null) {
            null
        } else {
            FMImageEnhancementInfo(gamma)
        }
        val frameAnalytics = FMLocalizationAnalytics(
            appSessionId,
            appSessionTags,
            localizationSessionId,
            frameEvents,
            rotationSpread,
            accumulatedARCoreInfo.translationAccumulator.totalTranslation,
            motionManager.magneticField,
            imageEnhancementInfo,
            rc.remoteConfigId
        )
        val openCVRelativeAnchorPose = anchorFrame?.let { anchorFrame ->
            FMUtility.anchorDeltaPoseForFrame(
                fmFrame,
                anchorFrame
            )
        }

        return FMLocalizationRequest(
            isSimulation,
            FMZone.ZoneType.PARKING,
            currentLocation,
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
            fmLocationListener?.didChangeState(state)
        }
    }

    /**
     * Method to check whether the SDK is ready to localize a frame or not.
     */
    fun session(fmFrame: FMFrame) {
        if (state != State.STOPPED) {
            // run the frame through the configured filters
            frameEvaluatorChain.evaluateAsync(fmFrame)
        }
        val frameToLocalize = frameEvaluatorChain.dequeueBestFrame()
        if (frameToLocalize != null)  {
            localize(frameToLocalize)
        }

        fmLocationListener?.didUpdateFrame(fmFrame, accumulatedARCoreInfo)
        accumulatedARCoreInfo.update(fmFrame)
    }

    override fun didFinishEvaluatingFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame) {
        frameEvaluationStatistics.addEvaluation(frame)
        fmLocationListener?.didUpdateFrameEvaluationStatistics(frameEvaluationStatistics)
    }

    override fun didEvaluateNewBestFrame(frameEvaluatorChain: FMFrameEvaluatorChain, newBestFrame: FMFrame) {
        // evaluator found a new best frame, show info in debug view
        frameEvaluationStatistics.setCurrentBest(newBestFrame)
        fmLocationListener?.didUpdateFrameEvaluationStatistics(frameEvaluationStatistics)
    }

    override fun didStartWindow(frameEvaluatorChain: FMFrameEvaluatorChain, startTime: Double) {
        frameEvaluationStatistics.startWindow(startTime)
        fmLocationListener?.didUpdateFrameEvaluationStatistics(frameEvaluationStatistics)
    }

    override fun didRejectFrameWhileEvaluatingOtherFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame:FMFrame, otherFrame: FMFrame) {
        // evaluator was busy and discarded the frame, show info in debug view

    }

    override fun didRejectFrameWithFilterReason(
        frameEvaluatorChain: FMFrameEvaluatorChain,
        frame: FMFrame,
        reason: FMFrameFilterRejectionReason
    ) {
        // evaluator filter rejected the frame, show info in debug view
        frameEvaluationStatistics.addFilterRejection(reason)
        fmLocationListener?.didUpdateFrameEvaluationStatistics(frameEvaluationStatistics)
        behaviorRequester.processFilterRejection(reason)
    }

    override fun didEvaluateFrameBelowMinScoreThreshold(
        frameEvaluatorChain: FMFrameEvaluatorChain,
        frame: FMFrame,
        minScoreThreshold: Float
    ) {
        // evaluator rejected the frame because it was below the min score threshold, show info in debug view
    }

    override fun didEvaluateFrameBelowCurrentBestScore(
        frameEvaluatorChain: FMFrameEvaluatorChain,
        frame: FMFrame,
        currentBestScore: Float
    ) {
        // evaluator rejected the frame because it was below the current best score, show info in debug view
    }
}
