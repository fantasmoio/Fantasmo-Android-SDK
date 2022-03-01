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
    fun didEvaluateFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame)
    fun didFindNewBestFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame)
    fun didDiscardFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame)
    fun didRejectFrameWithFilterReason(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, reason: FMFrameFilterRejectionReason)
    fun didRejectFrameBelowMinScoreThreshold(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, minScoreThreshold: Float)
    fun didRejectFrameBelowCurrentBestScore(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, currentBestScore: Float)
}

class FMFrameEvaluatorChain (remoteConfig: RemoteConfig.Config, context: Context) {
    val TAG: String = FMFrameEvaluatorChain::javaClass.name

    private val minWindowTime: Float = 0.4f
    private val maxWindowTime: Float = 1.2f
    private val minScoreThreshold: Float = 0.0f
    private val minHighQualityScore: Float = 0.9f
    private val frameEvaluator: FMFrameEvaluator
    private val preEvaluationFilters : MutableList<FMFrameFilter>
    private val defaultCoroutineScope = CoroutineScope(Dispatchers.Default)
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main)

    /// Image enhancer, applies gamma correction, nil if disabled via remote config
    private val imageEnhancer: FMImageEnhancer?

    private var currentBestFrame: FMFrame? = null

    private var windowStart: Double

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

        frameEvaluator = FMImageQualityEvaluator.makeEvaluator(context)

        windowStart = System.nanoTime() / n2s
    }


    fun evaluateAsync(fmFrame: FMFrame) {
        if(isEvaluatingFrame) {
            listener?.didDiscardFrame(this,
                fmFrame)
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
                listener?.didRejectFrameWithFilterReason(this, fmFrame, reason)
            return
        }

        // set a flag so we can only process one frame at a time
        isEvaluatingFrame = true

        // begin async stuff
        defaultCoroutineScope.launch {
            // enhance image, apply gamma correction if too dark
            imageEnhancer?.enhance(fmFrame)

            // evaluate the frame using the configured evaluator
            val evaluation = frameEvaluator.evaluate(fmFrame)

            mainCoroutineScope.launch {
                // finish evaluation on the main thread
                processEvaluation(evaluation, fmFrame)
                // unset flag to allow new frames to be evaluated
                isEvaluatingFrame = false
            }
        }
    }

    private fun processEvaluation(evaluation: FMFrameEvaluation, fmFrame: FMFrame) {
        if (!isEvaluatingFrame) {
            error("not evaluating frame")
        }

        // store the evaluation on the frame and notify the delegate
        fmFrame.evaluation = evaluation
        listener?.didEvaluateFrame(this, fmFrame)

        // check if the frame is above the min score threshold, otherwise return
        if (evaluation.score < minScoreThreshold) {
            listener?.didRejectFrameBelowMinScoreThreshold(this, fmFrame, minScoreThreshold)
            return
        }

        // check if the new frame score is better than our current best frame score, otherwise return
        val currentBestEvaluation = currentBestFrame?.evaluation
        if(currentBestEvaluation != null && currentBestEvaluation.score > evaluation.score) {
            listener?.didRejectFrameBelowCurrentBestScore(this, fmFrame, currentBestEvaluation.score)
            return
        }

        // frame is the new best, update our saved reference and notify the delegate
        currentBestFrame = fmFrame
        listener?.didFindNewBestFrame(this, fmFrame)
    }

    fun dequeueBestFrame() : FMFrame? {
        val evaluation = currentBestFrame?.evaluation
        if (currentBestFrame == null || evaluation == null) {
            return null
        }
        val currentTime = System.nanoTime() / n2s
        val timeElapsed = currentTime - windowStart
        if (timeElapsed < minWindowTime) {
            return null
        }

        if (evaluation.score >= minHighQualityScore || timeElapsed >= maxWindowTime) {
            reset()
            return currentBestFrame
        }

        return null
    }

    fun reset() {
        // TODO - reset window state, close current window etc.
        windowStart = System.nanoTime() / n2s
        currentBestFrame = null
    }

    //TODO - Implement
}