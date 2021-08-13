package com.fantasmo.sdk

import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterFailure
import com.fantasmo.sdk.utilities.FrameFailureThrottler
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameFailureThrottlerTest {

    @Test
    fun testHandler() {
        var failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = FrameFailureThrottler()

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
        val frameFailure = FrameFailureThrottler()

        frameFailure.onNext(failure)

        assertEquals(
            frameFailure.validationErrorToCountDict.isEmpty(),
            false
        )

        assertEquals(
            frameFailure.validationErrorToCountDict[failure],
            1
        )
    }

    @Test
    fun testOnNextWithFailure() {
        val failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = FrameFailureThrottler()

        frameFailure.validationErrorToCountDict[failure] = 2

        frameFailure.onNext(failure)

        assertEquals(
            frameFailure.validationErrorToCountDict.isEmpty(),
            false
        )

        assertEquals(
            frameFailure.validationErrorToCountDict[failure],
            3
        )
    }

    @Test
    fun testOnNextStartNewCycle() {
        val failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = FrameFailureThrottler()

        frameFailure.validationErrorToCountDict[failure] = 30
        frameFailure.lastErrorTime = 1619184130499

        frameFailure.onNext(failure)

        assertEquals(
            frameFailure.validationErrorToCountDict.isEmpty(),
            true
        )
    }
}