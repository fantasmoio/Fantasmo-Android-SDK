package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.frameSequenceFilter.FMFrameFilterFailure
import com.fantasmo.sdk.frameSequenceFilter.mapToBehaviourRequest
import junit.framework.Assert.assertEquals
import org.junit.Test

class FMFrameSequenceFilterRuleTest {

    @Test
    fun testMapRequestBehavior(){
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