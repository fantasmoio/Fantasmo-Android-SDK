package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.ar.core.Frame

/**
 * Class responsible for filtering frames according the implemented filters
 */
class FMInputQualityFilter(context: Context) {

    private val TAG = "FMInputQualityFilter"

    // the last time a frame was accepted
    var lastAcceptTime: Long = 0L

    // number of seconds after which we force acceptance
    var acceptanceThreshold = 3.0

    /**
     * List of filter rules to apply on frame received.
     */
    var filters = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        listOf(
            FMTrackingStateFilter(),
            FMCameraPitchFilter(context),
            FMMovementFilter(),
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
        lastAcceptTime = 0L
    }

    /**
     * Check if frame is valid to determine localize result.
     * @param arFrame: Frame for approval.
     * @return result: Pair<FMFrameFilterResult, FMFrameFilterFailure>
     */
    fun accepts(arFrame: Frame): FMFrameFilterResult {
        if (shouldForceAccept(arFrame)) {
            lastAcceptTime = arFrame.timestamp
            return FMFrameFilterResult.Accepted
        } else {
            for (filter in filters) {
                val result = filter.accepts(arFrame)
                if (result != FMFrameFilterResult.Accepted) {
                    return result
                }
            }
        }
        lastAcceptTime = arFrame.timestamp
        return FMFrameFilterResult.Accepted
    }


    /**
     * Method to Force Approve frame if a certain time has passed in case
     * of every frame in that period was refused
     * @param arFrame: Frame for approval
     */
    private fun shouldForceAccept(arFrame: Frame): Boolean {
        if (lastAcceptTime != 0L) {
            //convert to seconds (Frame timestamp is in nanoseconds)
            val elapsed = (arFrame.timestamp - lastAcceptTime) / 1000000000
            return elapsed > acceptanceThreshold
        }
        return false
    }
}