package com.fantasmo.sdk.frameSequenceFilter

import com.google.ar.core.Frame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin

class FMCameraPitchFilterRule : FMFrameSequenceFilterRule {
    private val radianThreshold = (PI.toFloat())/8

    override fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        val x = arFrame.camera.pose.rotationQuaternion[0]
        val y = arFrame.camera.pose.rotationQuaternion[1]
        val z = arFrame.camera.pose.rotationQuaternion[2]
        val w = arFrame.camera.pose.rotationQuaternion[3]

        val pitch: Float = asin(
            -2 * (x * z - y * w)/(x * x + y * y + z * z + w * w)
        )

        return when {
            abs(pitch)<=radianThreshold -> {
                Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
            }
            pitch>0 -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
            }
            else -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
            }
        }
    }
}
