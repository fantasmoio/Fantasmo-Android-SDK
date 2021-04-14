package com.fantasmo.sdk.filters

import com.google.ar.core.Frame

class FMMovementFilter : FMFrameFilter{
    val threshold = 0.25
    private lateinit var lastTransform : FloatArray

    override fun accepts(arFrame: Frame): FMFilterResult {
        return if(exceededThreshold(arFrame.camera.pose.translation)){
            lastTransform = arFrame.camera.pose.translation
            FMFilterResult.ACCEPTED
        }else FMFilterResult.REJECTED//(FMFilterRejectionReason.MOVINGTOOLITTLE)
    }

    private fun exceededThreshold(transform: FloatArray?): Boolean {
        return true
    }
}