package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import com.google.ar.core.Frame

/**
 * Defines rules for the implemented filters.
 */
interface FMFrameFilter {
    fun accepts(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure>
}

enum class FMFrameFilterFailure {
    PITCHTOOLOW,
    PITCHTOOHIGH,
    MOVINGTOOFAST,
    MOVINGTOOLITTLE,
    INSUFFICIENTFEATURES,
    ACCEPTED;

    /**
     * Method responsible for mapping a FrameFilterFailure to the end user.
     * @return: FMBehaviorRequest corresponding to and instruction to the end-user
     */
    fun mapToBehaviourRequest(): FMBehaviorRequest {
        return when (this) {
            PITCHTOOLOW -> FMBehaviorRequest.TILTUP
            PITCHTOOHIGH -> FMBehaviorRequest.TILTDOWN
            MOVINGTOOFAST -> FMBehaviorRequest.PANSLOWLY
            MOVINGTOOLITTLE -> FMBehaviorRequest.PANAROUND
            INSUFFICIENTFEATURES -> FMBehaviorRequest.PANAROUND
            else -> FMBehaviorRequest.ACCEPTED
        }
    }
}

enum class FMFrameFilterResult {
    ACCEPTED,
    REJECTED;
}


