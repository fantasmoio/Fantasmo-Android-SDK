package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.ar.core.Frame

/**
 * Class responsible for filtering frames according the implemented filters
 */
class FMFrameFilterChain(context: Context) {

    private val TAG = FMFrameFilterChain::class.java.simpleName

    // the last time a frame was accepted
    private var lastAcceptTime: Long = System.nanoTime()

    // number of seconds after which we force acceptance
    private var acceptanceThreshold = 1.0

    /**
     * List of filter rules to apply on frame received.
     */
    var filters = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        listOf(
            FMTrackingStateFilter(),
            FMCameraPitchFilter(context),
            FMMovementFilter(),
            FMImageQualityFilter(context),
            FMBlurFilter(context)
        )
    } else {
        listOf(
            FMTrackingStateFilter(),
            FMCameraPitchFilter(context),
            FMMovementFilter(),
        )
    }

    /**
     * Init a new Sequence of frames
     */
    fun restart() {
        lastAcceptTime = System.nanoTime()
    }

    /**
     * Check if frame is valid to determine localize result.
     * @param arFrame Frame for approval.
     * @return result FMFrameFilterResult
     */
    fun accepts(arFrame: Frame): FMFrameFilterResult {
        if (shouldForceAccept()) {
            lastAcceptTime = System.nanoTime()
            return FMFrameFilterResult.Accepted
        } else {
            for (filter in filters) {
                val result = filter.accepts(arFrame)
                if (result != FMFrameFilterResult.Accepted) {
                    return result
                }
            }
        }
        lastAcceptTime = System.nanoTime()
        return FMFrameFilterResult.Accepted
    }


    /**
     * Method to Force Approve frame if a certain time has passed in case
     * of every frame in that period was refused
     * @return result: Boolean
     */
    private fun shouldForceAccept(): Boolean {
        //convert to seconds (timestamp is in nanoseconds)
        val elapsed = (System.nanoTime() - lastAcceptTime) / 1_000_000_000
        return elapsed > acceptanceThreshold
    }

    fun evaluateAsync(arFrame: Frame, completion: (FMFrameFilterResult) -> Unit) {
        completion(accepts(arFrame))
    }
}