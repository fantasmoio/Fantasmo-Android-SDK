package com.fantasmo.sdk.models
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.YuvImage
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.utilities.YuvToRgbConverter
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.ResourceExhaustedException
import java.io.ByteArrayOutputStream

class FMFrame (private val frame: Frame,
               private val context: Context)
{
    private val TAG = FMFrame::class.java.simpleName
    val camera: Camera = frame.camera
    val cameraPose = camera.pose
    val timestamp = frame.timestamp
    private var _yuvImage: YuvImage? = null
    var enhancedImageGamma: Float? = null

    var yuvImage: YuvImage?
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        get() {
            if(_yuvImage == null) {
                setYuvImageFromFrame()
            }
            return _yuvImage
        }
    set(value) {_yuvImage = value}

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    private var yuvToRgbConverter = YuvToRgbConverter(context)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setYuvImageFromFrame() {
        try {
            val cameraImage = frame.acquireCameraImage()
            val cameraPlaneY = cameraImage.planes[0].buffer
            val cameraPlaneU = cameraImage.planes[2].buffer
            val cameraPlaneV = cameraImage.planes[1].buffer
            //Use the buffers to create a new byteArray
            val compositeByteArray =
                ByteArray(cameraPlaneY.capacity() + cameraPlaneU.capacity() + cameraPlaneV.capacity())
            cameraPlaneY.get(compositeByteArray, 0, cameraPlaneY.capacity())
            cameraPlaneU.get(compositeByteArray, cameraPlaneY.capacity(), cameraPlaneU.capacity())
            cameraPlaneV.get(
                compositeByteArray,
                cameraPlaneY.capacity() + cameraPlaneU.capacity(),
                cameraPlaneV.capacity()
            )
            _yuvImage = YuvImage(compositeByteArray,
                ImageFormat.NV21,
                cameraImage.width,
                cameraImage.height,
                null
            )
            // Release the image
            cameraImage.close()
        }
        catch (e: NotYetAvailableException) {
            Log.e(TAG, "FrameNotYetAvailable")
        } catch (e: DeadlineExceededException) {
            Log.e(TAG, "DeadlineExceededException in acquireFrameImage")
        } catch (e: ResourceExhaustedException) {
            Log.e(TAG, "ResourceExhaustedException")
        }
    }

    fun imageData(): ByteArray? {
        val image = yuvImage ?: return null
        val imageBitmap = yuvToRgbConverter.toBitmap(image)
        imageBitmap.rotate(getImageRotationDegrees(context))
        val data = getFileDataFromDrawable(imageBitmap)

        imageBitmap.recycle()
        return data
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
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

    private fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            FMUtility.Constants.JpegCompressionRatio,
            byteArrayOutputStream
        )
        return byteArrayOutputStream.toByteArray()
    }
}