package com.fantasmo.sdk.analytics

import com.fantasmo.sdk.filters.FMFrameFilterRejectionReason
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameFilterRejectionStatsTest {
    private val frameFilterStats = FrameFilterRejectionStatistics()

    @Test
    fun testAccumulate(){
        var result = FMFrameFilterRejectionReason.ImageTooBlurry
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveBlurFrameCount)

        frameFilterStats.accumulate(result)
        assertEquals(2,frameFilterStats.excessiveBlurFrameCount)

        result = FMFrameFilterRejectionReason.MovingTooLittle
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientMotionFrameCount)

        result = FMFrameFilterRejectionReason.MovingTooFast
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveMotionFrameCount)

        result = FMFrameFilterRejectionReason.PitchTooHigh
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientTiltFrameCount)

        result = FMFrameFilterRejectionReason.PitchTooLow
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveTiltFrameCount)

        result = FMFrameFilterRejectionReason.InsufficientFeatures
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientFeatures)

        val field = frameFilterStats.javaClass.getDeclaredField("totalFrameCount")
        field.isAccessible = true
        val fieldValue = field.get(frameFilterStats)
        assertEquals(7,fieldValue)
    }
}