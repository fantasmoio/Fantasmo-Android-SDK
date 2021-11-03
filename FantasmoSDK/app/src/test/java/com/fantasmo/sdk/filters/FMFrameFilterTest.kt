package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class FMFrameFilterTest {

    @Test
    fun testMapRequestBehavior() {
        var rejection = FMFilterRejectionReason.PITCHTOOLOW

        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.TILTUP
        )

        rejection = FMFilterRejectionReason.PITCHTOOHIGH
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.TILTDOWN
        )

        rejection = FMFilterRejectionReason.MOVINGTOOLITTLE
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PANAROUND
        )

        rejection = FMFilterRejectionReason.MOVINGTOOFAST
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PANSLOWLY
        )

        rejection = FMFilterRejectionReason.INSUFFICIENTFEATURES
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PANAROUND
        )

        rejection = FMFilterRejectionReason.IMAGETOOBLURRY
        assertEquals(
            rejection.mapToBehaviorRequest(),
            FMBehaviorRequest.PANSLOWLY
        )
    }
}