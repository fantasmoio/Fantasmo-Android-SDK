package com.fantasmo.sdk.frameSequenceFilter

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.ar.core.Frame

/**
 * Class responsible for filtering frames according the implemented filters
 */
class FMFrameSequenceFilter(context: Context) {

    private val TAG = "FMFrameSequenceFilter"

    // the last time a frame was accepted
    var timestampOfPreviousApprovedFrame: Long = 0L

    // number of seconds after which we force acceptance
    var acceptanceThreshold = 6.0

    /**
     * List of filter rules to apply on frame received.
     */
    var rules = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        listOf(
                FMCameraPitchFilterRule(),
                FMMovementFilterRule(),
                FMBlurFilterRule(context)
        )
    } else {
        listOf(
            FMCameraPitchFilterRule(),
            FMMovementFilterRule(),
        )
    }

    /**
     * Init a new Sequence of frames
     */
    fun prepareForNewFrameSequence() {
        timestampOfPreviousApprovedFrame = 0L
    }

    /**
     * Check if frame is valid to determine localize result.
     * @param arFrame: Frame for approval.
     * @return result: Pair<FMFrameFilterResult, FMFrameFilterFailure>
     */
    fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        if (shouldForceApprove(arFrame)) {
            timestampOfPreviousApprovedFrame = arFrame.timestamp
            Log.d(TAG, "shouldForceAccept True")
            return Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
        } else {
            for (rule in rules) {
                val result = rule.check(arFrame)
                if (result.first != FMFrameFilterResult.ACCEPTED) {
                    Log.d(TAG, "RULE_CHECK: Frame not accepted $result")
                    return result
                }
            }
        }
        timestampOfPreviousApprovedFrame = arFrame.timestamp
        return Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
    }


    /**
     * Method to Force Approve frame if a certain time has passed in case
     * of every frame in that period was refused
     * @param arFrame: Frame for approval
     */
    private fun shouldForceApprove(arFrame: Frame): Boolean {
        if (timestampOfPreviousApprovedFrame != 0L) {
            //convert to seconds (Frame timestamp is in nanoseconds)
            val elapsed = (arFrame.timestamp - timestampOfPreviousApprovedFrame) / 1000000000
            return elapsed > acceptanceThreshold
        }
        return false
    }
}