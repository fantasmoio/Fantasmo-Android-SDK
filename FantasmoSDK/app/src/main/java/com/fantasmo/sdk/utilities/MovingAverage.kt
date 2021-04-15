package com.fantasmo.sdk.utilities

class MovingAverage(private var period: Int = 30) {
    private var index = 0
    private var samples : ArrayList<Double> = ArrayList()

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