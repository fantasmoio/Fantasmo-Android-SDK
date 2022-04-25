package com.fantasmo.sdk.analytics

import com.fantasmo.sdk.filters.FMFrameFilterRejectionReason
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameFilterRejectionStatsTest {
    private val frameFilterStats = FrameFilterRejectionStatistics()

    @Test
    fun testAccumulate(){
        var result = FMFrameFilterRejectionReason.IMAGE_TOO_BLURRY
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveBlurFrameCount)

        frameFilterStats.accumulate(result)
        assertEquals(2,frameFilterStats.excessiveBlurFrameCount)

        result = FMFrameFilterRejectionReason.MOVING_TOO_LITTLE
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientMotionFrameCount)

        result = FMFrameFilterRejectionReason.MOVING_TOO_FAST
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveMotionFrameCount)

        result = FMFrameFilterRejectionReason.PITCH_TOO_HIGH
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientTiltFrameCount)

        result = FMFrameFilterRejectionReason.PITCH_TOO_LOW
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.excessiveTiltFrameCount)

        result = FMFrameFilterRejectionReason.INSUFFICIENT_FEATURES
        frameFilterStats.accumulate(result)
        assertEquals(1,frameFilterStats.insufficientFeatures)

        val field = frameFilterStats.javaClass.getDeclaredField("totalFrameCount")
        field.isAccessible = true
        val fieldValue = field.get(frameFilterStats)
        assertEquals(7,fieldValue)
    }
}