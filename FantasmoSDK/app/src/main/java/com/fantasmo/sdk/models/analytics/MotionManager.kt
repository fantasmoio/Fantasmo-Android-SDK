package com.fantasmo.sdk.models.analytics

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class MotionManager(context: Context) : SensorEventListener {

    private var TAG = "MotionManager"

    private var sensorManager: SensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager

    private var magnetometer: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var magnetometerReading = floatArrayOf()

    override fun onSensorChanged(event: SensorEvent?) {
        when(event?.sensor?.type) {
            //GET MAGNETIC VALUES
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetometerReading = event.values
                Log.d(TAG, "Reading on x: ${magnetometerReading[0]};\n" +
                        "Reading on y: ${magnetometerReading[1]};\n" +
                        "Reading on z: ${magnetometerReading[2]}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    fun restart(){
        Log.d(TAG, "Restart")
        sensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop(){
        Log.d(TAG, "Stop")
        magnetometerReading = floatArrayOf()
        sensorManager.unregisterListener(this,magnetometer)
    }
}

