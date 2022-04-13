package com.fantasmo.sdk.models.analytics

import com.fantasmo.sdk.evaluators.FMFrameEvaluationType
import com.fantasmo.sdk.filters.FMFrameFilterRejectionReason
import com.fantasmo.sdk.models.FMFrame

class FMFrameEvaluationStatistics (val type: FMFrameEvaluationType) {
    /// Model representing a single frame evaluation window
    class Window (val start: Double){

        var currentScore: Float? = null

        var currentBestScore: Float? = null

        var currentRejectionReason: FMFrameFilterRejectionReason? = null

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

    /// Total evaluations in the session.
    var totalEvaluations: Int = 0
    private set

    /// Total rejections in the session.
    val totalRejections: MutableMap<FMFrameFilterRejectionReason,Int> = mutableMapOf(FMFrameFilterRejectionReason.PITCH_TOO_LOW to 0,
        FMFrameFilterRejectionReason.PITCH_TOO_HIGH to 0,
        FMFrameFilterRejectionReason.MOVING_TOO_FAST to 0,
        FMFrameFilterRejectionReason.MOVING_TOO_LITTLE to 0,
        FMFrameFilterRejectionReason.INSUFFICIENT_FEATURES to 0)


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
        window.currentRejectionReason = null
    }

    /// Sets the best frame for the current window.
    fun setCurrentBest(frame: FMFrame) {
        val evaluation = frame.evaluation ?: return
        if(windows.size > 0) {
            val window = windows.last()
            window.currentBestScore = evaluation.score
        }
    }

    /// Increment the count for a specific filter rejection and set as the current rejection.
    fun addFilterRejection(rejectionReason: FMFrameFilterRejectionReason) {
        if(windows.size == 0)
            return

        val window = windows.last()
        // Add to session totals
        val totalRejectionsForReason = totalRejections[rejectionReason] ?: 0
        totalRejections[rejectionReason] = totalRejectionsForReason + 1

        // Add to current window
        window.rejections += 1
        window.currentRejectionReason = rejectionReason
    }

    /// Reset all statistics, used when starting a new session.
    fun reset() {
        windows.clear()
        highestScore = null
        lowestScore = null
        sumOfAllScores = 0f
        totalEvaluations = 0
        totalRejections.keys.forEach{totalRejections[it] = 0}
    }
}