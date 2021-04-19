package com.fantasmo.sdk.frameSequenceFilter

import android.util.Log
import com.google.ar.core.Frame
import org.opencv.android.OpenCVLoader

class FMFrameSequenceFilter {

    private val TAG = "FMFrameSequenceGuard"

    // the last time a frame was accepted
    var timestampOfPreviousApprovedFrame : Long = 0

    // number of seconds after which we force acceptance
    var acceptanceThreshold = 6.0

    private var rules = listOf(
        FMCameraPitchFilterRule(),
        FMMovementFilterRule(),
        FMBlurFilterRule()
    )

    fun prepareForNewFrameSequence() {
        OpenCVLoader.initDebug() //init OpenCV process
        timestampOfPreviousApprovedFrame = 0
    }


    fun check(arFrame : Frame) : Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        if(!shouldForceApprove(arFrame)) { //DEBUG: Force Filters to work on frame receiving
        //if(shouldForceApprove(arFrame)) {
            timestampOfPreviousApprovedFrame = arFrame.timestamp
            Log.d(TAG, "shouldForceAccept True")
            return Pair(FMFrameFilterResult.ACCEPTED,FMFrameFilterFailure.ACCEPTED)
        }
        else{
            Log.d(TAG, "shouldForceAccept False")
            for(rule in rules){
                val result = rule.check(arFrame)
                Log.d(TAG, "$rule, $result")
                if(result.first != FMFrameFilterResult.ACCEPTED){
                    Log.d(TAG, "accepts -> False")
                    return result
                }
            }
        }
        timestampOfPreviousApprovedFrame = arFrame.timestamp
        Log.d(TAG, "accepts -> True")
        return Pair(FMFrameFilterResult.ACCEPTED,FMFrameFilterFailure.ACCEPTED)
    }

    private fun shouldForceApprove(arFrame: Frame): Boolean {
        if(timestampOfPreviousApprovedFrame != 0L){
            val elapsed = arFrame.timestamp - timestampOfPreviousApprovedFrame
            return elapsed > acceptanceThreshold
        }
        return false
    }
}