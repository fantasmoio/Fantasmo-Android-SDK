package com.fantasmo.sdk.evaluators

import com.fantasmo.sdk.filters.FMFrameFilterRejectionReason

enum class FMFrameEvaluationType {
    IMAGE_QUALITY_ESTIMATION
}

data class FMFrameEvaluation (val type: FMFrameEvaluationType,
                                val score: Float, // 0.0 - 1.0
                                val imageQualityUserInfo: Map<String, String?>?  // optional analytics etc.
                            )

sealed class FMFrameEvaluationResult {
    object NewCurrentBest: FMFrameEvaluationResult()
    class Discarded(val reason: FMFrameEvaluationDiscardReason): FMFrameEvaluationResult()

    fun getDiscardReason(): FMFrameEvaluationDiscardReason? {
        return when (this){
            is Discarded -> reason
            else -> null
        }
    }

}

sealed class FMFrameEvaluationDiscardReason {
    object BelowMinScoreThreshold: FMFrameEvaluationDiscardReason()
    object BelowCurrentBestScore: FMFrameEvaluationDiscardReason()
    object OtherEvaluationInProgress: FMFrameEvaluationDiscardReason()
    object EvaluatorError: FMFrameEvaluationDiscardReason()
    class RejectedByFilter(val reason: FMFrameFilterRejectionReason): FMFrameEvaluationDiscardReason()

    fun getRejectionReason(): FMFrameFilterRejectionReason? {
        return when (this){
            is RejectedByFilter -> reason
            else -> null
        }
    }
}