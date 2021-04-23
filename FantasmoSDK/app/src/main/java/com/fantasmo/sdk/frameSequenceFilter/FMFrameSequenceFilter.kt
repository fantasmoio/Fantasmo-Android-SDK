package com.fantasmo.sdk.frameSequenceFilter

import android.util.Log
import com.google.ar.core.Frame

class FMFrameSequenceFilter {

    private val TAG = "FMFrameSequenceFilter"

    // the last time a frame was accepted
    var timestampOfPreviousApprovedFrame: Long = 0L

    // number of seconds after which we force acceptance
    var acceptanceThreshold = 6.0

    private var rules = listOf(
        FMCameraPitchFilterRule(),
        FMMovementFilterRule(),
        FMBlurFilterRule()
    )

    fun prepareForNewFrameSequence() {
        timestampOfPreviousApprovedFrame = 0L
    }

    fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        if (shouldForceApprove(arFrame)) {
            timestampOfPreviousApprovedFrame = arFrame.timestamp
            Log.d(TAG, "shouldForceAccept True")
            return Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
        } else {
            Log.d(TAG, "shouldForceAccept False")
            for (rule in rules) {
                val result = rule.check(arFrame)
                Log.d(TAG, "$rule, $result")
                if (result.first != FMFrameFilterResult.ACCEPTED) {
                    Log.d(TAG, "RULE_CHECK: Frame not accepted $result")
                    return result
                }
            }
        }
        timestampOfPreviousApprovedFrame = arFrame.timestamp
        return Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
    }

    private fun shouldForceApprove(arFrame: Frame): Boolean {
        if (timestampOfPreviousApprovedFrame != 0L) {
            //convert to seconds (Frame timestamp is in nanoseconds)
            val elapsed = (arFrame.timestamp - timestampOfPreviousApprovedFrame) / 1000000000
            return elapsed > acceptanceThreshold
        }
        return false
    }
}