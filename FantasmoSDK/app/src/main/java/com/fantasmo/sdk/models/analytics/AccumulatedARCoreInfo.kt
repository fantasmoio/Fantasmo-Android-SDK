package com.fantasmo.sdk.models.analytics

import com.google.ar.core.Frame

/**
 * Class responsible for accumulating information and statistics about how the ARCore is behaving
 * on the SDK. Also collects information about the filtered frames.
 */
class AccumulatedARCoreInfo {

    var trackingStateFrameStatistics = TrackingStateFrameStatistics()
    var translationAccumulator = TotalDeviceTranslationAccumulator(10)
    var rotationAccumulator = TotalDeviceRotationAccumulator()
    var elapsedFrames = 0

    /**
     * When invoked, this calls all the reset method on the class required to collect all the info
     * This should be called on each time a startUpdatingLocation is called
     */
    fun reset(){
        elapsedFrames = 0
        trackingStateFrameStatistics.reset()
        translationAccumulator.reset()
        rotationAccumulator.reset()
    }

    /**
     * When invoked, this method gives a copy of the frame (collected during the localize request
     * call) to each class involved in the statistics collection
     * @param arFrame: frame collected during localize request
     */
    fun update(arFrame: Frame) {
        elapsedFrames += 1
        trackingStateFrameStatistics.update(arFrame)
        translationAccumulator.update(arFrame)
        rotationAccumulator.update(arFrame)
    }

}