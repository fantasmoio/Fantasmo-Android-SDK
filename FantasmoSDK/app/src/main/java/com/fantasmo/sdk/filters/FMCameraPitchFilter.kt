package com.fantasmo.sdk.filters

import android.util.Log
import com.google.ar.core.Frame
import kotlin.math.PI
import kotlin.math.abs

class FMCameraPitchFilter : FMFrameFilter {
    private val TAG = "FMCameraPitchFilter"
    private val radianThreshold = (PI.toFloat())/8

    override fun accepts(arFrame: Frame): Pair<FMFilterResult,FMFilterRejectionReason> {
        Log.d(TAG,"accepts")
        val x = arFrame.camera.pose.rotationQuaternion[0]
        return when {
            x<=radianThreshold -> {
                Pair(FMFilterResult.ACCEPTED,FMFilterRejectionReason.ACCEPTED)
            }
            x>0 -> {
                Pair(FMFilterResult.REJECTED,FMFilterRejectionReason.PITCHTOOHIGH)
            }
            else -> {
                Pair(FMFilterResult.REJECTED,FMFilterRejectionReason.PITCHTOOLOW)
            }
        }
    }
}
