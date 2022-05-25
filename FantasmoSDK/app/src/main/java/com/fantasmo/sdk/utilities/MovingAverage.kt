package com.fantasmo.sdk.utilities

internal class MovingAverage(private var period: Int = 30) {
    private var index = 0
    private var samples: ArrayList<Float> = ArrayList()

    var average = (samples.sum()) / (samples.size)

    fun addSample(value: Float): Float {
        if (samples.size == period) {
            samples[index] = value
            index = (index + 1) % period
        } else {
            samples.add(value)
        }
        return average
    }
}