package com.fantasmo.sdk.evaluators

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.filters.*
import com.fantasmo.sdk.models.FMFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface FMFrameEvaluatorChainListener {
    fun didEvaluateFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, result: FMFrameEvaluationResult)
}

class FMFrameEvaluatorChain
    (remoteConfig: RemoteConfig.Config, context: Context) : FMFrameEvaluator {
    override val TAG: String = FMFrameEvaluatorChain::javaClass.name

    private val minWindowTime: Float = 0.4f
    private val maxWindowTime: Float = 1.2f
    private val minScoreThreshold: Float = 0.0f
    private val minHighQualityScore: Float = 0.9f
    private val frameEvaluator: FMFrameEvaluator?
    private val preEvaluationFilters : MutableList<FMFrameFilter>
    private val defaultCoroutineScope = CoroutineScope(Dispatchers.Default)
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main)

    /// Image enhancer, applies gamma correction, nil if disabled via remote config
    private val imageEnhancer: FMImageEnhancer?

    private var currentBestFrame: FMFrame? = null

    private var windowStart: Double? = null

    private var isEvaluatingFrame: Boolean = false

    var listener: FMFrameEvaluatorChainListener? = null

    private val n2s = 1_000_000_000.0

    init {
        // TODO - get these from remote config

        preEvaluationFilters = mutableListOf()

        if (remoteConfig.isTrackingStateFilterEnabled) {
            preEvaluationFilters.add(FMTrackingStateFilter())
        }
        if (remoteConfig.isCameraPitchFilterEnabled) {
            val cameraPitchFilter = FMCameraPitchFilter(
                remoteConfig.cameraPitchFilterMaxDownwardTilt,
                remoteConfig.cameraPitchFilterMaxUpwardTilt,
                context
            )
            preEvaluationFilters.add(cameraPitchFilter)
        }
        if (remoteConfig.isMovementFilterEnabled) {
            val movementFilter = FMMovementFilter(
                remoteConfig.movementFilterThreshold
            )
            preEvaluationFilters.add(movementFilter)
        }

        // configure the image enhancer, if enabled
        @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
        imageEnhancer = if (remoteConfig.isImageEnhancerEnabled) {
            FMImageEnhancer(remoteConfig.imageEnhancerTargetBrightness, context)
        } else {
            null
        }

        @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
        frameEvaluator = FMImageQualityEvaluator(context)
    }


    override fun evaluate(fmFrame: FMFrame) {
        if (windowStart == null) {
            windowStart = System.nanoTime() / n2s
        }

        if(isEvaluatingFrame) {
            listener?.didEvaluateFrame(this,
                fmFrame,
                FMFrameEvaluationResult.Discarded(FMFrameEvaluationDiscardReason.OtherEvaluationInProgress))
            return
        }

        // run frame through filters
        var filterResult: FMFrameFilterResult = FMFrameFilterResult.Accepted
        preEvaluationFilters.forEach {
            filterResult = it.accepts(fmFrame)
            if (filterResult != FMFrameFilterResult.Accepted) {
                return@forEach
            }
        }

        // if any filter rejects, throw frame away
        if (filterResult != FMFrameFilterResult.Accepted) {
            val reason = filterResult.getRejectedReason()
            if(reason != null)
                listener?.didEvaluateFrame(this, fmFrame, FMFrameEvaluationResult.Discarded(FMFrameEvaluationDiscardReason.RejectedByFilter(reason)))
            return
        }

        // set a flag so we can only process one frame at a time
        isEvaluatingFrame = true

        // begin async stuff
        defaultCoroutineScope.launch {
            // enhance image, apply gamma correction if too dark
            imageEnhancer?.enhance(fmFrame)

            // evaluate the frame using the configured evaluator
            frameEvaluator?.evaluate(fmFrame)

            mainCoroutineScope.launch {
                // finish evaluation on the main thread
                finishEvaluation(fmFrame)
                // unset flag to allow new frames to be evaluated
                isEvaluatingFrame = false
            }
        }
    }

    private fun finishEvaluation(fmFrame: FMFrame) {
        if (!isEvaluatingFrame) {
            error("not evaluating frame")
        }

        val evaluation = fmFrame.evaluation
        if(evaluation == null) {
            if (currentBestFrame?.evaluation == null) {
                currentBestFrame = fmFrame
                listener?.didEvaluateFrame(this, fmFrame, FMFrameEvaluationResult.NewCurrentBest)
            } else {
                listener?.didEvaluateFrame(this, fmFrame, FMFrameEvaluationResult.Discarded(FMFrameEvaluationDiscardReason.EvaluatorError))
            }
            return
        }

        // check if the frame is above the min score threshold, otherwise throw it away
        if (evaluation.score < minScoreThreshold) {
            listener?.didEvaluateFrame(this, fmFrame, FMFrameEvaluationResult.Discarded(FMFrameEvaluationDiscardReason.BelowMinScoreThreshold))
            return
        }

        // check if the new frame is better than our current best, otherwise throw it away
        val currentBestEvaluation = currentBestFrame?.evaluation
        if(currentBestEvaluation != null && currentBestEvaluation.score > evaluation.score) {
            listener?.didEvaluateFrame(this, fmFrame, FMFrameEvaluationResult.Discarded(FMFrameEvaluationDiscardReason.BelowCurrentBestScore))
        }

        // update our current best frame
        currentBestFrame = fmFrame
        listener?.didEvaluateFrame(this, fmFrame, FMFrameEvaluationResult.NewCurrentBest)
    }

    fun dequeueBestFrame() : FMFrame? {
        val evaluation = currentBestFrame?.evaluation
        if (windowStart == null || currentBestFrame == null || evaluation == null) {
            return null
        }
        val currentTime = System.nanoTime() / n2s
        val timeElapsed = currentTime - windowStart!!
        if (timeElapsed < minWindowTime) {
            return null
        }

        if (evaluation.score >= minHighQualityScore || timeElapsed >= maxWindowTime) {
            currentBestFrame = null
            windowStart = currentTime
            return currentBestFrame
        }

        return null
    }

    fun reset() {
        // TODO - reset window state, close current window etc.
        windowStart = null
        currentBestFrame = null
    }

    fun <T> getFilterOfType(type: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return preEvaluationFilters.first{type.isInstance(it)} as T?
    }

    //TODO - Implement
}