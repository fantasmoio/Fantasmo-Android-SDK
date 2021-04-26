package com.fantasmo.sdk.utilities

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.frameSequenceFilter.FMFrameFilterFailure
import com.fantasmo.sdk.frameSequenceFilter.mapToBehaviourRequest
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
    var lastErrorTime = System.currentTimeMillis()

    /**
     * Maps failure to display in app side
     * @param failure: Failure to be mapped to end user
     * @return FMBehaviorRequest
     */
    fun handler(failure: FMFrameFilterFailure): FMBehaviorRequest {
        return mapToBehaviourRequest(failure)
    }

    // Dictionary of failure events with corresponding incidence
    var validationErrorToCountDict: MutableMap<FMFrameFilterFailure, Int> = EnumMap(
        FMFrameFilterFailure::class.java
    )

    /**
     * On new failure, onNext is invoked to update validationErrorToCountDict.
     * @param failure: PITCHTOOLOW, PITCHTOOHIGH, MOVINGTOOFAST, MOVINGTOOLITTLE, ACCEPTED
     */
    fun onNext(failure: FMFrameFilterFailure) {
        var count = 0
        if (validationErrorToCountDict.containsKey(failure)) {
            count = validationErrorToCountDict[failure]!!
        }

        val elapsed = System.currentTimeMillis() - lastErrorTime

        if (elapsed > throttleThreshold && count >= incidenceThreshold) {
            startNewCycle()
        } else {
            validationErrorToCountDict[failure] = count + 1
        }
    }

    /**
     * Restart Counting Failure Process after a sequence.
     */
    fun restart() {
        startNewCycle()
    }

    /**
     * Creates a new for frame sequence acceptance.
     */
    private fun startNewCycle() {
        lastErrorTime = System.currentTimeMillis()
        validationErrorToCountDict.clear()
    }
}