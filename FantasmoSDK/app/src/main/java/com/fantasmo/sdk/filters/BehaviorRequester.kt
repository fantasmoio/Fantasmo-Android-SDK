package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import java.util.*

/**
 * Throttler for frame validation failure events each of which occurs when a frame turns out to be not acceptable for determining location.
 */
class BehaviorRequester(handler: (FMBehaviorRequest) -> Unit) {

    private val defaultBehavior = FMBehaviorRequest.POINTATBUILDINGS
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
    private var rejectionCounts: MutableMap<FMFilterRejectionReason, Int> = EnumMap(
        FMFilterRejectionReason::class.java
    )

    /**
     * On new failure, `onNext` is invoked to update `validationErrorToCountDict`.
     * @param frameFilterResult `FMFrameFilterResult
     */
    fun processResult(frameFilterResult: FMFrameFilterResult) {
        when (frameFilterResult) {
            FMFrameFilterResult.Accepted -> return
            else -> {
                val rejectionReason = frameFilterResult.getRejectedReason()!!
                var count = 0
                if (rejectionCounts.containsKey(rejectionReason)) {
                    count = rejectionCounts[rejectionReason]!!
                }
                count += 1

                if (count > incidenceThreshold) {
                    val elapsed = (System.nanoTime() - lastTriggerTime) / n2s
                    if (elapsed > throttleThreshold) {
                        val newBehavior = rejectionReason.mapToBehaviorRequest()
                        val behaviorRequest = if(newBehavior != lastTriggerBehavior){
                            newBehavior
                        } else defaultBehavior
                        requestHandler(behaviorRequest)
                        lastTriggerBehavior = behaviorRequest
                        lastTriggerTime = System.nanoTime()
                        rejectionCounts.clear()
                    }
                } else {
                    rejectionCounts[rejectionReason] = count
                }

                if (!didRequestInitialDefaultBehavior) {
                    didRequestInitialDefaultBehavior = true
                    requestHandler(defaultBehavior)
                }
            }
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