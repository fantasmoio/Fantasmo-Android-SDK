package com.fantasmo.sdk.filters.primeFilters

import android.content.Context
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.fantasmo.sdk.FMUtility.Companion.convertQuaternionToEuler
import com.fantasmo.sdk.FMUtility.Companion.convertToDegrees
import com.google.ar.core.Frame

/**
 * Class responsible for filtering frames due to critical angles.
 * Prevents from sending ground and sky images which have no characteristics
 * to determine location
 */
class FMCameraPitchFilter(private val context: Context) : FMFrameFilter {

    // Maximum values for tilting phone up or down
    private val lookDownThreshold = -65.0 // In degrees
    private val lookUpThreshold = 30.0 // In degrees

    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with PitchTooHigh or PitchTooLow failure
     */
    override fun accepts(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
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
                return checkTilt(orientedQuaternion, 1)
            }
            // SCREEN_ORIENTATION_LANDSCAPE
            Surface.ROTATION_90 -> {
                return checkTilt(orientedQuaternion, 1)
            }
            // SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_0 -> {
                return checkTilt(sensorQuaternion, -1)
            }
            // SCREEN_ORIENTATION_REVERSE_PORTRAIT
            Surface.ROTATION_180 -> {
                return checkTilt(sensorQuaternion, -1)
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
    private fun checkTilt(
        rotationQuaternion: FloatArray,
        orientationSign: Int
    ): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        val eulerAngles = convertToDegrees(convertQuaternionToEuler(rotationQuaternion))
        return when {
            // If it's looking Up or Down and it's in threshold
            (eulerAngles[2] in lookDownThreshold..lookUpThreshold) -> {
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
}