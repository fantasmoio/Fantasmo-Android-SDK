package com.fantasmo.sdk.filters

import android.content.Context
import android.util.Log
import com.fantasmo.sdk.FMLocationListener
import com.google.ar.core.Frame

enum class FMFilterRejectionReason {
    //
    PITCHTOOLOW,

    //
    PITCHTOOHIGH,

    //
    MOVINGTOOFAST,

    //
    MOVINGTOOLITTLE,

    ACCEPTED
}

enum class FMFilterResult{
    ACCEPTED,
    REJECTED;
}

interface FMFrameFilter{
    fun accepts(arFrame: Frame): Pair<FMFilterResult, FMFilterRejectionReason>
}

class FMInputQualityFilter(fmLocationListener: FMLocationListener, context: Context) {

    private val TAG = "FMInputQualityFilter"
    private val locationListener = fmLocationListener

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
        FMCameraPitchFilter(),
        FMMovementFilter(),
        //FMBlurFilter()
    )

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
                val result = filter.accepts(arFrame)
                Log.d(TAG, result.toString())
                if(result.first != FMFilterResult.ACCEPTED){
                    val rejection = filter.accepts(arFrame).second
                    addRejection(rejection)
                    notifyIfNeeded(rejection)
                    Log.d(TAG, "accepts $result -> False")
                    return false
                }
            }
        }
        resetAcceptanceClock()
        Log.d(TAG, "accepts -> True")
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
            //val behaviorRequest = mapToBehaviourRequest(rejection)
            //locationListener.locationManager(behaviorRequest)
            rejections.clear()
            lastRequestTime = System.currentTimeMillis()
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

    private fun mapToBehaviourRequest(rejection:FMFilterRejectionReason):FMBehaviorRequest{
        return when (rejection){
            FMFilterRejectionReason.PITCHTOOLOW -> FMBehaviorRequest.TILTUP
            FMFilterRejectionReason.PITCHTOOHIGH -> FMBehaviorRequest.TILTDOWN
            FMFilterRejectionReason.MOVINGTOOFAST -> FMBehaviorRequest.PANSLOWLY
            FMFilterRejectionReason.MOVINGTOOLITTLE -> FMBehaviorRequest.PANAROUND
            else -> FMBehaviorRequest.ACCEPTED
        }
    }

}