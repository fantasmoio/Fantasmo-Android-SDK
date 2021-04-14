package com.fantasmo.sdk.utilities

class MovingAverage {
    private var index = 0
    private var period = 0
    private lateinit var samples : ArrayList<Double>

    constructor(period: Int = 30) {
        this.period = period
        samples = ArrayList()
    }

    var average = (samples.sum())/(samples.size)

    fun addSample(value: Double): Double{
        if(samples.size == period){
            samples[index] = value
            index = (index+1)%period
        }else{
            samples.add(value)
        }
        return average
    }
}