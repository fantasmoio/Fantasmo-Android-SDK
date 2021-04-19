package com.fantasmo.sdk.frameSequenceFilter

import com.fantasmo.sdk.FMBehaviorRequest
import com.google.ar.core.Frame

interface FMFrameSequenceFilterRule{
    fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure>
}

enum class FMFrameFilterFailure {
    PITCHTOOLOW,
    PITCHTOOHIGH,
    MOVINGTOOFAST,
    MOVINGTOOLITTLE,
    ACCEPTED
}

enum class FMFrameFilterResult{
    ACCEPTED,
    REJECTED;
}

fun mapToBehaviourRequest(rejection:FMFrameFilterFailure): FMBehaviorRequest {
    return when (rejection){
        FMFrameFilterFailure.PITCHTOOLOW -> FMBehaviorRequest.TILTUP
        FMFrameFilterFailure.PITCHTOOHIGH -> FMBehaviorRequest.TILTDOWN
        FMFrameFilterFailure.MOVINGTOOFAST -> FMBehaviorRequest.PANSLOWLY
        FMFrameFilterFailure.MOVINGTOOLITTLE -> FMBehaviorRequest.PANAROUND
        else -> FMBehaviorRequest.ACCEPTED
    }
}