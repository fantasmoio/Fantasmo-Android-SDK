package com.fantasmo.sdk.models.analytics

import com.google.ar.core.Frame

/**
 * Class responsible for accumulating information and statistics about how the ARCore is behaving
 * on the SDK. Also collects information about the tracking state of the frames.
 * To reset accumulated data and start over, you should invoke `reset()`
 */
class AccumulatedARCoreInfo {

    var modelVersion: String = ""
    var scoreThreshold: Double = 0.0
    var lastImageQualityScore: Float = 0f
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
    fun update(arFrame: Frame) {
        elapsedFrames += 1
        trackingStateFrameStatistics.update(arFrame)
        translationAccumulator.update(arFrame)
        rotationAccumulator.update(arFrame)
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
        modelVersion = ""
        scoreThreshold = 0.0
        lastImageQualityScore = 0f
    }
}