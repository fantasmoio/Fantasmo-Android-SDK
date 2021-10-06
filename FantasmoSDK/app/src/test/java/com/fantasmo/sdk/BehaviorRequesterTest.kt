package com.fantasmo.sdk

import com.fantasmo.sdk.filters.FMFilterRejectionReason
import com.fantasmo.sdk.filters.BehaviorRequester
import com.fantasmo.sdk.filters.FMFrameFilterResult
import com.fantasmo.sdk.models.ErrorResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class BehaviorRequesterTest {

    private val behaviorRequester = BehaviorRequester {
        fmLocationListener.locationManager(didRequestBehavior = it)
    }


    @Test
    fun testProcessResult() {
        val reason = FMFilterRejectionReason.PITCHTOOLOW
        val failure = FMFrameFilterResult.Rejected(reason)
        behaviorRequester.processResult(failure)

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
        val reason = FMFilterRejectionReason.PITCHTOOLOW
        val failure = FMFrameFilterResult.Rejected(reason)

        val fieldRejectionCounts = behaviorRequester.javaClass.getDeclaredField("rejectionCounts")
        fieldRejectionCounts.isAccessible = true
        val result : MutableMap<FMFilterRejectionReason, Int> = fieldRejectionCounts.get(behaviorRequester) as MutableMap<FMFilterRejectionReason, Int>

        result[reason] = 2

        behaviorRequester.processResult(failure)

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
        val reason = FMFilterRejectionReason.PITCHTOOLOW
        val failure = FMFrameFilterResult.Rejected(reason)

        val fieldRejectionCounts = behaviorRequester.javaClass.getDeclaredField("rejectionCounts")
        fieldRejectionCounts.isAccessible = true
        val result : MutableMap<FMFilterRejectionReason, Int> = fieldRejectionCounts.get(behaviorRequester) as MutableMap<FMFilterRejectionReason, Int>

        result[reason] = 30

        val fieldLastTriggerTime = behaviorRequester.javaClass.getDeclaredField("lastTriggerTime")
        fieldLastTriggerTime.isAccessible = true
        fieldLastTriggerTime.set(behaviorRequester,1619184130499)

        behaviorRequester.processResult(failure)

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
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
            }

            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
            }

            override fun locationManager(result: FMLocationResult) {
            }
        }
}