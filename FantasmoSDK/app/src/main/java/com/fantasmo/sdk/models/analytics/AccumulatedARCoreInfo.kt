package com.fantasmo.sdk.models.analytics

import com.google.ar.core.Frame

class AccumulatedARCoreInfo {

    private var trackingStateFrameStatistics = TrackingStateFrameStatistics()

    fun reset(){
        trackingStateFrameStatistics.reset()
    }

    fun update(arFrame: Frame) {
        trackingStateFrameStatistics.update(arFrame)
    }

}