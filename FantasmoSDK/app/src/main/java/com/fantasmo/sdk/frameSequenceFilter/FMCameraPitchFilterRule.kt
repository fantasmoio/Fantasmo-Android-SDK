package com.fantasmo.sdk.frameSequenceFilter

import com.google.ar.core.Frame
import kotlin.math.PI

class FMCameraPitchFilterRule : FMFrameSequenceFilterRule {
    private val radianThreshold = (PI.toFloat())/8

    override fun check(arFrame: Frame): Pair<FMFrameFilterResult,FMFrameFilterFailure> {
        val x = arFrame.camera.pose.rotationQuaternion[0]
        return when {
            x<=radianThreshold -> {
                Pair(FMFrameFilterResult.ACCEPTED,FMFrameFilterFailure.ACCEPTED)
            }
            x>0 -> {
                Pair(FMFrameFilterResult.REJECTED,FMFrameFilterFailure.PITCHTOOHIGH)
            }
            else -> {
                Pair(FMFrameFilterResult.REJECTED,FMFrameFilterFailure.PITCHTOOLOW)
            }
        }
    }
}
