package com.fantasmo.sdk.filters

import android.util.Log
import com.fantasmo.sdk.models.FMPosition
import com.google.ar.core.Frame

class FMMovementFilter : FMFrameFilter{
    private val TAG = "FMMovementFilter"
    private val threshold = 0.25
    private var lastTransform : FloatArray = floatArrayOf(0F, 0F, 0F)

    override fun accepts(arFrame: Frame): FMFilterResult {
        Log.d(TAG,"accepts")
        return if(exceededThreshold(arFrame.camera.pose.translation)){
            lastTransform = arFrame.camera.pose.translation
            FMFilterResult.ACCEPTED
        }else{
            val result = FMFilterResult.REJECTED
            result.rejection = FMFilterRejectionReason.MOVINGTOOLITTLE
            result
        }
    }

    private fun exceededThreshold(newTransform: FloatArray?): Boolean {
        val diff = FMPosition.minus(FMPosition(lastTransform), newTransform?.let { FMPosition(it) }!!)
        return diff.x < threshold && diff.y < threshold && diff.z < threshold
    }
}