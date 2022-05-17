package com.fantasmo.sdk.utilities

import android.content.Context
import android.graphics.*
import android.os.Build
import android.renderscript.*
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.ScriptC_bicubic_resize

@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
class YuvToRgbConverter(
    val context: Context
) {
    private val TAG = YuvToRgbConverter::class.java.simpleName
    private lateinit var rs : RenderScript
    private lateinit var yuvToRgbIntrinsic : ScriptIntrinsicYuvToRGB
    private lateinit var resizeScript : ScriptC_bicubic_resize

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
            resizeScript = ScriptC_bicubic_resize(rs)
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
            resizeScript = ScriptC_bicubic_resize(rs)
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
            resizeScript = ScriptC_bicubic_resize(rs)
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
        resizeScript._inputImage = outputAllocation

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        resizeScript.invoke_resize(resizedOutputAllocation)
        val outputArray = ByteArray(imageWidth * imageHeight * 4)
        resizedOutputAllocation.copyTo(outputArray)
        inputAllocation.destroy()
        outputAllocation.destroy()
        resizedOutputAllocation.destroy()
        return outputArray
    }

    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param frame ARFrame from the ARSession
     * @return Bitmap in RGB format
     */
    fun toTensor(yuvImage: YuvImage, imageWidth: Int, imageHeight: Int): FloatArray {

        if (!::rs.isInitialized) {
            rs = RenderScript.create(context)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
            resizeScript = ScriptC_bicubic_resize(rs)
        }
        val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
        val inputAllocation = Allocation.createSized(rs, elemType.element, yuvImage.yuvData.size)
        yuvToRgbIntrinsic.setInput(inputAllocation)
        var builder = Type.Builder(rs, Element.RGBA_8888(rs))
        builder.setX(yuvImage.width)
        builder.setY(yuvImage.height)
        val outputAllocation = Allocation.createTyped(rs, builder.create())

        builder = Type.Builder(rs, Element.F32(rs))
        builder.setX(imageWidth)
        builder.setY(imageHeight * 3)
        val tensorAllocation = Allocation.createTyped(rs, builder.create())
        resizeScript._inputImage = outputAllocation

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        resizeScript.invoke_make_tf_tensor(tensorAllocation)
        val outputArray = FloatArray(imageWidth * imageHeight * 3)
        tensorAllocation.copyTo(outputArray)
        inputAllocation.destroy()
        outputAllocation.destroy()
        tensorAllocation.destroy()
        return outputArray
    }

    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param frame ARFrame from the ARSession
     * @return Bitmap in RGB format
     */
    fun toByteArray(bitmap: Bitmap, imageWidth: Int, imageHeight: Int): ByteArray {

        if (!::rs.isInitialized) {
            rs = RenderScript.create(context)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
            resizeScript = ScriptC_bicubic_resize(rs)
        }

        val builder = Type.Builder(rs, Element.RGBA_8888(rs))
        builder.setX(bitmap.width)
        builder.setY(bitmap.height)

        val inputAllocation = Allocation.createTyped(rs, builder.create())

        builder.setX(imageWidth)
        builder.setY(imageHeight)
        val resizedOutputAllocation = Allocation.createTyped(rs, builder.create())
        resizeScript._inputImage = inputAllocation

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(bitmap)
        resizeScript.invoke_resize(resizedOutputAllocation)

        val outputArray = ByteArray(imageWidth * imageHeight * 4)
        resizedOutputAllocation.copyTo(outputArray)
        val outputBmp = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)

        resizedOutputAllocation.copyTo(outputBmp)

        inputAllocation.destroy()
        resizedOutputAllocation.destroy()
        return outputArray
    }

}