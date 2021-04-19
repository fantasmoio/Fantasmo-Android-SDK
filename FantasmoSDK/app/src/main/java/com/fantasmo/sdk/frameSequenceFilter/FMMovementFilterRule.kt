package com.fantasmo.sdk.frameSequenceFilter

import android.util.Log
import com.fantasmo.sdk.models.FMPosition
import com.google.ar.core.Frame
import kotlin.math.abs

class FMMovementFilterRule : FMFrameSequenceFilterRule{
    private val TAG = "FMMovementFilterRule"
    private val threshold = 0.04 //0.25 is too big to test with simulation
    private var lastTransform : FloatArray = floatArrayOf(0F, 0F, 0F)

    override fun check(arFrame: Frame): Pair<FMFrameFilterResult,FMFrameFilterFailure> {
        return if(exceededThreshold(arFrame.camera.pose.translation)){
            lastTransform = arFrame.camera.pose.translation
            Pair(FMFrameFilterResult.ACCEPTED,FMFrameFilterFailure.ACCEPTED)
        }else{
            Pair(FMFrameFilterResult.REJECTED,FMFrameFilterFailure.MOVINGTOOLITTLE)
        }
    }

    private fun exceededThreshold(newTransform: FloatArray?): Boolean {
        Log.d(TAG,"exceededThreshold: ${FMPosition(lastTransform)}, ${FMPosition(newTransform!!)}")
        val diff = FMPosition.minus(FMPosition(lastTransform), FMPosition(newTransform))
        Log.d(TAG,"exceededThreshold DIFF: $diff")
        return !((abs(diff.x) < threshold)
                && (abs(diff.y) < threshold)
                && (abs(diff.z) < threshold))
    }
}