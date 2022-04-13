package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import java.util.*

/**
 * Throttler for frame validation failure events each of which occurs when a frame turns out to be not acceptable for determining location.
 */
class BehaviorRequester(handler: (FMBehaviorRequest) -> Unit) {

    private val defaultBehavior = FMBehaviorRequest.POINT_AT_BUILDINGS
    private var didRequestInitialDefaultBehavior = false

    private val n2s: Double = 1_000_000_000.0

    // Minimum number of seconds that must elapse between triggering.
    private var throttleThreshold = 2.0

    // The number of times a validation error of certain kind occurs before triggering.
    private var incidenceThreshold = 30

    // The last time of triggering.
    private var lastTriggerTime = System.nanoTime()
    private var lastTriggerBehavior: FMBehaviorRequest? = null

    private var requestHandler: ((FMBehaviorRequest) -> Unit) = handler

    // Dictionary of failure events with corresponding incidence
    private var rejectionCounts: MutableMap<FMFrameFilterRejectionReason, Int> = EnumMap(
        FMFrameFilterRejectionReason::class.java
    )

    // TODO - Change to processFilterRejection

    /**
     * On new failure, `onNext` is invoked to update `validationErrorToCountDict`.
     * @param frameFilterResult `FMFrameFilterResult
     */
    fun processFilterRejection(reason: FMFrameFilterRejectionReason) {
        var count = rejectionCounts[reason] ?: 0
        count += 1

        if (count > incidenceThreshold) {
            val elapsed = (System.nanoTime() - lastTriggerTime) / n2s
            if (elapsed > throttleThreshold) {
                val newBehavior = reason.mapToBehaviorRequest()
                val behaviorRequest = if(newBehavior != lastTriggerBehavior){
                    newBehavior
                } else defaultBehavior
                requestHandler(behaviorRequest)
                lastTriggerBehavior = behaviorRequest
                lastTriggerTime = System.nanoTime()
                rejectionCounts.clear()
            }
        } else {
            rejectionCounts[reason] = count
        }

        if (!didRequestInitialDefaultBehavior) {
            didRequestInitialDefaultBehavior = true
            requestHandler(defaultBehavior)
        }
    }

    /**
     * Restart Counting Failure Process when creating a new localization session.
     */
    fun restart() {
        lastTriggerTime = System.nanoTime()
        rejectionCounts.clear()
        didRequestInitialDefaultBehavior = false
    }
}