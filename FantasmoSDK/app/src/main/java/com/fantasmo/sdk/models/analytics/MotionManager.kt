package com.fantasmo.sdk.models.analytics

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MagneticField(
    var x : Float,
    var y : Float,
    var z : Float
)

class MotionManager(context: Context) : SensorEventListener {

    private var disabledSensor = false

    lateinit var magneticField: MagneticField

    private var sensorManager: SensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager

    private lateinit var magnetometer: Sensor

    override fun onSensorChanged(event: SensorEvent?) {
        when(event?.sensor?.type) {
            //GET MAGNETIC VALUES
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticField = MagneticField(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    fun restart(){
        if(!disabledSensor){
            magneticField = MagneticField(0f,0f,0f)
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            sensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_NORMAL)
        }else{
            magneticField = MagneticField(0f,0f,0f)
        }

    }

    fun stop(){
        if(!disabledSensor){
            magneticField = MagneticField(0f,0f,0f)
            sensorManager.unregisterListener(this,magnetometer)
        }
        else{
            magneticField = MagneticField(0f,0f,0f)
        }
    }

    private fun disableSensor(){
        disabledSensor = true
    }
}

