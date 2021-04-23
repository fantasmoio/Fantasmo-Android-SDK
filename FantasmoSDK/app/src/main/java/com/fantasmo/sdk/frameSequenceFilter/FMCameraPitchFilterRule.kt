package com.fantasmo.sdk.frameSequenceFilter

import com.google.ar.core.Frame
import kotlin.math.abs

class FMCameraPitchFilterRule : FMFrameSequenceFilterRule {
    private val radianThreshold = 0.20

    override fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        val x = arFrame.androidSensorPose.rotationQuaternion[0]

        return when {
            abs(x) <= radianThreshold -> {
                Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
            }
            x > 0 -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
            }
            else -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
            }
        }
    }
}
