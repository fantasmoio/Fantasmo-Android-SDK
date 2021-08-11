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

    lateinit var magneticField: MagneticField

    private var sensorManager: SensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager

    private var magnetometer: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

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
        magneticField = MagneticField(0f,0f,0f)
        sensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop(){
        magneticField = MagneticField(0f,0f,0f)
        sensorManager.unregisterListener(this,magnetometer)
    }
}

