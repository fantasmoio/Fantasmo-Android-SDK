package com.fantasmo.sdk.frameSequenceFilter

import com.google.ar.core.Frame
import kotlin.math.abs

/**
 * Class responsible for filtering frames due to critical angles.
 * Prevents from sending ground and sky images which have no characteristics
 * to determine location
 */
class FMCameraPitchFilterRule : FMFrameSequenceFilterRule {
    // Maximum value for tilting phone up or down
    private val radianThreshold = 0.20

    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with PitchTooHigh or PitchTooLow failure
     */
    override fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        // Angle of X-plane of device sensor system (Y-Axis up).
        val x = arFrame.camera.displayOrientedPose.rotationQuaternion[0]

        return when {
            abs(x) <= radianThreshold -> {
                Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
            }
            x < 0 -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
            }
            else -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
            }
        }
    }
}
