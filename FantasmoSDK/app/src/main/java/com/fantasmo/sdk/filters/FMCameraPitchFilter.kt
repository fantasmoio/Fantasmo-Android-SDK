package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.fantasmo.sdk.FMUtility.Companion.convertQuaternionToEuler
import com.fantasmo.sdk.FMUtility.Companion.convertToDegrees
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.FMFrameRejectionReason

/**
 * Class responsible for filtering frames due to critical angles.
 * Prevents from sending ground and sky images which have no characteristics
 * to determine location
 * Initializes with maximum values for tilting phone up or down in degrees
 */
internal class FMCameraPitchFilter(
    lookDownThreshold: Float,
    lookUpThreshold: Float,
    private val context: Context
) : FMFrameFilter {
    override val TAG = FMCameraPitchFilter::class.java.simpleName

    private val interval = if(lookDownThreshold < 0){
        lookDownThreshold..lookUpThreshold
    }else {
        (-lookDownThreshold)..lookUpThreshold
    }
    /**
     * Check frame acceptance.
     * @param arFrame Frame to be evaluated
     * @return Accepts frame or Rejects frame with PitchTooHigh or PitchTooLow failure
     */
    override fun accepts(fmFrame: FMFrame): FMFrameFilterResult {
        // RotationQuaternion virtual camera pose
        val orientedQuaternion = fmFrame.camera.displayOrientedPose.rotationQuaternion
        // RotationQuaternion from device sensor system
        val sensorQuaternion = fmFrame.androidSensorPose?.rotationQuaternion
            ?: return FMFrameFilterResult.Rejected(FMFrameRejectionReason.FRAME_ERROR)

        val rotation: Int = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation!!
        } else {
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
                FMFrameFilterResult.Accepted
            }
        }
    }

    /**
     * Verifies if tilt angle is acceptable, high or low
     * @param rotationQuaternion rotation quaternion correspondent to rotation of the device
     * @param orientationSign device orientation
     * @return FMFrameFilterResult
     */
    private fun checkTilt(
        rotationQuaternion: FloatArray,
        orientationSign: Int
    ): FMFrameFilterResult {
        val eulerAngles = convertToDegrees(convertQuaternionToEuler(rotationQuaternion))
        return when {
            // If it's looking Up or Down and it's in threshold
            (eulerAngles[0] in interval) -> {
                FMFrameFilterResult.Accepted
            }
            // If it's looking Up
            rotationQuaternion[0] * orientationSign < 0 -> {
                FMFrameFilterResult.Rejected(FMFrameRejectionReason.PITCH_TOO_HIGH)
            }
            // Else it's looking Down
            else -> {
                FMFrameFilterResult.Rejected(FMFrameRejectionReason.PITCH_TOO_LOW)
            }
        }
    }
}