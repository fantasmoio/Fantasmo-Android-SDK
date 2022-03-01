package com.fantasmo.sdk.evaluators

import com.fantasmo.sdk.models.FMFrame

interface FMFrameEvaluator {
    val TAG: String
    /// in-place evaluation, should set FMFrameEvaluation object on frame
    fun evaluate(fmFrame: FMFrame)
}