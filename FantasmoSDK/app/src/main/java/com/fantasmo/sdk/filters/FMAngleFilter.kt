package com.fantasmo.sdk.filters

import com.google.ar.core.Frame
import kotlin.math.PI

class FMAngleFilter : FMFrameFilter {
    private val radianThreshold = (PI)/(8.0f)

    override fun accepts(arFrame: Frame): FMFilterResult {
        val x = arFrame.camera.pose.rotationQuaternion[1]
        return when {
            x > radianThreshold -> {
                return FMFilterResult.REJECTED//(FMFilterRejectionReason.PITCHTOOHIGH)
            }
            x < -radianThreshold -> {
                return FMFilterResult.REJECTED//(FMFilterRejectionReason.PITCHTOOLOW)
            }
            else -> FMFilterResult.ACCEPTED
        }
    }
}
