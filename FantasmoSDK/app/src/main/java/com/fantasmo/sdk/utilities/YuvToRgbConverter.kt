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
import android.renderscript.Allocation

import android.renderscript.RenderScript


@RequiresApi(Build.VERSION_CODES.KITKAT)
class YuvToRgbConverter(
    val context: Context,
    private val imageHeight: Int,
    private val imageWidth: Int
) {
    private val TAG = YuvToRgbConverter::class.java.simpleName
    private val rs = RenderScript.create(context)
    private val yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private lateinit var inputAllocation: Allocation
    private lateinit var outputAllocation: Allocation

    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default


    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param frame ARFrame from the ARSession
     * @return Bitmap in RGB format
     */
    fun toBitmap(frame: Frame): Bitmap? {
        val width = frame.camera.imageIntrinsics.imageDimensions[0]
        val height = frame.camera.imageIntrinsics.imageDimensions[1]
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val image = acquireFrameImage(frame) ?: return null

        val cameraPlaneY = image.planes[0].buffer
        val cameraPlaneV = image.planes[1].buffer
        val cameraPlaneU = image.planes[2].buffer

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

        val yuvImage = YuvImage(
            compositeByteArray,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        GlobalScope.launch(defaultDispatcher) {
            FMUtility.setFrameQualityTest(image)
            image.close()
        }
        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized) {
            // Explicitly create an element with type NV21, since that's the pixel format we use
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvImage.yuvData.size)
        }
        if (!::outputAllocation.isInitialized) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.setInput(inputAllocation)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        outputAllocation.copyTo(output)

        return Bitmap.createScaledBitmap(output, imageWidth, imageHeight, true)
    }

    /**
     * Acquires Image from the ARFrame.
     * @param arFrame Frame from the ARSession
     * @return ByteArray containing the frame information
     */
    private fun acquireFrameImage(arFrame: Frame): Image? {
        try {
            return arFrame.acquireCameraImage()
        } catch (e: NotYetAvailableException) {
            Log.e(TAG, "FrameNotYetAvailable")
        } catch (e: DeadlineExceededException) {
            Log.e(TAG, "DeadlineExceededException")
        } catch (e: ResourceExhaustedException) {
            Log.e(TAG, "ResourceExhaustedException")
        }
        return null
    }
}