package com.fantasmo.sdk

import android.content.Context
import android.graphics.*
import android.media.Image
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.fantasmo.sdk.models.*
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import java.io.ByteArrayOutputStream
import kotlin.math.*

/**
 * Class with utility methods and constants
 */
class FMUtility {

    companion object {
        const val n2s = 1_000_000_000.0
        /**
         * Method to get the the AR Frame camera image data.
         * @param arFrame the AR Frame to localize.
         * @return a ByteArray with the data of the [arFrame]
         */
        fun getImageDataFromARFrame(context: Context, arFrame: Frame): ByteArray {
            //The camera image
            val cameraImage = arFrame.acquireCameraImage()

            val baOutputStream = createByteArrayOutputStream(cameraImage)

            // Release the image
            cameraImage.close()

            val imageBytes: ByteArray = baOutputStream.toByteArray()
            val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                .rotate(getImageRotationDegrees(context))
            val data = getFileDataFromDrawable(imageBitmap)

            imageBitmap.recycle()
            return data
        }

        fun createByteArrayOutputStream(cameraImage: Image): ByteArrayOutputStream {
            //The camera image received is in YUV YCbCr Format. Get buffers for each of the planes and use
            // them to create a new byte array defined by the size of all three buffers combined
            val cameraPlaneY = cameraImage.planes[0].buffer
            val cameraPlaneU = cameraImage.planes[1].buffer
            val cameraPlaneV = cameraImage.planes[2].buffer

            //Use the buffers to create a new byteArray that
            val compositeByteArray =
                ByteArray(cameraPlaneY.capacity() + cameraPlaneU.capacity() + cameraPlaneV.capacity())

            cameraPlaneY.get(compositeByteArray, 0, cameraPlaneY.capacity())
            cameraPlaneU.get(compositeByteArray, cameraPlaneY.capacity(), cameraPlaneU.capacity())
            cameraPlaneV.get(
                compositeByteArray,
                cameraPlaneY.capacity() + cameraPlaneU.capacity(),
                cameraPlaneV.capacity()
            )

            val baOutputStream = ByteArrayOutputStream()
            val yuvImage = YuvImage(
                compositeByteArray,
                ImageFormat.NV21,
                cameraImage.width,
                cameraImage.height,
                null
            )
            yuvImage.compressToJpeg(
                Rect(0, 0, cameraImage.width, cameraImage.height),
                100,
                baOutputStream
            )
            return baOutputStream
        }

        private fun getImageRotationDegrees(context: Context): Float {
            val rotation: Int = try {
                context.display?.rotation!!
            } catch (exception: UnsupportedOperationException) {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display: Display = wm.defaultDisplay
                display.rotation
            }

            when (rotation) {
                // SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                Surface.ROTATION_270 -> {
                    return -90f
                }
                // SCREEN_ORIENTATION_LANDSCAPE
                Surface.ROTATION_90 -> {
                    return 0f
                }
                // SCREEN_ORIENTATION_PORTRAIT
                Surface.ROTATION_0 -> {
                    return 90f
                }
                // SCREEN_ORIENTATION_REVERSE_PORTRAIT
                Surface.ROTATION_180 -> {
                    return 180f
                }
                else -> {
                    return 90f
                }
            }
        }

        /**
         * Utility method to get the correct Pose for each of the device orientations.
         */
        fun getPoseOfOpenCVVirtualCameraBasedOnDeviceOrientation(context: Context, frame: Frame): FMPose {
            return FMPose(frame.camera.displayOrientedPose)
        }

        /**
         * Calculate the FMPose difference of the anchor frame with respect to the given frame.
         * @param arFrame the current AR Frame.
         */
        fun anchorDeltaPoseForFrame(arFrame: Frame, anchorFrame: Frame): FMPose {
            // Pose of frame must be taken for "virtual" device as we send to server orientation of
            // "virtual" device for "localization" frame
            val poseARVirtualFrame = arFrame.camera.displayOrientedPose
            val poseAnchor = anchorFrame.androidSensorPose

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

        private fun Bitmap.rotate(degrees: Float): Bitmap {
            val matrix = Matrix().apply { postRotate(degrees) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }

        private fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                Constants.JpegCompressionRatio,
                byteArrayOutputStream
            )
            return byteArrayOutputStream.toByteArray()
        }

        /**
         * Converts Quaternion to Euler Angles
         * Source: https://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/index.htm
         * @param rotationQuaternion: rotation quaternion correspondent to rotation of the device
         * */
        fun convertQuaternionToEuler(rotationQuaternion: FloatArray): FloatArray {
            val qw = rotationQuaternion[3]
            val qx = rotationQuaternion[0]
            val qy = rotationQuaternion[1]
            val qz = rotationQuaternion[2]

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
    }
}