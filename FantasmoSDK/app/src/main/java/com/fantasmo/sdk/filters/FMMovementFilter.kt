package com.fantasmo.sdk.filters

import com.fantasmo.sdk.models.FMFrame
import kotlin.math.abs

/**
 * Class responsible for filtering frames due to the lack of movement.
 * Prevents from sending repeated values to the backend.
 * Initializes with sideways movement Threshold in meters
 */
class FMMovementFilter(private val movementFilterThreshold: Float) : FMFrameFilter {
    override val TAG = FMMovementFilter::class.java.simpleName

    // Previous frame translation
    private var lastTransform: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f)

    /**
     * Check frame acceptance.
     * @param arFrame Frame to be evaluated
     * @return Accepts frame or Rejects frame with MovingTooLittle failure
     */
    override fun accepts(fmFrame: FMFrame): FMFrameFilterResult {

         val newTransform = FloatArray(16)
        fmFrame.cameraPose?.toMatrix(newTransform, 0)
        
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