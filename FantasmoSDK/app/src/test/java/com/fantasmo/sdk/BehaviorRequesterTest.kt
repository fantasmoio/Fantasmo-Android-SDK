package com.fantasmo.sdk

import com.fantasmo.sdk.filters.FMFrameFilterFailure
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

        failure = FMFrameFilterFailure.INSUFFICIENTFEATURES
        assertEquals(
            frameFailure.handler(failure),
            FMBehaviorRequest.PANAROUND
        )
    }

    @Test
    fun testProcessResult() {
        val failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = BehaviorRequester()

        frameFailure.processResult(failure)

        val fieldRejectionCounts = frameFailure.javaClass.getDeclaredField("rejectionCounts")
        fieldRejectionCounts.isAccessible = true
        val result = fieldRejectionCounts.get(frameFailure) as MutableMap<*, *>

        assertEquals(
            result.isEmpty(),
            false
        )

        assertEquals(
            result[failure],
            1
        )
    }

    @Test
    fun testProcessResultWithFailure() {
        val failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = BehaviorRequester()

        val fieldRejectionCounts = frameFailure.javaClass.getDeclaredField("rejectionCounts")
        fieldRejectionCounts.isAccessible = true
        val result : MutableMap<FMFrameFilterFailure, Int> = fieldRejectionCounts.get(frameFailure) as MutableMap<FMFrameFilterFailure, Int>

        result[failure] = 2

        frameFailure.processResult(failure)

        assertEquals(
            result.isEmpty(),
            false
        )

        assertEquals(
            result[failure],
            3
        )
    }

    @Test
    fun testProcessResultStartNewCycle() {
        val failure = FMFrameFilterFailure.PITCHTOOLOW
        val frameFailure = BehaviorRequester()

        val fieldRejectionCounts = frameFailure.javaClass.getDeclaredField("rejectionCounts")
        fieldRejectionCounts.isAccessible = true
        val result : MutableMap<FMFrameFilterFailure, Int> = fieldRejectionCounts.get(frameFailure) as MutableMap<FMFrameFilterFailure, Int>

        result[failure] = 30

        val fieldLastTriggerTime = frameFailure.javaClass.getDeclaredField("lastTriggerTime")
        fieldLastTriggerTime.isAccessible = true
        fieldLastTriggerTime.set(frameFailure,1619184130499)

        frameFailure.processResult(failure)

        assertEquals(
            result.isEmpty(),
            true
        )
    }
}