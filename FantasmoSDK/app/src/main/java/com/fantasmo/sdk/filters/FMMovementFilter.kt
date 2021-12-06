package com.fantasmo.sdk.filters

import com.fantasmo.sdk.models.FMPosition
import com.google.ar.core.Frame
import kotlin.math.abs

/**
 * Class responsible for filtering frames due to the lack of movement.
 * Prevents from sending repeated values to the backend.
 * Initializes with sideways movement Threshold in meters
 */
class FMMovementFilter(private val movementFilterThreshold: Float) : FMFrameFilter {
    // Previous frame translation
    private var lastTransform: FloatArray = floatArrayOf(0F, 0F, 0F)

    /**
     * Check frame acceptance.
     * @param arFrame Frame to be evaluated
     * @return Accepts frame or Rejects frame with MovingTooLittle failure
     */
    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        return if (exceededThreshold(arFrame.camera.pose.translation)) {
            lastTransform = arFrame.camera.pose.translation
            FMFrameFilterResult.Accepted
        } else {
            FMFrameFilterResult.Rejected(FMFilterRejectionReason.MOVINGTOOLITTLE)
        }
    }

    /**
     * Check if frame translation has exceeded threshold
     * @param newTransform Frame translation
     * @return If frame translation is within (false) or without threshold (true)
     */
    private fun exceededThreshold(newTransform: FloatArray?): Boolean {
        val diff = FMPosition.minus(FMPosition(lastTransform), FMPosition(newTransform!!))
        return ((abs(diff.x) > movementFilterThreshold)
                || (abs(diff.y) > movementFilterThreshold)
                || (abs(diff.z) > movementFilterThreshold))
    }
}