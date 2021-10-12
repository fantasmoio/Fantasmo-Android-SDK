package com.fantasmo.sdk.filters

import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

/**
 * Class responsible for filtering frames due to ARCore tracking failures.
 * Prevents from sending frames that were broken during ARSession
 */
class FMTrackingStateFilter : FMFrameFilter {

    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        if (arFrame.camera.trackingState == TrackingState.TRACKING) {
            //Normal behavior
            return FMFrameFilterResult.Accepted
        } else if (arFrame.camera.trackingState == TrackingState.PAUSED &&
            arFrame.camera.trackingFailureReason == TrackingFailureReason.NONE
        ) {
            // Initializing
            return FMFrameFilterResult.Rejected(FMFilterRejectionReason.MOVINGTOOLITTLE)
        } else return when (arFrame.camera.trackingFailureReason) {
            TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                // Motion tracking was paused because the camera
                // is in use by another application
                FMFrameFilterResult.Rejected(FMFilterRejectionReason.MOVINGTOOLITTLE)
            }
            TrackingFailureReason.EXCESSIVE_MOTION -> {
                // Ask the user to move the device more slowly
                FMFrameFilterResult.Rejected(FMFilterRejectionReason.MOVINGTOOFAST)
            }
            TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                // Ask the user to move to a different area and to
                // avoid blank walls and surfaces without detail
                FMFrameFilterResult.Rejected(FMFilterRejectionReason.INSUFFICIENTFEATURES)
            }
            TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                // Ask the user to move to a more brightly lit area
                FMFrameFilterResult.Rejected(FMFilterRejectionReason.INSUFFICIENTFEATURES)
            }
            else -> {
                // No specific user action is likely to resolve this issue
                FMFrameFilterResult.Rejected(FMFilterRejectionReason.MOVINGTOOFAST)
            }
        }
    }
}