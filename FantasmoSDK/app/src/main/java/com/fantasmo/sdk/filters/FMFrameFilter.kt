package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.models.FMFrame

enum class FMFrameFilterRejectionReason {
    PitchTooLow,
    PitchTooHigh,
    ImageTooBlurry,
    MovingTooFast,
    MovingTooLittle,
    InsufficientFeatures,
    ImageQualityScoreBelowThreshold,
    FrameError;

    /**
     * Method responsible for mapping a `FMFilterRejectionReason to the end user.
     * @return `FMBehaviorRequest` corresponding to and instruction to the end-user
     */
    fun mapToBehaviorRequest(): FMBehaviorRequest {
        return when (this) {
            PitchTooLow -> FMBehaviorRequest.TiltUp
            PitchTooHigh -> FMBehaviorRequest.TiltDown
            MovingTooFast -> FMBehaviorRequest.PanSlowly
            ImageTooBlurry -> FMBehaviorRequest.PanSlowly
            MovingTooLittle -> FMBehaviorRequest.PanAround
            InsufficientFeatures -> FMBehaviorRequest.PanAround
            ImageQualityScoreBelowThreshold -> FMBehaviorRequest.PanAround
            FrameError -> FMBehaviorRequest.PointAtBuildings
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