package com.fantasmo.sdk.models.analytics

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Class responsible for holding the reads from the magnetometer values.
 */
class MagneticField(
    var x: Float,
    var y: Float,
    var z: Float
)

/**
 * Class responsible for getting magnetometer events values.
 * Encapsulates device motion updates used for getting magnetometer data.
 * If no data is gathered, an all-zero field is return.
 */
class MotionManager(val context: Context) : SensorEventListener {

    private val TAG = "MotionManager"
    private var disabledSensor = false
    private var registered = false

    lateinit var magneticField: MagneticField

    private lateinit var sensorManager: SensorManager
    private lateinit var magnetometer: Sensor

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
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

    fun restart() {
        magneticField = MagneticField(0f, 0f, 0f)
        if (!disabledSensor) {
            sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
            // Verifying if Sensor Magnetic_Field exists
            if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
                //In positive case register it
                magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                sensorManager.registerListener(
                    this,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                registered = true
                Log.d(TAG, "Sensor Magnetic Field found")
            } else {
                //Else non existent
                Log.d(TAG, "Sensor Magnetic Field not found")
            }
        }
    }

    fun stop() {
        magneticField = MagneticField(0f, 0f, 0f)
        if (!disabledSensor) {
            if (registered) {
                sensorManager.unregisterListener(this, magnetometer)
                registered = false
            }
        }
    }

    private fun disableSensor() {
        disabledSensor = true
    }
}

