package com.fantasmo.sdk.evaluators

import android.content.Context
import android.os.Build
import android.util.Log
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.filters.*
import com.fantasmo.sdk.models.FMFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface FMFrameEvaluatorChainListener {
    fun didStartWindow(frameEvaluatorChain: FMFrameEvaluatorChain,  startTime: Double)
    fun didRejectFrameWhileEvaluatingOtherFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, otherFrame: FMFrame)
    fun didRejectFrameWithFilterReason(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, reason: FMFrameFilterRejectionReason)
    fun didEvaluateNewBestFrame(frameEvaluatorChain: FMFrameEvaluatorChain, newBestFrame: FMFrame)
    fun didFinishEvaluatingFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame)
    fun didEvaluateFrameBelowMinScoreThreshold(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, minScoreThreshold: Float)
    fun didEvaluateFrameBelowCurrentBestScore(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, currentBestScore: Float)
}

class FMFrameEvaluatorChain (remoteConfig: RemoteConfig.Config, context: Context) {
    val TAG: String = FMFrameEvaluatorChain::class.java.simpleName

    private val frameEvaluator: FMFrameEvaluator
    private val filters : MutableList<FMFrameFilter> = mutableListOf()
    private val defaultCoroutineScope = CoroutineScope(Dispatchers.Default)
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main)

    /// Image enhancer, applies gamma correction, nil if disabled via remote config
    private val imageEnhancer: FMImageEnhancer?

    private var currentBestFrame: FMFrame? = null

    private var evaluatingFrame: FMFrame? = null

    private var windowStart: Double

    private var minWindowTime: Float

    private var maxWindowTime: Float

    private var minScoreThreshold: Float

    private var minHighQualityScore: Float

    var listener: FMFrameEvaluatorChainListener? = null

    private val n2s = 1_000_000_000.0

    init {
        // TODO - get these from remote config

        if (remoteConfig.isTrackingStateFilterEnabled) {
            filters.add(FMTrackingStateFilter())
        }
        if (remoteConfig.isCameraPitchFilterEnabled) {
            val cameraPitchFilter = FMCameraPitchFilter(
                remoteConfig.cameraPitchFilterMaxDownwardTilt,
                remoteConfig.cameraPitchFilterMaxUpwardTilt,
                context
            )
            filters.add(cameraPitchFilter)
        }
        if (remoteConfig.isMovementFilterEnabled) {
            val movementFilter = FMMovementFilter(
                remoteConfig.movementFilterThreshold
            )
            filters.add(movementFilter)
        }

        // configure the image enhancer, if enabled
        imageEnhancer = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH && remoteConfig.isImageEnhancerEnabled) {
            FMImageEnhancer(remoteConfig.imageEnhancerTargetBrightness, context)
        } else {
            null
        }

        frameEvaluator = FMImageQualityEvaluator.makeEvaluator(context)

        minWindowTime = remoteConfig.minLocalizationWindowTime
        maxWindowTime = remoteConfig.maxLocalizationWindowTime
        minScoreThreshold = remoteConfig.minFrameEvaluationScore
        minHighQualityScore = remoteConfig.minFrameEvaluationHighQualityScore

        windowStart = System.nanoTime() / n2s
    }


    fun evaluateAsync(fmFrame: FMFrame) {
        if(evaluatingFrame != null) {
            Log.d(TAG, "Already evaluating frame, rejecting frame ${fmFrame.timestamp}")
            evaluatingFrame?.let {
                listener?.didRejectFrameWhileEvaluatingOtherFrame(this, fmFrame,
                    it
                )
            }
            return
        }

        // run frame through filters
        var filterResult: FMFrameFilterResult = FMFrameFilterResult.Accepted
        for (filter in filters) {
            filterResult = filter.accepts(fmFrame)
            if (filterResult != FMFrameFilterResult.Accepted) {
                break
            }
        }

        // if any filter rejects, throw frame away
        if (filterResult != FMFrameFilterResult.Accepted) {
            val reason = filterResult.getRejectedReason()
            Log.d(TAG, "Frame ${fmFrame.timestamp} rejected for reason $reason")
            if(reason != null)
                listener?.didRejectFrameWithFilterReason(this, fmFrame, reason)
            return
        }

        // set a flag so we can only process one frame at a time
        evaluatingFrame = fmFrame
        // begin async stuff
        defaultCoroutineScope.launch {
            // enhance image, apply gamma correction if too dark
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                imageEnhancer?.enhance(fmFrame)
            }

            // evaluate the frame using the configured evaluator
            val evaluation = frameEvaluator.evaluate(fmFrame)

            mainCoroutineScope.launch {
                // finish evaluation on the main thread
                processEvaluation(evaluation)
            }
        }
    }

    private fun processEvaluation(evaluation: FMFrameEvaluation) {
        val fmFrame = evaluatingFrame ?: error("evaluating frame is null")

        // store the evaluation on the frame and notify the delegate
        fmFrame.evaluation = evaluation
        listener?.didFinishEvaluatingFrame(this, fmFrame)
        val currentBestScore = currentBestFrame?.evaluation?.score
        // check if the frame is above the min score threshold, otherwise return
        if (evaluation.score < minScoreThreshold) {
            Log.d(TAG, "Frame ${fmFrame.timestamp} score ${evaluation.score} below threshold")
            listener?.didEvaluateFrameBelowMinScoreThreshold(this, fmFrame, minScoreThreshold)
        }
        // check if the new frame score is better than our current best frame score, otherwise return

        else if(currentBestScore != null && currentBestScore > evaluation.score) {
            Log.d(TAG, "Frame ${fmFrame.timestamp} score ${evaluation.score} below current best score")
            listener?.didEvaluateFrameBelowCurrentBestScore(this, fmFrame, currentBestScore)
        }
        else {
            // frame is the new best, update our saved reference and notify the delegate
            Log.d(TAG, "Frame ${fmFrame.timestamp} score ${evaluation.score} new best")
            currentBestFrame = fmFrame
            listener?.didEvaluateNewBestFrame(this, fmFrame)
        }

        evaluatingFrame = null
        listener?.didFinishEvaluatingFrame(this, fmFrame)
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
            Log.d(TAG, "Time elapsed $timeElapsed, max window time $maxWindowTime\nscore ${evaluation.score}, min high quality score $minHighQualityScore, dequeuing frame")
            val returnFrame = currentBestFrame
            resetWindow()
            return returnFrame
        }
        return null
    }

    fun resetWindow() {
        windowStart = System.nanoTime() / n2s
        currentBestFrame = null
        listener?.didStartWindow(this, windowStart)
    }

}