package com.fantasmo.sdk.models.analytics

import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import java.util.*

/**
 * Class responsible for creating statistics about frame events on ARCore position tracking
 */
class TrackingStateFrameStatistics {

    var totalNumberOfFrames: Int = 0

    // Number of events where the frames captured have a normal tracking state
    var framesWithNormalTrackingState: Int = 0

    // Number of events where the excessive motion callback is received from ARKit/ARCore.
    var excessiveMotionEventCount: Int = 0

    // Number of events where the loss of tracking callback is received from ARKit/ARCore
    var framesWithLimitedTrackingState: Int = 0

    // Number of frames captured at the moment when tracking state was `ARFrame.camera.trackingState == CAMERA_UNAVAILABLE`
    var framesWithNotAvailableTracking: Int = 0
    private var framesWithLimitedTrackingStateByReason : MutableMap<TrackingFailureReason,Int> = EnumMap(
        TrackingFailureReason::class.java
    )

    /**
     * Resets counters on new startUpdatingLocation call
     */
    fun reset() {
        totalNumberOfFrames = 0
        framesWithNormalTrackingState = 0
        excessiveMotionEventCount = 0
        framesWithLimitedTrackingState = 0
        framesWithNotAvailableTracking = 0
        framesWithLimitedTrackingStateByReason.clear()
    }

        /**
     * Receives a frame and updates the counter
     * that best describes the tracking state event
     * @param arFrame: frame to be evaluated
     */

    /**
     * Receives a frame and updates the counter
     * that best describes the tracking state event
     * @param arFrame: frame to be evaluated
     */
    fun update(arFrame: Frame) {
        when (val reason = arFrame.camera.trackingFailureReason) {
            //Loss of tracking due to Camera Unavailable
            TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                framesWithNotAvailableTracking += 1
            }
            TrackingFailureReason.NONE -> {
                //Normal ARCore behavior
                framesWithNormalTrackingState += 1
                Log.d("NORMAL","None TrackingState: $framesWithNormalTrackingState")
            }
            else -> {
                //Excessive Motion Event
                //Loss of tracking due to bad internal state
                //Loss of tracking due to Insufficient Features
                //Loss of tracking due to Insufficient Light
                if(framesWithLimitedTrackingStateByReason.containsKey(reason)){
                    val count = framesWithLimitedTrackingStateByReason[reason]!!
                    framesWithLimitedTrackingStateByReason[reason] = count + 1
                }else{
                    framesWithLimitedTrackingStateByReason[reason] = 0
                }
                framesWithLimitedTrackingState += 1
            }
        }
        totalNumberOfFrames += 1
    }
}