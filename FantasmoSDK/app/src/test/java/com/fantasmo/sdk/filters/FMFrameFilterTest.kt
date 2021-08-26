package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class FMFrameFilterTest {

    @Test
    fun testMapRequestBehavior() {
        var rejection = FMFrameFilterFailure.PITCHTOOLOW

        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.TILTUP
        )

        rejection = FMFrameFilterFailure.PITCHTOOHIGH
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.TILTDOWN
        )

        rejection = FMFrameFilterFailure.MOVINGTOOLITTLE
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.PANAROUND
        )

        rejection = FMFrameFilterFailure.MOVINGTOOFAST
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.PANSLOWLY
        )

        rejection = FMFrameFilterFailure.ACCEPTED
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.ACCEPTED
        )

        rejection = FMFrameFilterFailure.INSUFFICIENTFEATURES
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.PANAROUND
        )

        rejection = FMFrameFilterFailure.IMAGETOOBLURRY
        assertEquals(
            rejection.mapToBehaviourRequest(),
            FMBehaviorRequest.PANSLOWLY
        )
    }
}