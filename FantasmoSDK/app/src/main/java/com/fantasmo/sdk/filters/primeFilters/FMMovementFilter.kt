package com.fantasmo.sdk.filters.primeFilters

import com.fantasmo.sdk.models.FMPosition
import com.google.ar.core.Frame
import kotlin.math.abs

/**
 * Class responsible for filtering frames due to the lack of movement.
 * Prevents from sending repeated values to the backend.
 */
class FMMovementFilter : FMFrameFilter {
    // Sideways movement threshold
    private val threshold = 0.02 //0.25
    // Previous frame translation
    private var lastTransform: FloatArray = floatArrayOf(0F, 0F, 0F)

    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with MovingTooLittle failure
     */
    override fun accepts(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        return if (exceededThreshold(arFrame.camera.pose.translation)) {
            lastTransform = arFrame.camera.pose.translation
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
        } else {
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE)
        }
    }

    /**
     * Check if frame translation has exceeded threshold
     * @param newTransform: Frame translation
     * @return If frame translation is within(false) or without threshold(true)
     */
    private fun exceededThreshold(newTransform: FloatArray?): Boolean {
        val diff = FMPosition.minus(FMPosition(lastTransform), FMPosition(newTransform!!))
        return ((abs(diff.x) > threshold)
                || (abs(diff.y) > threshold)
                || (abs(diff.z) > threshold))
    }
}