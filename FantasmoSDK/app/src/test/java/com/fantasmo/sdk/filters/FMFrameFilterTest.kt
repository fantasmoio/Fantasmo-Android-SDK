package com.fantasmo.sdk.filters

import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterFailure
import com.fantasmo.sdk.filters.primeFilters.mapToBehaviourRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class FMFrameFilterTest {

    @Test
    fun testMapRequestBehavior() {
        var rejection = FMFrameFilterFailure.PITCHTOOLOW

        assertEquals(
            mapToBehaviourRequest(rejection),
            FMBehaviorRequest.TILTUP
        )

        rejection = FMFrameFilterFailure.PITCHTOOHIGH
        assertEquals(
            mapToBehaviourRequest(rejection),
            FMBehaviorRequest.TILTDOWN
        )

        rejection = FMFrameFilterFailure.MOVINGTOOLITTLE
        assertEquals(
            mapToBehaviourRequest(rejection),
            FMBehaviorRequest.PANAROUND
        )

        rejection = FMFrameFilterFailure.MOVINGTOOFAST
        assertEquals(
            mapToBehaviourRequest(rejection),
            FMBehaviorRequest.PANSLOWLY
        )

        rejection = FMFrameFilterFailure.ACCEPTED
        assertEquals(
            mapToBehaviourRequest(rejection),
            FMBehaviorRequest.ACCEPTED
        )

    }
}