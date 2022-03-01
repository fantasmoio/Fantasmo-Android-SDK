package com.fantasmo.sdk.models.analytics

import com.fantasmo.sdk.filters.FMFrameFilterRejectionReason

/**
 * Class responsible for gathering information about frame filtration rejection.
 */
class FrameFilterRejectionStatistics {

    private var totalFrameCount = 0
    var excessiveTiltFrameCount = 0
    var insufficientTiltFrameCount = 0
    var excessiveBlurFrameCount = 0
    var insufficientMotionFrameCount = 0
    var insufficientFeatures = 0
    var excessiveMotionFrameCount = 0
    var imageQualityFrameCount = 0
    var frameErrorCount = 0


    /**
     * During shouldLocalize call, frames are filtered from rejected and accepted.
     * When a frame is rejected, this method gets the rejection failure and updates
     * the information about the localization session regarding frame filtration.
     * @param result FMFrameFilterFailure
     */
    fun accumulate(result: FMFrameFilterRejectionReason) {
        totalFrameCount += 1
        when (result) {
            FMFrameFilterRejectionReason.MovingTooFast -> {
                excessiveMotionFrameCount += 1
            }
            FMFrameFilterRejectionReason.ImageTooBlurry -> {
                excessiveBlurFrameCount += 1
            }
            FMFrameFilterRejectionReason.MovingTooLittle -> {
                insufficientMotionFrameCount += 1
            }
            FMFrameFilterRejectionReason.PitchTooHigh -> {
                insufficientTiltFrameCount += 1
            }
            FMFrameFilterRejectionReason.PitchTooLow -> {
                excessiveTiltFrameCount += 1
            }
            FMFrameFilterRejectionReason.InsufficientFeatures -> {
                insufficientFeatures += 1
            }
            FMFrameFilterRejectionReason.ImageQualityScoreBelowThreshold -> {
                imageQualityFrameCount += 1
            }
            FMFrameFilterRejectionReason.FrameError -> {
                frameErrorCount += 1
            }
        }
    }

    /**
     * Resets counters on a new localization session.
     */
    fun reset() {
        totalFrameCount = 0
        excessiveTiltFrameCount = 0
        excessiveBlurFrameCount = 0
        insufficientTiltFrameCount = 0
        insufficientMotionFrameCount = 0
        insufficientFeatures = 0
        imageQualityFrameCount = 0
        frameErrorCount = 0
    }
}