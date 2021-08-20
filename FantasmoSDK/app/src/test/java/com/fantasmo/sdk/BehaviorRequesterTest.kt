package com.fantasmo.sdk

import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterFailure
import com.fantasmo.sdk.filters.BehaviorRequester
import org.junit.Assert.assertEquals
import org.junit.Test

class BehaviorRequesterTest {

    @Test
    fun testHandler() {
        var failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = BehaviorRequester()

        assertEquals(
            frameFailure.handler(failure),
            FMBehaviorRequest.TILTUP
        )

        failure = FMFrameFilterFailure.PITCHTOOHIGH
        assertEquals(
            frameFailure.handler(failure),
            FMBehaviorRequest.TILTDOWN
        )

        failure = FMFrameFilterFailure.MOVINGTOOLITTLE
        assertEquals(
            frameFailure.handler(failure),
            FMBehaviorRequest.PANAROUND
        )

        failure = FMFrameFilterFailure.MOVINGTOOFAST
        assertEquals(
            frameFailure.handler(failure),
            FMBehaviorRequest.PANSLOWLY
        )

        failure = FMFrameFilterFailure.ACCEPTED
        assertEquals(
            frameFailure.handler(failure),
            FMBehaviorRequest.ACCEPTED
        )
    }

    @Test
    fun testOnNext() {
        val failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = BehaviorRequester()

        frameFailure.processResult(failure)

        assertEquals(
            frameFailure.rejectionCounts.isEmpty(),
            false
        )

        assertEquals(
            frameFailure.rejectionCounts[failure],
            1
        )
    }

    @Test
    fun testOnNextWithFailure() {
        val failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = BehaviorRequester()

        frameFailure.rejectionCounts[failure] = 2

        frameFailure.processResult(failure)

        assertEquals(
            frameFailure.rejectionCounts.isEmpty(),
            false
        )

        assertEquals(
            frameFailure.rejectionCounts[failure],
            3
        )
    }

    @Test
    fun testOnNextStartNewCycle() {
        val failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = BehaviorRequester()

        frameFailure.rejectionCounts[failure] = 30
        frameFailure.lastErrorTime = 1619184130499

        frameFailure.processResult(failure)

        assertEquals(
            frameFailure.rejectionCounts.isEmpty(),
            true
        )
    }
}