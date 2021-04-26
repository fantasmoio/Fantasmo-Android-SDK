package com.fantasmo.sdk.frameSequenceFilter

import android.util.Log
import com.fantasmo.sdk.models.FMPosition
import com.google.ar.core.Frame
import kotlin.math.abs

class FMMovementFilterRule : FMFrameSequenceFilterRule {
    private val TAG = "FMMovementFilterRule"
    private val threshold = 0.01 //0.25
    private var lastTransform: FloatArray = floatArrayOf(0F, 0F, 0F)

    override fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        return if (exceededThreshold(arFrame.camera.pose.translation)) {
            lastTransform = arFrame.camera.pose.translation
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
        } else {
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE)
        }
    }

    private fun exceededThreshold(newTransform: FloatArray?): Boolean {
        val diff = FMPosition.minus(FMPosition(lastTransform), FMPosition(newTransform!!))
        return !((abs(diff.x) < threshold)
                && (abs(diff.y) < threshold)
                && (abs(diff.z) < threshold))
    }
}