package com.fantasmo.sdk.filters.primeFilters

import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

/**
 * Class responsible for filtering frames due to ARCore tracking failures.
 * Prevents from sending frames that were broken during ARSession
 */
class FMTrackingStateFilter : FMFrameFilter {

    override fun accepts(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        if (arFrame.camera.trackingState == TrackingState.TRACKING) {
            //Normal behavior
            return Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
        } else if (arFrame.camera.trackingState == TrackingState.PAUSED &&
            arFrame.camera.trackingFailureReason == TrackingFailureReason.NONE
        ) {
            // Initializing
            return Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE)
        } else return when (arFrame.camera.trackingFailureReason) {
            TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                // Motion tracking was paused because the camera
                // is in use by another application
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE)
            }
            TrackingFailureReason.EXCESSIVE_MOTION -> {
                // Ask the user to move the device more slowly
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOFAST)
            }
            TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                // Ask the user to move to a different area and to
                // avoid blank walls and surfaces without detail
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.INSUFFICIENTFEATURES)
            }
            TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                // Ask the user to move to a more brightly lit area
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.INSUFFICIENTFEATURES)
            }
            else -> {
                // No specific user action is likely to resolve this issue
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOFAST)
            }
        }
    }
}