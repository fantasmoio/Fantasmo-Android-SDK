package com.fantasmo.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.Settings
import com.fantasmo.sdk.fantasmosdk.BuildConfig
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
            val poseAnchor = anchorFrame.camera.pose

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
         * Source: https://de.mathworks.com/matlabcentral/fileexchange/20696-function-to-convert-between-dcm-euler-angles-quaternions-and-euler-vectors
         * Calculating Euler Angles in YXZ order,
         * @param rotationQuaternion: rotation quaternion correspondent to rotation of the device
         * */
        fun convertQuaternionToEuler(rotationQuaternion: FloatArray): FloatArray {
            var qw = rotationQuaternion[3]
            var qx = rotationQuaternion[0]
            var qy = rotationQuaternion[1]
            var qz = rotationQuaternion[2]

            val yaw: Float
            val pitch: Float
            val roll: Float
            val sqw = qw * qw
            val sqx = qx * qx
            val sqy = qy * qy
            val sqz = qz * qz

            val unit = sqx + sqy + sqz + sqw // if normalised is one, otherwise is correction factor
            val norm = sqrt(unit)
            qw /= norm
            qx /= norm
            qy /= norm
            qz /= norm

            val test: Float = qx * qw - qy * qz
            if (test > 0.499 * unit) { // singularity at north pole
                roll = (2 * atan2(qx, qw))
                pitch = (Math.PI / 2).toFloat()
                yaw = 0f
                return floatArrayOf(pitch, roll, yaw)
            }
            if (test < -0.499 * unit) { // singularity at south pole
                roll = (-2 * atan2(qx, qw))
                pitch = (-Math.PI / 2).toFloat()
                yaw = 0f
                return floatArrayOf(pitch, roll, yaw)
            }
            // order yxz, psi=yaw, theta=pitch, phi=roll
            roll=atan2(2f*(qx*qz+qy*qw),sqw-sqx-sqy+sqz)
            pitch=asin(2f*(qx*qw-qy*qz))
            yaw=atan2(2f*(qx*qy+qz*qw),(sqw-sqx+sqy-sqz))

            return floatArrayOf(pitch, roll, yaw)
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

class FMDeviceAndHostInfo(context: Context) {
    @SuppressLint("HardwareIds")
    val udid = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val manufacturer: String = Build.MANUFACTURER // Samsung
    val model: String = Build.MODEL  // SM-G780
    val deviceOs = "android"
    val deviceModel = "$manufacturer $model" // Samsung SM-G780
    val deviceOsVersion = Build.VERSION.SDK_INT.toString() // "30" (Android 11)
    val sdkVersion = BuildConfig.VERSION_NAME // "1.0.5"
    private val packageInfo = context.packageManager
        .getPackageInfo(context.packageName, 0)
    val hostAppMarketingVersion = packageInfo.versionName
    val hostAppBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode.toString()
    }  else {
        packageInfo?.versionCode.toString()
    }
    val hostAppBundleIdentifier = context.packageName
}