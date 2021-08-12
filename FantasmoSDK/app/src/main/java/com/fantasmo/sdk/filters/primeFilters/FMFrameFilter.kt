package com.fantasmo.sdk.filters.primeFilters

import com.fantasmo.sdk.filters.FMBehaviorRequest
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
    ACCEPTED
}

enum class FMFrameFilterResult {
    ACCEPTED,
    REJECTED;
}

/**
 * Method responsible for mapping a FrameFilterFailure to the end user.
 * @param rejection: FrameFilterFailure
 * @return: FMBehaviorRequest corresponding to and instruction to the end-user
 */
fun mapToBehaviourRequest(rejection: FMFrameFilterFailure): FMBehaviorRequest {
    return when (rejection) {
        FMFrameFilterFailure.PITCHTOOLOW -> FMBehaviorRequest.TILTUP
        FMFrameFilterFailure.PITCHTOOHIGH -> FMBehaviorRequest.TILTDOWN
        FMFrameFilterFailure.MOVINGTOOFAST -> FMBehaviorRequest.PANSLOWLY
        FMFrameFilterFailure.MOVINGTOOLITTLE -> FMBehaviorRequest.PANAROUND
        FMFrameFilterFailure.INSUFFICIENTFEATURES -> FMBehaviorRequest.PANAROUND
        else -> FMBehaviorRequest.ACCEPTED
    }
}