package com.fantasmo.sdk.utilities

import android.content.Context
import android.graphics.*
import android.media.Image
import android.os.Build
import android.renderscript.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.FMUtility
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.ResourceExhaustedException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.KITKAT)
class YuvToRgbConverter(
    val context: Context,
    private val imageHeight: Int,
    private val imageWidth: Int
) {
    private val TAG = YuvToRgbConverter::class.java.simpleName
    private val rs = RenderScript.create(context)
    private val yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param frame ARFrame from the ARSession
     * @return Bitmap in RGB format
     */
    fun toBitmap(frame: Frame): Bitmap? {
        val frameToByteArray = acquireFrameImageML(frame)

        if (frameToByteArray == null) {
            return null
        } else {
            GlobalScope.launch(defaultDispatcher) {
                val realFrame = FMUtility.acquireFrameImage(frame)
                FMUtility.setFrame(realFrame)
            }
            val yuvType = Type.Builder(rs, Element.U8(rs)).setX(frameToByteArray.size)
            val inData = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)

            val rgbaType =
                Type.Builder(rs, Element.RGBA_8888(rs)).setX(imageWidth).setY(imageHeight)
            val outData = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)

            inData.copyFrom(frameToByteArray)

            yuvToRgbIntrinsic.setInput(inData)
            yuvToRgbIntrinsic.forEach(outData)

            val bitmapLocal = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            outData.copyTo(bitmapLocal)

            inData.destroy()
            outData.destroy()
            return bitmapLocal
        }
    }

    /**
     * Convert ARFrame to ByteArray.
     * @param arFrame Frame from the ARSession
     * @return ByteArray containing the frame information
     */
    private fun acquireFrameImageML(arFrame: Frame): ByteArray? {
        try {
            val cameraImage = arFrame.acquireCameraImage()
            arFrame.acquireCameraImage().close()

            val baOutputStream = createByteArrayOutputStreamML(cameraImage)
            // Release the image
            cameraImage.close()
            return baOutputStream.toByteArray()
        } catch (e: NotYetAvailableException) {
            Log.e(TAG, "FrameNotYetAvailable")
        } catch (e: DeadlineExceededException) {
            Log.e(TAG, "DeadlineExceededException")
        } catch (e: ResourceExhaustedException) {
            Log.e(TAG, "ResourceExhaustedException")
        }
        return null
    }

    /**
     * Creates ByteArrayOutputStream from the Image acquired from the
     * ARFrame and resizes it to the specified model shape.
     * @param cameraImage Image acquired from ARFrame
     * @return ByteArrayOutputStream in YUV format
     */
    private fun createByteArrayOutputStreamML(cameraImage: Image): ByteArrayOutputStream {
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
            imageWidth,
            imageHeight,
            null
        )

        yuvImage.compressToJpeg(
            Rect(0, 0, imageWidth, imageHeight),
            100,
            baOutputStream
        )
        return baOutputStream
    }
}