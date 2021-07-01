package com.fantasmo.sdk.frameSequenceFilter

import android.content.Context
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.Frame
import kotlin.math.abs

/**
 * Class responsible for filtering frames due to critical angles.
 * Prevents from sending ground and sky images which have no characteristics
 * to determine location
 */
class FMCameraPitchFilterRule(private val context: Context) : FMFrameSequenceFilterRule {
    // Maximum value for tilting phone up or down
    private val radianThreshold = 0.16

    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with PitchTooHigh or PitchTooLow failure
     */
    override fun check(arFrame: Frame): Pair<FMFrameFilterResult, FMFrameFilterFailure> {
        // Angle of X-plane of virtual camera pose
        val xOrientedPose = arFrame.camera.displayOrientedPose.rotationQuaternion[0]
        // Angle of X-plane of device sensor system
        val xSensorPose = arFrame.androidSensorPose.rotationQuaternion[0]

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
                return when {
                    abs(xOrientedPose) <= radianThreshold -> {
                        Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
                    }
                    xOrientedPose < 0 -> {
                        Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
                    }
                    else -> {
                        Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
                    }
                }
            }
            // SCREEN_ORIENTATION_LANDSCAPE
            Surface.ROTATION_90 -> {
                return when {
                    abs(xOrientedPose) <= radianThreshold -> {
                        Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
                    }
                    xOrientedPose < 0 -> {
                        Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
                    }
                    else -> {
                        Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
                    }
                }
            }
            // SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_0 -> {
                return when {
                    abs(xSensorPose) <= radianThreshold -> {
                        Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
                    }
                    xSensorPose > 0 -> {
                        Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
                    }
                    else -> {
                        Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
                    }
                }
            }
            // SCREEN_ORIENTATION_REVERSE_PORTRAIT
            Surface.ROTATION_180 -> {
                return when {
                    abs(xSensorPose) <= radianThreshold -> {
                        Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
                    }
                    xSensorPose < 0 -> {
                        Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOHIGH)
                    }
                    else -> {
                        Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.PITCHTOOLOW)
                    }
                }
            }
            else -> {
                Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED)
            }
        }
    }
}
