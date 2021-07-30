package com.fantasmo.sdk.models.analytics

import android.util.Log
import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterFailure

class FrameFilterRejectionStatisticsAccumulator {

    private val TAG = "FrameFilterStatistics"

    private var totalFrameCount = 0
    private var excessiveTiltFrameCount = 0
    private var excessiveBlurFrameCount = 0
    private var insufficientMotionFrameCount = 0

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
        }
        Log.d(TAG,"Tilt: $excessiveTiltFrameCount, " +
                "Blur: $excessiveBlurFrameCount, " +
                "Motion: $insufficientMotionFrameCount," +
                "Total: $totalFrameCount")
    }

    fun reset() {
        totalFrameCount = 0
        excessiveTiltFrameCount = 0
        excessiveBlurFrameCount = 0
        insufficientMotionFrameCount = 0
    }
}