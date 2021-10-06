package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class FMFrameFilterTest {

    @Test
    fun testMapRequestBehavior() {
        var rejection = FMFilterRejectionReason.PITCHTOOLOW

        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.TILTUP
        )

        rejection = FMFilterRejectionReason.PITCHTOOHIGH
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.TILTDOWN
        )

        rejection = FMFilterRejectionReason.MOVINGTOOLITTLE
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.PANAROUND
        )

        rejection = FMFilterRejectionReason.MOVINGTOOFAST
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.PANSLOWLY
        )

        rejection = FMFilterRejectionReason.INSUFFICIENTFEATURES
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.PANAROUND
        )

        rejection = FMFilterRejectionReason.IMAGETOOBLURRY
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.PANSLOWLY
        )
    }
}