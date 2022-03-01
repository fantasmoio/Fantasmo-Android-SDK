package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class FMFrameFilterTest {

    @Test
    fun testMapRequestBehavior() {
        var rejection = FMFrameFilterRejectionReason.PitchTooLow

        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.TiltUp
        )

        rejection = FMFrameFilterRejectionReason.PitchTooHigh
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.TiltDown
        )

        rejection = FMFrameFilterRejectionReason.MovingTooLittle
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PanAround
        )

        rejection = FMFrameFilterRejectionReason.MovingTooFast
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PanSlowly
        )

        rejection = FMFrameFilterRejectionReason.InsufficientFeatures
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PanAround
        )

        rejection = FMFrameFilterRejectionReason.ImageTooBlurry
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PanSlowly
        )
    }
}