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
    val context: Context
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
    fun toBitmap(yuvImage: YuvImage): Bitmap? {
        val output = Bitmap.createBitmap(yuvImage.width, yuvImage.width, Bitmap.Config.ARGB_8888)

        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized) {
            // Explicitly create an element with type NV21, since that's the pixel format we use
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvImage.yuvData.size)
            yuvToRgbIntrinsic.setInput(inputAllocation)
        }
        if (!::outputAllocation.isInitialized) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        outputAllocation.copyTo(output)

        return output
    }

    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param frame ARFrame from the ARSession
     * @return Bitmap in RGB format
     */
    fun toBitmap(yuvImage: YuvImage, imageWidth: Int, imageHeight: Int): Bitmap? {
        val output = Bitmap.createBitmap(yuvImage.width, yuvImage.height, Bitmap.Config.ARGB_8888)

        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized) {
            // Explicitly create an element with type NV21, since that's the pixel format we use
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvImage.yuvData.size)
            yuvToRgbIntrinsic.setInput(inputAllocation)
        }
        if (!::outputAllocation.isInitialized) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        outputAllocation.copyTo(output)

        return Bitmap.createScaledBitmap(output, imageWidth, imageHeight, true)
    }
}