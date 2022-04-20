package com.fantasmo.sdk.models.analytics

import com.fantasmo.sdk.models.FMFrame
import kotlin.math.max
import kotlin.math.min

/**
 * Class responsible for tracking total rotation movement during the session
 * Calculates the spread rotation of a device based on the device rotation provided by the sequence of ARCore frames
 * Also measures the maximum and minimum angles in radians in all axis (x,y,z)
 * */

class RotationSpreadAccumulator {
    var min : Float = Float.POSITIVE_INFINITY
    private set
    var max : Float = Float.NEGATIVE_INFINITY
    private set
    val spread : Float
    get() = min(max(max - min, 0f), 360f)

    fun addValue(value: Float) {
        min = min(value, min)
        max = max(value, max)
    }

    fun reset() {
        min = Float.POSITIVE_INFINITY
        max = Float.NEGATIVE_INFINITY
    }
}

class TotalDeviceRotationAccumulator {

    // Euler Angles follow these coordinates:
    // https://developer.android.com/guide/topics/sensors/sensors_overview#sensors-coords
    // Minimum, Maximum and Spread values of rotation in each axis
    // Being Min=[0], Max=[1] and Spread=[2]

    // Pitch is rotation on X axis
    // Valid range is [−π/2,π/2], Pitch spread ∈ [0,π]
    val pitch = RotationSpreadAccumulator()

    // Roll is rotation on Y axis
    // Max and Min valid range is [−π,π], Roll spread ∈ [0,2π]
    val roll = RotationSpreadAccumulator()

    // Yaw is rotation on Z axis
    // Max and Min valid range is [−π,π], Yaw spread ∈ [0,2π]
    var yaw = RotationSpreadAccumulator()

    private var frameCounter: Int = 0

    /**
     * On every frame, update get the rotation from the current frame
     * @param fmFrame FMFrame
     */
    fun update(fmFrame: FMFrame) {
        val rotation = fmFrame.sensorAngles
        if(rotation != null) {
            updateRotationValues(rotation)
        }
        frameCounter += 1
    }

    /**
     * Method called by updated responsible for finding new maximum
     * and minimum values from the rotation Quaternion of the frame
     * Also updates the total amount of rotation on each axis
     * @param rotation FloatArray correspondent to the rotationQuaternion
     */
    private fun updateRotationValues(rotation: FloatArray) {
        pitch.addValue(rotation[0])
        roll.addValue(rotation[1])
        yaw.addValue(rotation[2])
    }

    /**
     * Resets counters and each axis rotation array
     */
    fun reset() {
        frameCounter = 0
        pitch.reset()
        roll.reset()
        yaw.reset()
    }
}