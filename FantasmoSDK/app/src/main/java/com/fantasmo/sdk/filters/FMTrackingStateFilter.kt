package com.fantasmo.sdk.filters

import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.FMFrameRejectionReason
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

/**
 * Class responsible for filtering frames due to ARCore tracking failures.
 * Prevents from sending frames that were broken during ARSession
 */
class FMTrackingStateFilter : FMFrameFilter {
    override val TAG = FMTrackingStateFilter::class.java.simpleName

    override fun accepts(fmFrame: FMFrame): FMFrameFilterResult {
        if (fmFrame.camera.trackingState == TrackingState.TRACKING) {
            //Normal behavior
            return FMFrameFilterResult.Accepted
        } else if (fmFrame.camera.trackingState == TrackingState.PAUSED &&
            fmFrame.camera.trackingFailureReason == TrackingFailureReason.NONE
        ) {
            // Initializing
            return FMFrameFilterResult.Rejected(FMFrameRejectionReason.MOVING_TOO_LITTLE)
        } else return when (fmFrame.camera.trackingFailureReason) {
            TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                // Motion tracking was paused because the camera
                // is in use by another application
                FMFrameFilterResult.Rejected(FMFrameRejectionReason.MOVING_TOO_LITTLE)
            }
            TrackingFailureReason.EXCESSIVE_MOTION -> {
                // Ask the user to move the device more slowly
                FMFrameFilterResult.Rejected(FMFrameRejectionReason.MOVING_TOO_FAST)
            }
            TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                // Ask the user to move to a different area and to
                // avoid blank walls and surfaces without detail
                FMFrameFilterResult.Rejected(FMFrameRejectionReason.TRACKING_STATE_INSUFFICIENT_FEATURES)
            }
            TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                // Ask the user to move to a more brightly lit area
                FMFrameFilterResult.Rejected(FMFrameRejectionReason.TRACKING_STATE_INSUFFICIENT_FEATURES)
            }
            else -> {
                // No specific user action is likely to resolve this issue
                FMFrameFilterResult.Rejected(FMFrameRejectionReason.MOVING_TOO_FAST)
            }
        }
    }
}