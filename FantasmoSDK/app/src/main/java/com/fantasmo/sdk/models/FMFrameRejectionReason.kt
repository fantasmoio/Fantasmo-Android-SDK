package com.fantasmo.sdk.models

import com.fantasmo.sdk.FMBehaviorRequest

enum class FMFrameRejectionReason {
    // filter rejections
    PITCH_TOO_LOW,
    PITCH_TOO_HIGH,
    MOVING_TOO_FAST,
    MOVING_TOO_LITTLE,
    TRACKING_STATE_INITIALIZING,
    TRACKING_STATE_RELOCALIZING,
    TRACKING_STATE_EXCESSIVE_MOTION,
    TRACKING_STATE_INSUFFICIENT_FEATURES,
    TRACKING_STATE_NOT_AVAILABLE,
    IMAGE_QUALITY_SCORE_BELOW_THRESHOLD,
    // evaluator rejections
    OTHER_EVALUATION_IN_PROGRESS,
    SCORE_BELOW_CURRENT_BEST,
    SCORE_BELOW_MIN_THRESHOLD,
    FRAME_ERROR;
    /**
     * Method responsible for mapping a `FMFilterRejectionReason to the end user.
     * @return `FMBehaviorRequest` corresponding to and instruction to the end-user
     */
    fun mapToBehaviorRequest(): FMBehaviorRequest {
        return when (this) {
            PITCH_TOO_LOW -> FMBehaviorRequest.TILT_UP
            PITCH_TOO_HIGH -> FMBehaviorRequest.TILT_DOWN
            TRACKING_STATE_EXCESSIVE_MOTION -> FMBehaviorRequest.PAN_SLOWLY
            MOVING_TOO_FAST -> FMBehaviorRequest.PAN_SLOWLY
            else -> {FMBehaviorRequest.PAN_AROUND}
        }
    }
}

