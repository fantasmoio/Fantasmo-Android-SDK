package com.fantasmo.sdk.models.tensorflowML

sealed class ImageQualityEstimationResult {

    object UNKNOWN : ImageQualityEstimationResult()
    class ESTIMATE(val score: Float) : ImageQualityEstimationResult()
    class ERROR(val message: String) : ImageQualityEstimationResult()

    fun description(): String {
        return when (this){
            is ESTIMATE -> "Estimate: $score"
            is ERROR -> "Error: $message"
            else -> "Unknown"
        }
    }
}
