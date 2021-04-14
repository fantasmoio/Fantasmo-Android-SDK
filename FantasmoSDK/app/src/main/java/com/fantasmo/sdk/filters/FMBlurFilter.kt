package com.fantasmo.sdk.filters

import com.fantasmo.sdk.utilities.MovingAverage

class FMBlurFilter {
    var variance = 0
    var varianceAverager = MovingAverage()
    //var averageVariance: Double(varianceAverager.average)

    var varianceThreshold = 4.0 // empirically determined
    var suddenDropThreshold = 2.0 // empirically determined
}