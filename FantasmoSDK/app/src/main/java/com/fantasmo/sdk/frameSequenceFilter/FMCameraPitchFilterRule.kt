package com.fantasmo.sdk.frameSequenceFilter

import android.content.Context
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.Frame
import kotlin.math.*

/**
 * Class responsible for filtering frames due to critical angles.
 * Prevents from sending ground and sky images which have no characteristics
 * to determine location
 */
class FMCameraPitchFilterRule(private val context: Context) : FMFrameSequenceFilterRule {
    // Maximum values for tilting phone up or down
    private val lookDownThreshold = -65.0 // In degrees
    private val lookUpThreshold = 30.0 // In degrees
    
    private var bank : Double = 0.0

    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with PitchTooHigh or PitchTooLow failure
     */
    override fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        // RotationQuaternion virtual camera pose
        val orientedQuaternion = arFrame.camera.displayOrientedPose.rotationQuaternion
        // RotationQuaternion from device sensor system
        val sensorQuaternion = arFrame.androidSensorPose.rotationQuaternion

        val rotation: Int = try {
            context.display?.rotation!!
        } catch (exception: UnsupportedOperationException) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display: Display = wm.defaultDisplay
            display.rotation
        }

        return when (rotation) {
            // SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            Surface.ROTATION_270 -> {
                return checkTilt(orientedQuaternion,1)
            }
            // SCREEN_ORIENTATION_LANDSCAPE
            Surface.ROTATION_90 -> {
                return checkTilt(orientedQuaternion,1)
            }
            // SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_0 -> {
                return checkTilt(sensorQuaternion,-1)
            }
            // SCREEN_ORIENTATION_REVERSE_PORTRAIT
            Surface.ROTATION_180 -> {
                return checkTilt(sensorQuaternion,-1)
            }
            else -> {
                Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
            }
        }
    }

    /**
     * Verifies if tilt angle is acceptable, high or low
     * @param rotationQuaternion: rotation quaternion correspondent to rotation of the device
     * @param orientationSign: device orientation
     * @return Pair<FMFrameFilterResult, FMFrameFilterFailure>
     * */
    private fun checkTilt(rotationQuaternion: FloatArray, orientationSign: Int): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        convertQuaternionToEuler(rotationQuaternion)
        return when {
            // If it's looking Up or Down and it's in threshold
            (bank in lookDownThreshold..lookUpThreshold)  -> {
                Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
            }
            // If it's looking Up
            rotationQuaternion[0] * orientationSign < 0 -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
            }
            // Else it's looking Down
            else -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
            }
        }
    }

    /**
     * Converts Quaternion to Euler Angles
     * Source: https://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/index.htm
     * @param rotationQuaternion: rotation quaternion correspondent to rotation of the device
     * */
    private fun convertQuaternionToEuler(rotationQuaternion: FloatArray){
        val qw = rotationQuaternion[3]
        val qx = rotationQuaternion[0]
        val qy = rotationQuaternion[1]
        val qz = rotationQuaternion[2]

        var heading: Double
        var attitude: Double

        val sqw = qw * qw
        val sqx = qx * qx
        val sqy = qy * qy
        val sqz = qz * qz

        val unit = sqx + sqy + sqz + sqw // if normalised is one, otherwise is correction factor
        val test = qx*qy + qz*qw
        if (test > 0.499*unit) { // singularity at north pole
            heading = (2 * atan2(qx,qw)).toDouble()
            attitude = Math.PI/2
            bank = 0.0
            return
        }
        if (test < -0.499*unit) { // singularity at south pole
            heading = (-2 * atan2(qx,qw)).toDouble()
            attitude = -Math.PI/2
            bank = 0.0
            return
        }
        heading = atan2(2*qy*qw-2*qx*qz , sqx - sqy - sqz + sqw).toDouble()
        attitude = asin(2*test/unit).toDouble()
        bank = atan2(2*qx*qw-2*qy*qz , -sqx + sqy - sqz + sqw).toDouble()

        bank = Math.toDegrees(bank)
    }
}
