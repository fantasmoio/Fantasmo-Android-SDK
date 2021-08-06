package com.fantasmo.sdk.models.analytics

import com.google.ar.core.Frame

/**
 * Class responsible for accumulating information and statistics about how the ARCore is behaving
 * on the SDK. Also collects information about the filtered frames.
 */
class AccumulatedARCoreInfo {

    var trackingStateFrameStatistics = TrackingStateFrameStatistics()

    /**
     * When invoked, this calls all the reset method on the class required to collect all the info
     * This should be called on each time a startUpdatingLocation is called
     */
    fun reset(){
        trackingStateFrameStatistics.reset()
    }

    /**
     * When invoked, this method gives a copy of the frame (collected during the localize request
     * call) to each class involved in the statistics collection
     * @param arFrame: frame collected during localize request
     */
    fun update(arFrame: Frame) {
        trackingStateFrameStatistics.update(arFrame)
    }

}