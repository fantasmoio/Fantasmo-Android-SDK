package com.fantasmo.sdk

import com.fantasmo.sdk.filters.FMFrameFilterRejectionReason
import com.fantasmo.sdk.filters.BehaviorRequester
import com.fantasmo.sdk.filters.FMFrameFilterResult
import com.fantasmo.sdk.models.ErrorResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class BehaviorRequesterTest {

    private val behaviorRequester = BehaviorRequester {
        fmLocationListener.didRequestBehavior(behavior = it)
    }


    @Test
    fun testProcessResult() {
        val reason = FMFrameFilterRejectionReason.PITCH_TOO_LOW
        behaviorRequester.processFilterRejection(reason)

        val fieldRejectionCounts = behaviorRequester.javaClass.getDeclaredField("rejectionCounts")
        fieldRejectionCounts.isAccessible = true
        val result = fieldRejectionCounts.get(behaviorRequester) as MutableMap<*, *>

        assertEquals(
            false,
            result.isEmpty()
        )

        assertEquals(
            1,
            result[reason]
        )
    }

    @Test
    fun testProcessResultWithFailure() {
        val reason = FMFrameFilterRejectionReason.PITCH_TOO_LOW
        val failure = FMFrameFilterResult.Rejected(reason)

        val fieldRejectionCounts = behaviorRequester.javaClass.getDeclaredField("rejectionCounts")
        fieldRejectionCounts.isAccessible = true
        val result : MutableMap<FMFrameFilterRejectionReason, Int> = fieldRejectionCounts.get(behaviorRequester) as MutableMap<FMFrameFilterRejectionReason, Int>

        result[reason] = 2

        behaviorRequester.processFilterRejection(reason)

        assertEquals(
            false,
            result.isEmpty()
        )

        assertEquals(
            3,
            result[reason]
        )
    }

    @Test
    fun testProcessResultStartNewCycle() {
        val reason = FMFrameFilterRejectionReason.PITCH_TOO_LOW

        val fieldRejectionCounts = behaviorRequester.javaClass.getDeclaredField("rejectionCounts")
        fieldRejectionCounts.isAccessible = true
        val result : MutableMap<FMFrameFilterRejectionReason, Int> = fieldRejectionCounts.get(behaviorRequester) as MutableMap<FMFrameFilterRejectionReason, Int>

        result[reason] = 30

        val fieldLastTriggerTime = behaviorRequester.javaClass.getDeclaredField("lastTriggerTime")
        fieldLastTriggerTime.isAccessible = true
        fieldLastTriggerTime.set(behaviorRequester,1619184130499)

        behaviorRequester.processFilterRejection(reason)

        assertEquals(
            true,
            result.isEmpty()
        )
    }


    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun didFailWithError(error: ErrorResponse, metadata: Any?) {
            }

            override fun didRequestBehavior(behavior: FMBehaviorRequest) {
            }

            override fun didUpdateLocation(result: FMLocationResult) {
            }
        }
}