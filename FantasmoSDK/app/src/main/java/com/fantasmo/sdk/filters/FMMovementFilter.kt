package com.fantasmo.sdk.filters

import com.google.ar.core.Frame
import kotlin.math.abs

/**
 * Class responsible for filtering frames due to the lack of movement.
 * Prevents from sending repeated values to the backend.
 * Initializes with sideways movement Threshold in meters
 */
class FMMovementFilter(private val movementFilterThreshold: Float) : FMFrameFilter {
    // Previous frame translation
    private var lastTransform: FloatArray = FloatArray(16){0f}
    /**
     * Check frame acceptance.
     * @param arFrame Frame to be evaluated
     * @return Accepts frame or Rejects frame with MovingTooLittle failure
     */
    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        val newTransform = FloatArray(16)
        arFrame.camera.pose.toMatrix(newTransform, 0)
        return if (exceededThreshold(newTransform)) {
            lastTransform = newTransform
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
        newTransform?.forEachIndexed { index, value ->
            if (abs(value - lastTransform[index]) > movementFilterThreshold) {
                return true
            }
        }
        return false
    }
}