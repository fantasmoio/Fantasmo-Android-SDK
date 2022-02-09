package com.fantasmo.sdk.models
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.FMUtility
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.ResourceExhaustedException

class FMFrame (private val frame: Frame){
    private val TAG = FMFrame::class.java.simpleName
    val camera: Camera = frame.camera
    val androidSensorPose: Pose = frame.androidSensorPose
    private var _yuvImage: YuvImage? = null
    val yuvImage: YuvImage
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        get() {
                if(_yuvImage == null) {
                    setYuvImageFromFrame()
            }
            return _yuvImage ?: throw AssertionError("Set to null by another thread")
        }
    var processedYuvImage: YuvImage? = null


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setYuvImageFromFrame() {
        try {
            val cameraImage = frame.acquireCameraImage()
            val cameraPlaneY = cameraImage.planes[0].buffer
            val cameraPlaneU = cameraImage.planes[1].buffer
            val cameraPlaneV = cameraImage.planes[2].buffer
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
        } catch (e: NotYetAvailableException) {
            Log.e(TAG, "FrameNotYetAvailable")
        } catch (e: DeadlineExceededException) {
            Log.e(TAG, "DeadlineExceededException in acquireFrameImage")
        } catch (e: ResourceExhaustedException) {
            Log.e(TAG, "ResourceExhaustedException")
        }
    }
}