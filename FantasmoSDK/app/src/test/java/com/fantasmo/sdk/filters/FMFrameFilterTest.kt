package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class FMFrameFilterTest {

    @Test
    fun testMapRequestBehavior() {
        var rejection = FMFrameFilterRejectionReason.PITCH_TOO_LOW

        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.TILT_UP
        )

        rejection = FMFrameFilterRejectionReason.PITCH_TOO_HIGH
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.TILT_DOWN
        )

        rejection = FMFrameFilterRejectionReason.MOVING_TOO_LITTLE
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PAN_AROUND
        )

        rejection = FMFrameFilterRejectionReason.MOVING_TOO_FAST
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PAN_SLOWLY
        )

        rejection = FMFrameFilterRejectionReason.INSUFFICIENT_FEATURES
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PAN_AROUND
        )

        rejection = FMFrameFilterRejectionReason.IMAGE_TOO_BLURRY
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PAN_SLOWLY
        )
    }
}