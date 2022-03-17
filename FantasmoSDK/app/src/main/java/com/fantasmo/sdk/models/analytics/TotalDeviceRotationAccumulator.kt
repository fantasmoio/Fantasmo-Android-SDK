package com.fantasmo.sdk.models.analytics

import com.fantasmo.sdk.FMUtility.Companion.convertQuaternionToEuler
import com.fantasmo.sdk.models.FMFrame
import kotlin.math.max
import kotlin.math.min

/**
 * Class responsible for tracking total rotation movement during the session
 * Calculates the spread rotation of a device based on the device rotation provided by the sequence of ARCore frames
 * Also measures the maximum and minimum angles in radians in all axis (x,y,z)
 * */
class TotalDeviceRotationAccumulator {

    // Euler Angles follow this rule:
    // https://www.euclideanspace.com/maths/geometry/rotations/euler/index.htm
    // Minimum, Maximum and Spread values of rotation in each axis
    // Being Min=[0], Max=[1] and Spread=[2]
    // Roll is rotation on X axis
    // Max and Min valid range is [−π,π], Roll spread ∈ [0,2π]
    var roll = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f)
        private set

    // Yaw is rotation on Y axis
    // Max and Min valid range is [−π,π], Yaw spread ∈ [0,2π]
    var yaw = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f)
        private set

    // Pitch is rotation on Z axis
    // Valid range is [−π/2,π/2], Pitch spread ∈ [0,π]
    var pitch = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f)
        private set

    private var frameCounter: Int = 0

    /**
     * On every frame, update get the rotation from the current frame
     * @param fmFrame FMFrame
     */
    fun update(fmFrame: FMFrame) {
        val rotation = fmFrame.cameraAngles
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
        val yawCurrent = rotation[0]
        val pitchCurrent = rotation[1]
        val rollCurrent = rotation[2]

        //Update Yaw values
        yaw[0] = min(yawCurrent, yaw[0])
        yaw[1] = max(yawCurrent, yaw[1])
        yaw[2] = min(max(yaw[1] - yaw[0], 0f), 360f)

        //Update Pitch values
        pitch[0] = min(pitchCurrent, pitch[0])
        pitch[1] = max(pitchCurrent, pitch[1])
        pitch[2] = min(max(pitch[1] - pitch[0], 0f), 360f)

        //Update Roll values
        roll[0] = min(rollCurrent, roll[0])
        roll[1] = max(rollCurrent, roll[1])
        roll[2] = min(max(roll[1] - roll[0], 0f), 360f)
    }

    /**
     * Resets counters and each axis rotation array
     */
    fun reset() {
        frameCounter = 0
        yaw = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f)
        pitch = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f)
        roll = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f)
    }
}