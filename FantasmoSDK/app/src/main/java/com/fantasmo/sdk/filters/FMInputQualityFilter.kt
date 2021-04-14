package com.fantasmo.sdk.filters

import android.content.Context
import com.fantasmo.sdk.FMLocationManager
import com.google.ar.core.Frame

class FMInputQualityFilter(locationManager: FMLocationManager, context: Context) {

    // the last time a frame was accepted
    var lastAcceptTime = System.currentTimeMillis()
    // number of seconds after which we force acceptance
    var acceptanceThreshold = 6.0

    // the last time we issued a behavior request
    var lastRequestTime = System.currentTimeMillis()
    // minimum number of seconds that must elapse between behavior requests
    var throttleThreshold = 2.0
    // the number of times a rejection occurs that should prompt a behavior request
    var incidenceThreshold = 30

    private var filters = listOf(
        FMAngleFilter(),
        FMMovementFilter(),
        FMBlurFilter()
    ) as List<FMFrameFilter>

    private lateinit var rejections : MutableMap<FMFilterRejectionReason,Int>

    fun startFiltering(){
        resetAcceptanceClock()
    }

    private fun resetAcceptanceClock() {
        lastAcceptTime = System.currentTimeMillis()
    }

    fun accepts(arFrame : Frame) : Boolean{
        if(!shouldForceAccept()){
            for(filter in filters){

            }
        }
        resetAcceptanceClock()
        return true
    }


    private fun shouldForceAccept(): Boolean {
        val elapsed = System.currentTimeMillis() - lastAcceptTime
        return elapsed > acceptanceThreshold
    }

    private fun notifyIfNeeded(rejection: FMFilterRejectionReason) {
        val count = rejections.size

        val elapsed = System.currentTimeMillis() - lastAcceptTime
        if(elapsed > throttleThreshold && count > incidenceThreshold){
            //locationManager.
        }
    }

    private fun addRejection(rejection: FMFilterRejectionReason) {
        if(rejections.containsKey(rejection)){
            var count = rejections[rejection]
            if(count!=null){
                count+=1
                rejections[rejection] = count
            }
        }else{
            rejections[rejection] = 0
        }
    }
}

interface FMFrameFilter{
    fun accepts(arFrame: Frame): FMFilterResult
}

enum class FMFilterRejectionReason {
    //
    PITCHTOOLOW,

    //
    PITCHTOOHIGH,

    //
    MOVINGTOOFAST,

    //
    MOVINGTOOLITTLE
}

enum class FMFilterResult{
    ACCEPTED,
    REJECTED
}