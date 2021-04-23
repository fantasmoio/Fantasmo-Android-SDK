package com.fantasmo.sdk.utilities

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.frameSequenceFilter.FMFrameFilterFailure
import com.fantasmo.sdk.frameSequenceFilter.mapToBehaviourRequest
import java.util.*

class FrameFailureThrottler {

    // Minimum number of seconds that must elapse between triggering.
    private var throttleThreshold = 2.0

    // The number of times a validation error of certain kind occurs before triggering.
    var incidenceThreshold = 30

    // The last time of triggering.
    var lastErrorTime = System.currentTimeMillis()

    fun handler(failure: FMFrameFilterFailure): FMBehaviorRequest {
        return mapToBehaviourRequest(failure)
    }

    var validationErrorToCountDict : MutableMap<FMFrameFilterFailure,Int> = EnumMap(
        FMFrameFilterFailure::class.java)

    fun onNext(failure: FMFrameFilterFailure) {
        var count = 0
        if(validationErrorToCountDict.containsKey(failure)){
            count = validationErrorToCountDict[failure]!!
        }

        val elapsed = System.currentTimeMillis() - lastErrorTime

        if(elapsed > throttleThreshold && count >= incidenceThreshold){
            startNewCycle()
        } else{
            validationErrorToCountDict[failure] = count + 1
        }
    }

    fun restart() {
        startNewCycle()
    }

    private fun startNewCycle() {
        lastErrorTime = System.currentTimeMillis()
        validationErrorToCountDict.clear()
    }
}