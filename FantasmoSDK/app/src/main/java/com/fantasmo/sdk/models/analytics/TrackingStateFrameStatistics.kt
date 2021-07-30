package com.fantasmo.sdk.models.analytics

import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason

class TrackingStateFrameStatistics {

    private var totalNumberOfFrames: Int = 0

    // Number of events where the frames captured have a normal tracking state
    private var normalEventCount: Int = 0

    // Number of events where the excessive motion callback is received from ARKit/ARCore.
    private var excessiveMotionEventCount: Int = 0

    // Number of events where the loss of tracking callback is received from ARKit/ARCore
    private var lossOfTrackingEventCount: Int = 0

    fun reset() {
        totalNumberOfFrames = 0
        normalEventCount = 0
        excessiveMotionEventCount = 0
        lossOfTrackingEventCount = 0
    }

    fun update(arFrame: Frame) {
        totalNumberOfFrames += 1

        when (arFrame.camera.trackingFailureReason) {
            //Excessive Motion Event
            TrackingFailureReason.EXCESSIVE_MOTION -> {
                excessiveMotionEventCount += 1
            }
            //Loss of tracking due to bad internal state
            TrackingFailureReason.BAD_STATE -> {
                lossOfTrackingEventCount += 1
            }
            //Loss of tracking due to Insufficient Features
            TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                lossOfTrackingEventCount += 1
            }
            //Loss of tracking due to Insufficient Light
            TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                lossOfTrackingEventCount += 1
            }
            //Loss of tracking due to Camera Unavailable
            TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                lossOfTrackingEventCount += 1
            }
            else -> {
                //Normal ARCore behavior
                normalEventCount += 1
            }
        }
    }
}