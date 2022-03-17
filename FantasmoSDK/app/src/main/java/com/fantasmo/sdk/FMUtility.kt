package com.fantasmo.sdk

import android.graphics.*
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.utilities.math.Vector3
import com.google.ar.core.Pose
import kotlin.math.*

/**
 * Class with utility methods and constants
 */
class FMUtility {

    companion object {

        /**
         * Utility method to get the correct Pose for each of the device orientations.
         */
        fun getPoseOfOpenCVVirtualCameraBasedOnDeviceOrientation(fmFrame: FMFrame): FMPose {
            return FMPose(fmFrame.camera.displayOrientedPose)
        }

        /**
         * Calculate the FMPose difference of the anchor frame with respect to the given frame.
         * @param arFrame the current AR Frame.
         */
        fun anchorDeltaPoseForFrame(fmFrame: FMFrame, anchorFrame: FMFrame): FMPose {
            // Pose of frame must be taken for "virtual" device as we send to server orientation of
            // "virtual" device for "localization" frame
            val poseARVirtualFrame = fmFrame.camera.displayOrientedPose
            val poseAnchor = anchorFrame.cameraPose

            return if (poseAnchor != null && poseARVirtualFrame != null) {
                FMPose.diffPose(poseAnchor, poseARVirtualFrame)
            } else {
                FMPose()
            }
        }

        /// Axis vector must be represented with unit vector.
        fun makeRotation(angle: Float, axis: Vector3): Pose {
            assert( abs(axis.length() - 1) < 0.001 )
            val a = sin(angle/2)
            return Pose.makeRotation(a * axis.x, a * axis.y, a * axis.z, cos(angle/2))
        }

        /**
         * Converts Quaternion to Euler Angles.
         * Source: https://github.com/IdeoG/quaternion-vector3-java/blob/master/Quaternion.java
         * @param rotationQuaternion: rotation quaternion correspondent to rotation of the device
         * */
        fun convertQuaternionToEuler(rotationQuaternion: FloatArray): FloatArray {
            val eulerAngles = FloatArray(3)
            val w = rotationQuaternion[3]
            val x = rotationQuaternion[0]
            val y = rotationQuaternion[1]
            val z = rotationQuaternion[2]
            val sqw = w * w
            val sqx = x * x
            val sqy = y * y
            val sqz = z * z

            eulerAngles[0] =
                atan2(2.0 * (x * y + z * w), (sqx - sqy - sqz + sqw).toDouble()).toFloat()
            eulerAngles[1] =
                atan2(2.0 * (y * z + x * w), (-sqx - sqy + sqz + sqw).toDouble()).toFloat()
            eulerAngles[2] = asin(-2.0 * (x * z - y * w)).toFloat()

            return eulerAngles
        }

        fun convertToDegrees(eulerAngles: FloatArray): FloatArray {
            eulerAngles[0] = Math.toDegrees(eulerAngles[0].toDouble()).toFloat()
            eulerAngles[1] = Math.toDegrees(eulerAngles[1].toDouble()).toFloat()
            eulerAngles[2] = Math.toDegrees(eulerAngles[2].toDouble()).toFloat()
            return eulerAngles
        }

        // Euclidean distance https://en.wikipedia.org/wiki/Euclidean_distance
        fun distance(translation: FloatArray, previousTranslation: FloatArray): Float {
            return sqrt(
                (translation[0] - previousTranslation[0]).pow(2) +
                        (translation[1] - previousTranslation[1]).pow(2) +
                        (translation[2] - previousTranslation[2]).pow(2)
            )
        }
    }

    object Constants {
        // Compression factor of JPEG encoding, range 0 (worse) to 100 (best).
        // Anything below 70 severely degrades localization recall and accuracy.
        const val JpegCompressionRatio: Int = 90

        // Scale factor when encoding an image to JPEG.
        const val ImageScaleFactor: Double = 2.0 / 3.0

        // Default pixel buffer resolution and plane count for ARFrames.
        const val PixelBufferWidth: Int = 1920
        const val PixelBufferHeight: Int = 1440
        const val PixelBufferPlaneCount: Int = 2

        const val defaultConfigId = "default-android_17.01.22"
        const val fileName = "remote_config.json"
    }
}