package com.fantasmo.sdk

import android.content.Context
import android.graphics.*
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.Frame
import java.io.ByteArrayOutputStream


/**
 * Class with utility methods and constants
 */
class FMUtility {
    companion object {
        /**
         * Method to get the the AR Frame camera image data.
         * @param arFrame the AR Frame to localize.
         * @return a ByteArray with the data of the [arFrame]
         */
        fun getImageDataFromARFrame(context: Context, arFrame: Frame): ByteArray {
            //The camera image
            val cameraImage = arFrame.acquireCameraImage()

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

            // Release the image
            cameraImage.close()

            val imageBytes: ByteArray = baOutputStream.toByteArray()
            val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                .rotate(getImageRotationDegrees(context))
            val data = getFileDataFromDrawable(imageBitmap)

            imageBitmap.recycle()
            return data
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
                    return 180f
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
                    return 0f
                }
            }
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