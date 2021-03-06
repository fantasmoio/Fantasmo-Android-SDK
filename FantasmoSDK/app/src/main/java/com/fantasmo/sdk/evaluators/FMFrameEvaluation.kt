package com.fantasmo.sdk.evaluators
import com.fantasmo.sdk.models.FMFrameRejectionReason
import com.fantasmo.sdk.models.analytics.FMImageQualityUserInfo

enum class FMFrameEvaluationType {
    IMAGE_QUALITY_ESTIMATION
}

data class FMFrameEvaluation (val type: FMFrameEvaluationType,
                                val score: Float, // 0.0 - 1.0
                                val time: Float, // time it took to perform the evaluation in seconds
                                val imageQualityUserInfo: FMImageQualityUserInfo?  // optional analytics etc.
                            )

internal sealed class FMFrameEvaluationResult {
    object NewCurrentBest: FMFrameEvaluationResult()
    class Discarded(val reason: FMFrameEvaluationDiscardReason): FMFrameEvaluationResult()

    fun getDiscardReason(): FMFrameEvaluationDiscardReason? {
        return when (this){
            is Discarded -> reason
            else -> null
        }
    }

}

internal sealed class FMFrameEvaluationDiscardReason {
    object BelowMinScoreThreshold: FMFrameEvaluationDiscardReason()
    object BelowCurrentBestScore: FMFrameEvaluationDiscardReason()
    object OtherEvaluationInProgress: FMFrameEvaluationDiscardReason()
    object EvaluatorError: FMFrameEvaluationDiscardReason()
    class RejectedByFilter(val reason: FMFrameRejectionReason): FMFrameEvaluationDiscardReason()

    fun getRejectionReason(): FMFrameRejectionReason? {
        return when (this){
            is RejectedByFilter -> reason
            else -> null
        }
    }
}