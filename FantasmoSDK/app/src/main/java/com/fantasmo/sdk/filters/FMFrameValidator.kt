package com.fantasmo.sdk.filters

import com.google.ar.core.Frame

interface FMFrameValidator{
    fun accepts(arFrame: Frame): Pair<FMValidatorResult, FMFrameValidationError>
}

enum class FMFrameValidationError {
    PITCHTOOLOW,
    PITCHTOOHIGH,
    MOVINGTOOFAST,
    MOVINGTOOLITTLE,
    ACCEPTED
}

enum class FMValidatorResult{
    ACCEPTED,
    REJECTED;
}

fun mapToBehaviourRequest(rejection:FMFrameValidationError):FMBehaviorRequest{
    return when (rejection){
        FMFrameValidationError.PITCHTOOLOW -> FMBehaviorRequest.TILTUP
        FMFrameValidationError.PITCHTOOHIGH -> FMBehaviorRequest.TILTDOWN
        FMFrameValidationError.MOVINGTOOFAST -> FMBehaviorRequest.PANSLOWLY
        FMFrameValidationError.MOVINGTOOLITTLE -> FMBehaviorRequest.PANAROUND
        else -> FMBehaviorRequest.ACCEPTED
    }
}