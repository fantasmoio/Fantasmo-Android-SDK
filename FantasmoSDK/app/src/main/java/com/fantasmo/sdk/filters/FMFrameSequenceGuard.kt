package com.fantasmo.sdk.filters

import android.content.Context
import android.util.Log
import com.fantasmo.sdk.FMLocationListener
import com.google.ar.core.Frame
//import org.opencv.android.OpenCVLoader //Decomment after OpenCV installation

class FMFrameSequenceGuard(fmLocationListener: FMLocationListener, context: Context) {

    private val TAG = "FMInputQualityFilter"
    private val locationListener = fmLocationListener

    // the last time a frame was accepted
    var timestampOfPreviousApprovedFrame : Long = 0

    // number of seconds after which we force acceptance
    var acceptanceThreshold = 6.0

    private var filters = listOf(
        FMCameraPitchValidator(),
        FMMovementValidator(),
        //FMBlurValidator()
    )

    fun startFiltering(){
        //OpenCVLoader.initDebug() //init OpenCV process
        prepareForNewFrameSequence()
    }

    private fun prepareForNewFrameSequence() {
        timestampOfPreviousApprovedFrame = 0
    }

    fun accepts(arFrame : Frame) : Boolean{
        if(!shouldForceApprove(arFrame)) {
            timestampOfPreviousApprovedFrame = arFrame.timestamp
            Log.d(TAG, "shouldForceAccept True")
            return true
        }
        else{
            Log.d(TAG, "shouldForceAccept False")
            for(filter in filters){
                val result = filter.accepts(arFrame)
                Log.d(TAG, "$filter, $result")
                if(result.first != FMValidatorResult.ACCEPTED){
                    Log.d(TAG, "accepts -> False")
                    return false
                }
            }
        }
        timestampOfPreviousApprovedFrame = arFrame.timestamp
        Log.d(TAG, "accepts -> True")
        return true
    }

    private fun shouldForceApprove(arFrame: Frame): Boolean {
        if(timestampOfPreviousApprovedFrame != 0L){
            val elapsed = arFrame.timestamp - timestampOfPreviousApprovedFrame
            return elapsed > acceptanceThreshold
        }
        return false
    }
}