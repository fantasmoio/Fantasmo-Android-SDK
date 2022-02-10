package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.models.FMFrame

enum class FMFilterRejectionReason {
    PITCHTOOLOW,
    PITCHTOOHIGH,
    IMAGETOOBLURRY,
    MOVINGTOOFAST,
    MOVINGTOOLITTLE,
    INSUFFICIENTFEATURES,
    IMAGEQUALITYSCOREBELOWTHRESHOLD,
    FRAMEERROR;

    /**
     * Method responsible for mapping a `FMFilterRejectionReason to the end user.
     * @return `FMBehaviorRequest` corresponding to and instruction to the end-user
     */
    fun mapToBehaviorRequest(): FMBehaviorRequest {
        return when (this) {
            PITCHTOOLOW -> FMBehaviorRequest.TILTUP
            PITCHTOOHIGH -> FMBehaviorRequest.TILTDOWN
            MOVINGTOOFAST -> FMBehaviorRequest.PANSLOWLY
            IMAGETOOBLURRY -> FMBehaviorRequest.PANSLOWLY
            MOVINGTOOLITTLE -> FMBehaviorRequest.PANAROUND
            INSUFFICIENTFEATURES -> FMBehaviorRequest.PANAROUND
            IMAGEQUALITYSCOREBELOWTHRESHOLD -> FMBehaviorRequest.PANAROUND
            FRAMEERROR -> FMBehaviorRequest.POINTATBUILDINGS
        }
    }
}

sealed class FMFrameFilterResult {
    object Accepted : FMFrameFilterResult()
    class Rejected(val reason: FMFilterRejectionReason): FMFrameFilterResult()

    fun getRejectedReason(): FMFilterRejectionReason? {
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