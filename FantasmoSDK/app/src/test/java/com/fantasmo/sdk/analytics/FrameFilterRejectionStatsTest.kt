package com.fantasmo.sdk.analytics

import com.fantasmo.sdk.filters.FMFilterRejectionReason
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameFilterRejectionStatsTest {
    private val frameFilterStats = FrameFilterRejectionStatistics()

    @Test
    fun testAccumulate(){
        var result = FMFilterRejectionReason.IMAGETOOBLURRY
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveBlurFrameCount)

        frameFilterStats.accumulate(result)
        assertEquals(2,frameFilterStats.excessiveBlurFrameCount)

        result = FMFilterRejectionReason.MOVINGTOOLITTLE
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientMotionFrameCount)

        result = FMFilterRejectionReason.PITCHTOOHIGH
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveTiltFrameCount)

        result = FMFilterRejectionReason.PITCHTOOLOW
        frameFilterStats.accumulate(result)
        assertEquals(2,frameFilterStats.excessiveTiltFrameCount)

        result = FMFilterRejectionReason.INSUFFICIENTFEATURES
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientFeatures)

        val field = frameFilterStats.javaClass.getDeclaredField("totalFrameCount")
        field.isAccessible = true
        val fieldValue = field.get(frameFilterStats)
        assertEquals(6,fieldValue)
    }
}