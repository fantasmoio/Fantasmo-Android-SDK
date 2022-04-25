package com.fantasmo.sdk.models.analytics

import com.fantasmo.sdk.models.FMFrame

/**
 * Class responsible for accumulating information and statistics about how the ARCore is behaving
 * on the SDK. Also collects information about the tracking state of the frames.
 * To reset accumulated data and start over, you should invoke `reset()`
 */
class AccumulatedARCoreInfo {

    var trackingStateFrameStatistics = TrackingStateFrameStatistics()
    var elapsedFrames = 0

    // Allows to receive the total translation (distance) that device has moved from the starting moment.
    var translationAccumulator = TotalDeviceTranslationAccumulator(10)

    // Spread of angles as min and max values for each component (that is for yaw, pitch and roll)
    var rotationAccumulator = TotalDeviceRotationAccumulator()

    /**
     * When invoked, this method gives a copy of the frame (collected during the localize request
     * call) to each class involved in the statistics collection
     * @param arFrame frame collected during localize request
     */
    fun update(fmFrame: FMFrame) {
        elapsedFrames += 1
        trackingStateFrameStatistics.update(fmFrame)
        translationAccumulator.update(fmFrame)
        rotationAccumulator.update(fmFrame)
    }

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
}