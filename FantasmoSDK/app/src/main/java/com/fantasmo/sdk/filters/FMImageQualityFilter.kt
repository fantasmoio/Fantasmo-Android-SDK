package com.fantasmo.sdk.filters

import android.content.Context
import android.util.Log
import com.fantasmo.sdk.models.tensorflowML.ImageQualityEstimationResult
import com.fantasmo.sdk.models.tensorflowML.ImageQualityEstimator
import com.google.ar.core.Frame

class FMImageQualityFilter(val context: Context) : FMFrameFilter {
    private val TAG = FMImageQualityFilter::class.java.simpleName

    private var imageQualityEstimator = ImageQualityEstimator.makeEstimator(context)

    private val scoreThreshold = 0.0

    private var lastImageQualityScore = 0f

    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        val iqeResult = imageQualityEstimator.estimateImageQuality(arFrame)
        Log.d(TAG, iqeResult.description())
        return when (iqeResult) {
            is ImageQualityEstimationResult.ERROR -> FMFrameFilterResult.Accepted//FMFrameFilterResult.Rejected(FMFilterRejectionReason.IMAGEQUALITYSCOREBELOWTHRESHOLD)
            is ImageQualityEstimationResult.ESTIMATE -> {
                lastImageQualityScore = iqeResult.score
                if (iqeResult.score >= scoreThreshold) {
                    FMFrameFilterResult.Accepted
                } else {
                    FMFrameFilterResult.Rejected(FMFilterRejectionReason.IMAGEQUALITYSCOREBELOWTHRESHOLD)
                }
            }
            ImageQualityEstimationResult.UNKNOWN -> {
                FMFrameFilterResult.Accepted
            }
        }
    }
}