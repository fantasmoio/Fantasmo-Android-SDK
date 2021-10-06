package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import com.google.ar.core.Frame

enum class FMFilterRejectionReason {
    PITCHTOOLOW,
    PITCHTOOHIGH,
    IMAGETOOBLURRY,
    MOVINGTOOFAST,
    MOVINGTOOLITTLE,
    INSUFFICIENTFEATURES;

    /**
     * Method responsible for mapping a FrameFilterFailure to the end user.
     * @return: FMBehaviorRequest corresponding to and instruction to the end-user
     */
    fun mapToBehaviourRequest(): FMBehaviorRequest {
        return when (this) {
            PITCHTOOLOW -> FMBehaviorRequest.TILTUP
            PITCHTOOHIGH -> FMBehaviorRequest.TILTDOWN
            MOVINGTOOFAST -> FMBehaviorRequest.PANSLOWLY
            IMAGETOOBLURRY -> FMBehaviorRequest.PANSLOWLY
            MOVINGTOOLITTLE -> FMBehaviorRequest.PANAROUND
            INSUFFICIENTFEATURES -> FMBehaviorRequest.PANAROUND
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
    fun accepts(arFrame: Frame): FMFrameFilterResult
}