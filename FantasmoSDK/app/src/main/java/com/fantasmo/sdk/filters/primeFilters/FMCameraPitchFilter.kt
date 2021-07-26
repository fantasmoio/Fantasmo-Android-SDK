package com.fantasmo.sdk.filters.primeFilters

import android.content.Context
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.fantasmo.sdk.filters.FMFrameFilterFailure
import com.fantasmo.sdk.filters.FMFrameFilterResult
import com.fantasmo.sdk.filters.FMFrameFilter
import com.google.ar.core.Frame
import kotlin.math.abs

/**
 * Class responsible for filtering frames due to critical angles.
 * Prevents from sending ground and sky images which have no characteristics
 * to determine location
 */
class FMCameraPitchFilter(private val context: Context) : FMFrameFilter {
    // Maximum value for tilting phone up or down
    private val radianThreshold = 0.16

    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with PitchTooHigh or PitchTooLow failure
     */
    override fun accepts(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        // Angle of X-plane of virtual camera pose
        val xOrientedAngle = arFrame.camera.displayOrientedPose.rotationQuaternion[0]
        // Angle of X-plane of device sensor system
        val xSensorAngle = arFrame.androidSensorPose.rotationQuaternion[0]

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
                return checkTilt(xOrientedAngle,1)
            }
            // SCREEN_ORIENTATION_LANDSCAPE
            Surface.ROTATION_90 -> {
                return checkTilt(xOrientedAngle,1)
            }
            // SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_0 -> {
                return checkTilt(xSensorAngle,-1)
            }
            // SCREEN_ORIENTATION_REVERSE_PORTRAIT
            Surface.ROTATION_180 -> {
                return checkTilt(xSensorAngle,-1)
            }
            else -> {
                Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
            }
        }
    }

    /**
     * Verifies if tilt angle is acceptable, high or low
     * @param xAngle: tilt angle value
     * @param orientationSign: device orientation
     * @return Pair<FMFrameFilterResult, FMFrameFilterFailure>
     * */
    private fun checkTilt(xAngle: Float, orientationSign: Int): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        return when {
            abs(xAngle) <= radianThreshold -> {
                Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
            }
            xAngle * orientationSign < 0 -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
            }
            else -> {
                Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
            }
        }
    }
}
