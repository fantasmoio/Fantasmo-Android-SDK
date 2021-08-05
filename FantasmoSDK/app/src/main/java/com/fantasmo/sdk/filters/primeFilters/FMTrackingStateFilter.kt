package com.fantasmo.sdk.filters.primeFilters

import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason

/**
 * Class responsible for filtering frames due to ARCore tracking failures.
 * Prevents from sending frames that were broken during ARSession
 */
class FMTrackingStateFilter : FMFrameFilter {

    override fun accepts(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        return when (arFrame.camera.trackingFailureReason) {
            TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE)
            }
            TrackingFailureReason.EXCESSIVE_MOTION -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOFAST)
            }
            TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.INSUFFICIENTFEATURES)
            }
            TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.INSUFFICIENTFEATURES)
            }
            TrackingFailureReason.BAD_STATE -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOFAST)
            }
            else -> Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
        }
    }
}