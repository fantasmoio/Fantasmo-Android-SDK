package com.fantasmo.sdk.filters

import android.util.Log
import com.google.ar.core.Frame
import kotlin.math.PI
import kotlin.math.abs

class FMCameraPitchFilter : FMFrameFilter {
    private val TAG = "FMCameraPitchFilter"
    private val radianThreshold = (PI)/(8.0f)

    override fun accepts(arFrame: Frame): FMFilterResult {
        Log.d(TAG,"accepts")
        val x = arFrame.camera.pose.rotationQuaternion[0]
        return when {
            x<=radianThreshold -> {
                FMFilterResult.ACCEPTED
            }
            x>0 -> {
                val result = FMFilterResult.REJECTED
                result.rejection = FMFilterRejectionReason.PITCHTOOHIGH
                result
            }
            else -> {
                val result = FMFilterResult.REJECTED
                result.rejection = FMFilterRejectionReason.PITCHTOOLOW
                result
            }
        }
    }
}
