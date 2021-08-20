package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.util.Log
import com.fantasmo.sdk.FMUtility.Companion.n2s
import com.fantasmo.sdk.filters.primeFilters.*
import com.fantasmo.sdk.filters.primeFilters.FMCameraPitchFilter
import com.google.ar.core.Frame

/**
 * Class responsible for filtering frames according the implemented filters
 */
class FMCompoundFrameQualityFilter(context: Context) {

    private val TAG = "FMFrameSequenceFilter"

    // the last time a frame was accepted
    var lastAcceptTime: Long = System.nanoTime()

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
        lastAcceptTime = System.nanoTime()
    }

    /**
     * Check if frame is valid to determine localize result.
     * @param arFrame: Frame for approval.
     * @return result: Pair<FMFrameFilterResult, FMFrameFilterFailure>
     */
    fun accepts(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        if (shouldForceAccept()) {
            lastAcceptTime = System.nanoTime()
            Log.d(TAG, "shouldForceAccept True")
            return Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
        } else {
            for (filter in filters) {
                val result = filter.accepts(arFrame)
                if (result.first != FMFrameFilterResult.ACCEPTED) {
                    Log.d(TAG, "RULE_CHECK: Frame not accepted $result")
                    return result
                }
            }
        }
        lastAcceptTime = System.nanoTime()
        return Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
    }


    /**
     * Method to Force Approve frame if a certain time has passed in case
     * of every frame in that period was refused
     * @return result: Boolean
     */
    private fun shouldForceAccept(): Boolean {
        val elapsed = (System.nanoTime() - lastAcceptTime) / n2s
        Log.d(TAG,"Elapsed: $elapsed; LastAcceptTime: $lastAcceptTime")
        return (elapsed > acceptanceThreshold)
    }

}