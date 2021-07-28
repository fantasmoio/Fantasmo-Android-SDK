package com.fantasmo.sdk.models.analytics

import android.util.Log
import com.google.ar.core.Frame
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class TotalDeviceRotationAccumulator {

    private val TAG = "TotalDeviceRotation"

    // Current value of total rotation in radians, which is updated as more frames are passed via `update(`
    private var totalRotation = 0f

    // Minimum and Maximum values of rotation in each axis
    // Being Min=[0] and Max=[1]
    // Valid range is [−π,π]
    private var minMaxYaw = floatArrayOf(0f, 0f)
    private var spreadYaw = 0f

    // Valid range is [−π/2,π/2]
    private var minMaxPitch = floatArrayOf(0f, 0f)
    private var spreadPitch = 0f

    // Valid range is [−π,π]
    private var minMaxRoll = floatArrayOf(0f, 0f)
    private var spreadRoll = 0f

    private var frameCounter: Int = 0
    private var nextFrameToTake = 0

    fun update(arFrame: Frame) {
        val rotation = arFrame.androidSensorPose.rotationQuaternion
        checkMinMaxDegrees(rotation!!)

        Log.d(
            TAG, "Yaw: (${minMaxYaw[0]},${minMaxYaw[1]});\n" +
                    "Spread Yaw: $spreadYaw\n" +
                    "Pitch: (${minMaxPitch[0]},${minMaxPitch[1]});\n" +
                    "Spread Pitch: $spreadPitch\n" +
                    "Roll: (${minMaxRoll[0]},${minMaxRoll[1]});\n" +
                    "Spread Roll: $spreadRoll\n" +
                    "Frames Visited: $frameCounter;"
        )
        frameCounter += 1
    }

    private fun checkMinMaxDegrees(rotation: FloatArray) {
        val rads = convertQuaternionToEuler(rotation)
        val yaw = rads[0]
        val pitch = rads[1]
        val roll = rads[2]

        //Update Yaw values
        minMaxYaw[0] = min(yaw, minMaxYaw[0])
        minMaxYaw[1] = max(yaw, minMaxYaw[1])
        spreadYaw = min(max(minMaxYaw[1] - minMaxYaw[0], 0f), 2 * Math.PI.toFloat())

        //Update Pitch values
        minMaxPitch[0] = min(pitch, minMaxPitch[0])
        minMaxPitch[1] = max(pitch, minMaxPitch[1])
        spreadPitch = min(max(minMaxPitch[1] - minMaxPitch[0], 0f), 2 * Math.PI.toFloat())

        //Update Roll values
        minMaxRoll[0] = min(roll, minMaxRoll[0])
        minMaxRoll[1] = max(roll, minMaxRoll[1])
        spreadRoll = min(max(minMaxRoll[1] - minMaxRoll[0], 0f), 2 * Math.PI.toFloat())
    }

    /**
     * Converts Quaternion to Euler Angles
     * Source: https://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/index.htm
     * @param rotationQuaternion: rotation quaternion correspondent to rotation of the device
     * */
    private fun convertQuaternionToEuler(rotationQuaternion: FloatArray): FloatArray {
        val qw = rotationQuaternion[3]
        val qx = rotationQuaternion[0]
        val qy = rotationQuaternion[1]
        val qz = rotationQuaternion[2]
        Log.d(TAG, "QW: $qw, QX: $qx, QY: $qy, QZ:$qz")

        val yaw: Float
        val pitch: Float
        val roll: Float

        val sqw = qw * qw
        val sqx = qx * qx
        val sqy = qy * qy
        val sqz = qz * qz

        val unit = sqx + sqy + sqz + sqw // if normalised is one, otherwise is correction factor
        val test = qx * qy + qz * qw
        if (test > 0.499 * unit) { // singularity at north pole
            yaw = (2 * atan2(qx, qw))
            pitch = (Math.PI / 2).toFloat()
            roll = 0f
            return floatArrayOf(yaw, pitch, roll)
        }
        if (test < -0.499 * unit) { // singularity at south pole
            yaw = (-2 * atan2(qx, qw))
            pitch = (-Math.PI / 2).toFloat()
            roll = 0f
            return floatArrayOf(yaw, pitch, roll)
        }

        //Values are in radians
        yaw = atan2(2 * qy * qw - 2 * qx * qz, sqx - sqy - sqz + sqw)
        pitch = kotlin.math.asin(2 * test / unit)
        roll = atan2(2 * qx * qw - 2 * qy * qz, -sqx + sqy - sqz + sqw)

        return floatArrayOf(yaw, pitch, roll)
    }

    fun reset() {
        frameCounter = 0
        nextFrameToTake = 0
        totalRotation = 0f
        spreadYaw = 0f
        spreadPitch = 0f
        spreadRoll = 0f
        minMaxYaw = floatArrayOf(0f, 0f)
        minMaxPitch = floatArrayOf(0f, 0f)
        minMaxRoll = floatArrayOf(0f, 0f)
    }
}