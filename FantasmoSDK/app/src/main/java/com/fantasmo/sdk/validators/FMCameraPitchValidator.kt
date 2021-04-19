package com.fantasmo.sdk.validators

import com.google.ar.core.Frame
import kotlin.math.PI

class FMCameraPitchValidator : FMFrameValidator {
    private val radianThreshold = (PI.toFloat())/8

    override fun accepts(arFrame: Frame): Pair<FMValidatorResult,FMFrameValidationError> {
        val x = arFrame.camera.pose.rotationQuaternion[0]
        return when {
            x<=radianThreshold -> {
                Pair(FMValidatorResult.ACCEPTED,FMFrameValidationError.ACCEPTED)
            }
            x>0 -> {
                Pair(FMValidatorResult.REJECTED,FMFrameValidationError.PITCHTOOHIGH)
            }
            else -> {
                Pair(FMValidatorResult.REJECTED,FMFrameValidationError.PITCHTOOLOW)
            }
        }
    }
}
