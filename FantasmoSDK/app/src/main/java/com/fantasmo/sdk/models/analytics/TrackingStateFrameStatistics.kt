package com.fantasmo.sdk.models.analytics

import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason

/**
 * Class responsible for creating statistics about frame events on ARCore position tracking
 */
class TrackingStateFrameStatistics {

    private val TAG = "TrackingStateFrameStats"
    var totalNumberOfFrames: Int = 0

    // Number of events where the frames captured have a normal tracking state
    var normalEventCount: Int = 0

    // Number of events where the excessive motion callback is received from ARKit/ARCore.
    var excessiveMotionEventCount: Int = 0

    // Number of events where the loss of tracking callback is received from ARKit/ARCore
    var lossOfTrackingEventCount: Int = 0

    /**
     * Resets counters on new startUpdatingLocation call
     */
    fun reset() {
        totalNumberOfFrames = 0
        normalEventCount = 0
        excessiveMotionEventCount = 0
        lossOfTrackingEventCount = 0
    }

    /**
     * Receives a frame and updates the counter
     * that best describes the tracking state event
     * @param arFrame: frame to be evaluated
     */
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
        Log.d(
            TAG, "ExcessiveMotion: $excessiveMotionEventCount\n" +
                    "LossOfTracking: $lossOfTrackingEventCount\n" +
                    "NormalEvent: $normalEventCount\n" +
                    "TotalFrames: $totalNumberOfFrames"
        )
    }
}