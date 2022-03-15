package com.fantasmo.sdk.utilities

import android.content.Context
import android.graphics.*
import android.os.Build
import android.renderscript.*
import androidx.annotation.RequiresApi
import android.renderscript.Allocation
import android.renderscript.RenderScript


@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
class YuvToRgbConverter(
    val context: Context
) {
    private val TAG = YuvToRgbConverter::class.java.simpleName
    private lateinit var rs : RenderScript
    private lateinit var yuvToRgbIntrinsic : ScriptIntrinsicYuvToRGB
    private lateinit var resizeIntrinsic : ScriptIntrinsicResize

    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param frame ARFrame from the ARSession
     * @return Bitmap in RGB format
     */
    fun toBitmap(yuvImage: YuvImage): Bitmap {
        val output = Bitmap.createBitmap(yuvImage.width, yuvImage.height, Bitmap.Config.ARGB_8888)

        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::rs.isInitialized) {
            rs = RenderScript.create(context)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
            resizeIntrinsic = ScriptIntrinsicResize.create(rs)
        }
        // Explicitly create an element with type NV21, since that's the pixel format we use
        val builder = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21)
        builder.setX(yuvImage.width)
        builder.setY(yuvImage.height)
        val inputAllocation = Allocation.createTyped(rs, builder.create())
        yuvToRgbIntrinsic.setInput(inputAllocation)
        val outputAllocation = Allocation.createFromBitmap(rs, output)
        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        outputAllocation.copyTo(output)

        inputAllocation.destroy()
        outputAllocation.destroy()

        return output
    }

    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param frame ARFrame from the ARSession
     * @return Bitmap in RGB format
     */
    fun toBitmap(yuvImage: YuvImage, imageWidth: Int, imageHeight: Int): Bitmap {
        val output = Bitmap.createBitmap(yuvImage.width, yuvImage.height, Bitmap.Config.ARGB_8888)

        if (!::rs.isInitialized) {
            rs = RenderScript.create(context)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
            resizeIntrinsic = ScriptIntrinsicResize.create(rs)
        }

        // Explicitly create an element with type NV21, since that's the pixel format we use
        val builder = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21)
        builder.setX(yuvImage.width)
        builder.setY(yuvImage.height)
        val inputAllocation = Allocation.createTyped(rs, builder.create())
        yuvToRgbIntrinsic.setInput(inputAllocation)
        val outputAllocation = Allocation.createFromBitmap(rs, output)
        resizeIntrinsic.setInput(outputAllocation)

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        outputAllocation.copyTo(output)

        inputAllocation.destroy()
        outputAllocation.destroy()

        return Bitmap.createScaledBitmap(output, imageWidth, imageHeight, true)
    }

    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param frame ARFrame from the ARSession
     * @return Bitmap in RGB format
     */
    fun toByteArray(yuvImage: YuvImage, imageWidth: Int, imageHeight: Int): ByteArray {

        if (!::rs.isInitialized) {
            rs = RenderScript.create(context)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
            resizeIntrinsic = ScriptIntrinsicResize.create(rs)
        }
        val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
        val inputAllocation = Allocation.createSized(rs, elemType.element, yuvImage.yuvData.size)
        yuvToRgbIntrinsic.setInput(inputAllocation)
        val builder = Type.Builder(rs, Element.RGBA_8888(rs))
        builder.setX(yuvImage.width)
        builder.setY(yuvImage.height)
        val outputAllocation = Allocation.createTyped(rs, builder.create())

        builder.setX(imageWidth)
        builder.setY(imageHeight)
        val resizedOutputAllocation = Allocation.createTyped(rs, builder.create())
        resizeIntrinsic.setInput(outputAllocation)

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        resizeIntrinsic.forEach_bicubic(resizedOutputAllocation)
        val outputArray = ByteArray(imageWidth * imageHeight * 4)
        resizedOutputAllocation.copyTo(outputArray)
        inputAllocation.destroy()
        outputAllocation.destroy()
        resizedOutputAllocation.destroy()
        return outputArray
    }
}