package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.models.FMFrame

enum class FMFrameFilterRejectionReason {
    PITCH_TOO_LOW,
    PITCH_TOO_HIGH,
    IMAGE_TOO_BLURRY,
    MOVING_TOO_FAST,
    MOVING_TOO_LITTLE,
    INSUFFICIENT_FEATURES,
    IMAGE_QUALITY_SCORE_BELOW_THRESHOLD,
    FRAME_ERROR;

    /**
     * Method responsible for mapping a `FMFilterRejectionReason to the end user.
     * @return `FMBehaviorRequest` corresponding to and instruction to the end-user
     */
    fun mapToBehaviorRequest(): FMBehaviorRequest {
        return when (this) {
            PITCH_TOO_LOW -> FMBehaviorRequest.TILT_UP
            PITCH_TOO_HIGH -> FMBehaviorRequest.TILT_DOWN
            MOVING_TOO_FAST -> FMBehaviorRequest.PAN_SLOWLY
            IMAGE_TOO_BLURRY -> FMBehaviorRequest.PAN_SLOWLY
            MOVING_TOO_LITTLE -> FMBehaviorRequest.PAN_AROUND
            INSUFFICIENT_FEATURES -> FMBehaviorRequest.PAN_AROUND
            IMAGE_QUALITY_SCORE_BELOW_THRESHOLD -> FMBehaviorRequest.PAN_AROUND
            FRAME_ERROR -> FMBehaviorRequest.POINT_AT_BUILDINGS
        }
    }
}

sealed class FMFrameFilterResult {
    object Accepted : FMFrameFilterResult()
    class Rejected(val reason: FMFrameFilterRejectionReason): FMFrameFilterResult()

    fun getRejectedReason(): FMFrameFilterRejectionReason? {
        return when (this){
            is Rejected -> reason
            else -> null
        }
    }
}

/**
 * Prime filters are original blocks for a compound frame filter or can be used alone as a standalone filter.
 */
interface FMFrameFilter {
    val TAG: String
    fun accepts(fmFrame: FMFrame): FMFrameFilterResult
}