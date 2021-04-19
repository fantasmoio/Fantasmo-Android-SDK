package com.fantasmo.sdk.validators

import com.fantasmo.sdk.models.FMPosition
import com.google.ar.core.Frame

class FMMovementValidator : FMFrameValidator{
    private val threshold = 0.25
    private var lastTransform : FloatArray = floatArrayOf(0F, 0F, 0F)

    override fun accepts(arFrame: Frame): Pair<FMValidatorResult,FMFrameValidationError> {
        return if(exceededThreshold(arFrame.camera.pose.translation)){
            lastTransform = arFrame.camera.pose.translation
            Pair(FMValidatorResult.ACCEPTED,FMFrameValidationError.ACCEPTED)
        }else{
            Pair(FMValidatorResult.REJECTED,FMFrameValidationError.MOVINGTOOLITTLE)
        }
    }

    private fun exceededThreshold(newTransform: FloatArray?): Boolean {
        val diff = FMPosition.minus(FMPosition(lastTransform), newTransform?.let { FMPosition(it) }!!)
        return diff.x < threshold && diff.y < threshold && diff.z < threshold
    }
}