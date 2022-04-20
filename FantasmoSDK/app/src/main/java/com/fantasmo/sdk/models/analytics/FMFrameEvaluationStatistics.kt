package com.fantasmo.sdk.models.analytics

import com.fantasmo.sdk.evaluators.FMFrameEvaluationType
import com.fantasmo.sdk.filters.FMFrameFilter
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.FMFrameRejectionReason

class FMFrameEvaluationStatistics (val type: FMFrameEvaluationType) {
    /// Model representing a single frame evaluation window
    class Window (val start: Double){

        var currentScore: Float? = null

        var currentBestScore: Float? = null

        var currentFilterRejection: FMFrameRejectionReason? = null

        var currentImageQualityUserInfo: FMImageQualityUserInfo? = null

        var evaluations: Int = 0

        var rejections: Int = 0
    }

    /// Ordered list of evaluation windows in the session, last window being the newest.
    var windows: MutableList<Window> = arrayListOf()
    private set
    /// Highest score evaluated in the session.
    var highestScore: Float? = null
    private set

    /// Lowest score evaluated in the session.
    var lowestScore: Float? = null
    private set

    /// Sum of all scores evaluated in the session.
    var sumOfAllScores: Float = 0f
    private set

    /// Total time spent evaluating frames in the session.
    var totalEvaluationTime: Float = 0f
    private set

    /// Total evaluations in the session.
    var totalEvaluations: Int = 0
    private set

    /// Dictionary of frame rejection reasons and the number of times each occurred in the session.
    var rejectionReasons = FMFrameRejectionReason.values().associate { reason -> reason to 0 }.toMutableMap()
    private set

    /// Total rejections in the session.
    val totalRejections: Int
        get() = rejectionReasons.values.sum()

    /// Average of all evaluation scores in the session.
    val averageEvaluationScore: Float
        get() = if(totalEvaluations > 0) sumOfAllScores / totalEvaluations.toFloat() else 0f

    /// Average time it took to evaluate a single frame in the session
    val averageEvaluationTime: Float
        get() = if(totalEvaluations > 0)  totalEvaluationTime / totalEvaluations.toFloat() else 0f


    /// Creates a new window and makes it the current window.
    fun startWindow(startTime: Double) {
        windows.add(Window(startTime))
    }

    /// Adds evaluation data from the given frame to the current window and updates statistics.
    fun addEvaluation(frame: FMFrame) {
        val evaluation = frame.evaluation ?: return
        if(windows.size == 0)
            return

        val window = windows.last()
        // Update session stats
        totalEvaluations += 1
        totalEvaluationTime += evaluation.time
        sumOfAllScores += evaluation.score
        val highestScore = this.highestScore
        if (highestScore == null || evaluation.score > highestScore) {
            this.highestScore = evaluation.score
        }
        val lowestScore = this.lowestScore
        if (lowestScore == null || evaluation.score < lowestScore) {
            this.lowestScore = evaluation.score
        }

        // Update current window stats
        window.evaluations += 1
        window.currentScore = evaluation.score
        window.currentFilterRejection = null
    }

    /// Sets the best frame for the current window.
    fun setCurrentBest(frame: FMFrame) {
        val evaluation = frame.evaluation ?: return
        if(windows.size > 0) {
            val window = windows.last()
            window.currentBestScore = evaluation.score
        }
    }

    /// Increment the count for a rejection type.
    fun addRejection(rejectionReason: FMFrameRejectionReason, filter: FMFrameFilter? = null) {
        if(windows.size == 0)
            return

        val window = windows.last()
        // Add to session totals
        val totalRejectionsForReason = rejectionReasons[rejectionReason] ?: 0
        rejectionReasons[rejectionReason] = totalRejectionsForReason + 1

        // Add to current window
        window.rejections += 1
        if(filter != null)
            window.currentFilterRejection = rejectionReason
    }

    /// Reset all statistics, used when starting a new session.
    fun reset() {
        windows.clear()
        highestScore = null
        lowestScore = null
        sumOfAllScores = 0f
        totalEvaluations = 0
        rejectionReasons = FMFrameRejectionReason.values().associate { reason -> reason to 0 }.toMutableMap()
    }
}