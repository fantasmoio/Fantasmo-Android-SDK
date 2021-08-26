package com.fantasmo.sdk.analytics

import com.fantasmo.sdk.filters.FMFrameFilterFailure
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameFilterRejectionStatsTest {
    private val frameFilterStats = FrameFilterRejectionStatistics()

    @Test
    fun testAccumulate(){
        var result = FMFrameFilterFailure.IMAGETOOBLURRY
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveBlurFrameCount)

        frameFilterStats.accumulate(result)
        assertEquals(2,frameFilterStats.excessiveBlurFrameCount)

        result = FMFrameFilterFailure.MOVINGTOOLITTLE
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientMotionFrameCount)

        result = FMFrameFilterFailure.PITCHTOOHIGH
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveTiltFrameCount)

        result = FMFrameFilterFailure.PITCHTOOLOW
        frameFilterStats.accumulate(result)
        assertEquals(2,frameFilterStats.excessiveTiltFrameCount)

        result = FMFrameFilterFailure.INSUFFICIENTFEATURES
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientFeatures)

        assertEquals(6,frameFilterStats.totalFrameCount)
    }
}