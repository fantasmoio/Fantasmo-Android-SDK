package com.fantasmo.sdk.utilities

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMUtility.Companion.n2s
import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterFailure
import com.fantasmo.sdk.filters.primeFilters.mapToBehaviourRequest
import java.util.*

/**
 * Throttler for frame validation failure events each of which occurs when a frame turns out to be not acceptable for determining location.
 */
class FrameFailureThrottler {

    // Minimum number of seconds that must elapse between triggering.
    private var throttleThreshold = 2.0

    // The number of times a validation error of certain kind occurs before triggering.
    private var incidenceThreshold = 30

    // The last time of triggering.
    var lastErrorTime = System.nanoTime()

    /**
     * Maps failure to display in app side
     * @param failure: Failure to be mapped to end user
     * @return FMBehaviorRequest
     */
    fun handler(failure: FMFrameFilterFailure): FMBehaviorRequest {
        return mapToBehaviourRequest(failure)
    }

    // Dictionary of failure events with corresponding incidence
    var rejectionCounts: MutableMap<FMFrameFilterFailure, Int> = EnumMap(
        FMFrameFilterFailure::class.java
    )

    /**
     * On new failure, onNext is invoked to update rejectionCounts.
     * @param failure: PITCHTOOLOW, PITCHTOOHIGH, MOVINGTOOFAST, MOVINGTOOLITTLE, ACCEPTED
     */
    fun onNext(failure: FMFrameFilterFailure) {
        if(failure == FMFrameFilterFailure.ACCEPTED){
            return
        }
        else{
            var count = 0
            if (rejectionCounts.containsKey(failure)) {
                count = rejectionCounts[failure]!!
            }
            count += 1

            if (count > incidenceThreshold) {
                val elapsed = (System.nanoTime() - lastErrorTime) / n2s
                if (elapsed > throttleThreshold) {
                    // handler(failure)
                    restart()
                }
            } else {
                rejectionCounts[failure] = count
            }
        }
    }

    /**
     * Restart Counting Failure Process after a sequence.
     */
    fun restart() {
        lastErrorTime = System.nanoTime()
        rejectionCounts.clear()
    }
}