package com.fantasmo.sdk.models.analytics

import android.util.Log
import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterFailure

/**
 * Class responsible for gathering information about frame filtration rejection
 */
class FrameFilterRejectionStatistics {

    private val TAG = "FrameFilterStatistics"

    private var totalFrameCount = 0
    private var excessiveTiltFrameCount = 0
    private var excessiveBlurFrameCount = 0
    private var insufficientMotionFrameCount = 0
    private var insufficientFeatures = 0

    /**
     * During shouldLocalize call, frames are filtered from rejected and accepted
     * When a frame is rejected, this method gets the rejection failure and updates
     * the information about the localization session regarding frame filtration
     * @param result: FMFrameFilterFailure
     */
    fun accumulate(result: FMFrameFilterFailure) {
        totalFrameCount += 1
        when (result) {
            FMFrameFilterFailure.MOVINGTOOFAST -> {
                excessiveBlurFrameCount += 1
            }
            FMFrameFilterFailure.MOVINGTOOLITTLE -> {
                insufficientMotionFrameCount += 1
            }
            FMFrameFilterFailure.PITCHTOOHIGH -> {
                excessiveTiltFrameCount += 1
            }
            FMFrameFilterFailure.PITCHTOOLOW -> {
                excessiveTiltFrameCount += 1
            }
            FMFrameFilterFailure.INSUFFICIENTFEATURES -> {
                insufficientFeatures += 1
            }
        }
        Log.d(TAG,"Tilt: $excessiveTiltFrameCount, " +
                "Blur: $excessiveBlurFrameCount, " +
                "Motion: $insufficientMotionFrameCount, " +
                "Insufficient Features: $insufficientFeatures, " +
                "Total: $totalFrameCount")
    }

    /**
     * Resets counters on a new localization session
     */
    fun reset() {
        totalFrameCount = 0
        excessiveTiltFrameCount = 0
        excessiveBlurFrameCount = 0
        insufficientMotionFrameCount = 0
        insufficientFeatures = 0
    }
}