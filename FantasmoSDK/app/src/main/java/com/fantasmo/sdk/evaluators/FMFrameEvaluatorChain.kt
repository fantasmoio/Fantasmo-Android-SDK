package com.fantasmo.sdk.evaluators

import android.content.Context
import android.os.Build
import android.util.Log
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.filters.*
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.FMFrameRejectionReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface FMFrameEvaluatorChainListener {
    fun didStartWindow(frameEvaluatorChain: FMFrameEvaluatorChain,  startTime: Double)
    fun didRejectFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, reason: FMFrameRejectionReason)
    fun didRejectFrameWithFilter(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame, filter:FMFrameFilter, reason: FMFrameRejectionReason)
    fun didEvaluateNewBestFrame(frameEvaluatorChain: FMFrameEvaluatorChain, newBestFrame: FMFrame)
    fun didFinishEvaluatingFrame(frameEvaluatorChain: FMFrameEvaluatorChain, frame: FMFrame)
}

class FMFrameEvaluatorChain (remoteConfig: RemoteConfig.Config, context: Context) {
    val TAG: String = FMFrameEvaluatorChain::class.java.simpleName

    val frameEvaluator: FMFrameEvaluator

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
            listener?.didRejectFrame(this, fmFrame, FMFrameRejectionReason.OTHER_EVALUATION_IN_PROGRESS)
            return
        }

        // run frame through filters
        var filterResult: FMFrameFilterResult = FMFrameFilterResult.Accepted
        for (filter in filters) {
            filterResult = filter.accepts(fmFrame)
            if (filterResult != FMFrameFilterResult.Accepted) {
                filterResult.getRejectedReason()
                    ?.let { listener?.didRejectFrameWithFilter(this, fmFrame, filter, it) }
                return
            }
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
            val start = System.nanoTime()
            val evaluation = frameEvaluator.evaluate(fmFrame)
            Log.d(TAG, "Evaluation took ${(System.nanoTime() - start).toDouble() / 1_000_000} ms")

            mainCoroutineScope.launch {
                // finish evaluation on the main thread
                processEvaluation(evaluation)
            }
        }
    }

    private fun processEvaluation(evaluation: FMFrameEvaluation) {
        val fmFrame = evaluatingFrame
        if (fmFrame == null) {
            Log.e(TAG, "null frame when processing evaluation")
            return
        }

        // store the evaluation on the frame and notify the delegate
        fmFrame.evaluation = evaluation
        val currentBestScore = currentBestFrame?.evaluation?.score
        // check if the frame is above the min score threshold, otherwise return
        if (evaluation.score < minScoreThreshold) {
            Log.d(TAG, "Frame ${fmFrame.timestamp} score ${evaluation.score} below threshold")
            listener?.didRejectFrame(this, fmFrame, FMFrameRejectionReason.SCORE_BELOW_MIN_THRESHOLD)
        }
        // check if the new frame score is better than our current best frame score, otherwise return

        else if(currentBestScore != null && currentBestScore > evaluation.score) {
            Log.d(TAG, "Frame ${fmFrame.timestamp} score ${evaluation.score} below current best score")
            listener?.didRejectFrame(this, fmFrame, FMFrameRejectionReason.SCORE_BELOW_CURRENT_BEST)
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