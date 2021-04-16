package com.fantasmo.sdk.utilities

import com.fantasmo.sdk.filters.FMFrameValidationError
import com.fantasmo.sdk.filters.mapToBehaviourRequest
import java.util.*

class FrameFailureThrottler {

    // The last time of triggering.
    var lastErrorTime = System.currentTimeMillis()
    // Minimum number of seconds that must elapse between triggering.
    var throttleThreshold = 2.0
    // The number of times a validation error of certain kind occurs before triggering.
    var incidenceThreshold = 30

    private var rejections : MutableMap<FMFrameValidationError,Int> = EnumMap(
        FMFrameValidationError::class.java)

    private fun onNext(rejection: FMFrameValidationError) {
        val count = addRejection(rejection)
        val elapsed = System.currentTimeMillis() - lastErrorTime

        if(elapsed > throttleThreshold && count > incidenceThreshold){
            val behaviorRequest = mapToBehaviourRequest(rejection)
            //locationListener.locationManager(behaviorRequest)
            startNewCycle()
        } else{
            rejections[rejection] = count
        }
    }

    private fun addRejection(rejection: FMFrameValidationError):Int {
        if(rejections.containsKey(rejection)){
            var count = rejections[rejection]
            if(count!=null){
                count+=1
                rejections[rejection] = count
                return count
            }
        }else{
            rejections[rejection] = 0
            return 0
        }
        return 0
    }

    fun restart() {
        startNewCycle()
    }

    private fun startNewCycle() {
        lastErrorTime = System.currentTimeMillis()
        rejections.clear()
    }
}