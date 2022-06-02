package com.fantasmo.sdk.utilities

import android.content.Context
import android.graphics.*
import android.os.Build
import android.renderscript.*
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.ScriptC_bicubic_resize

@RequiresApi(Build.VERSION_CODES.KITKAT)
internal class YuvToRgbConverter(
    val context: Context
) {
    private val TAG = YuvToRgbConverter::class.java.simpleName
    private lateinit var rs : RenderScript
    private lateinit var yuvToRgbIntrinsic : ScriptIntrinsicYuvToRGB
    @RequiresApi(Build.VERSION_CODES.N)
    private lateinit var resizeScript : ScriptC_bicubic_resize
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    private lateinit var resizeIntrinsic : ScriptIntrinsicResize

    /**
     * Converts ARFrame to bitmap format.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param yuvImage YUV Image from the ARSession
     * @return Bitmap in RGB format
     */
    fun toBitmap(yuvImage: YuvImage): Bitmap {
        if(!::rs.isInitialized)
            initRenderScript()

        val output = Bitmap.createBitmap(yuvImage.width, yuvImage.height, Bitmap.Config.ARGB_8888)

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
     * @param yuvImage YUV Image from the ARSession
     * @param imageWidth output image width
     * @param imageHeight output image height
     * @return Bitmap in RGB format
     */
    fun toBitmap(yuvImage: YuvImage, imageWidth: Int, imageHeight: Int): Bitmap {
        if(!::rs.isInitialized)
            initRenderScript()

        val output = Bitmap.createBitmap(yuvImage.width, yuvImage.height, Bitmap.Config.ARGB_8888)

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
     * @param yuvImage YUV ImageFrame from the ARSession
     * @param imageWidth output image width
     * @param imageHeight output image height
     * @return Byte Array with RGB bitmap data
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun toByteArray(yuvImage: YuvImage, imageWidth: Int, imageHeight: Int): ByteArray {
        if(!::rs.isInitialized)
            initRenderScript()

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
        val outputArray = ByteArray(imageWidth * imageHeight * 4)

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvImage.yuvData)
        yuvToRgbIntrinsic.forEach(outputAllocation)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resizeScript._inputImage = outputAllocation
            resizeScript.invoke_resize(resizedOutputAllocation)
        } else {
            resizeIntrinsic.setInput(outputAllocation)
            resizeIntrinsic.forEach_bicubic(resizedOutputAllocation)
        }

        resizedOutputAllocation.copyTo(outputArray)
        inputAllocation.destroy()
        outputAllocation.destroy()
        resizedOutputAllocation.destroy()
        return outputArray
    }

    /**
     * Converts ARFrame to a TFLite Tensor.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model, and the resizing.
     * Uses RenderScript to make the conversion.
     * @param yuvImage YUV ImageFrame from the ARSession
     * @param imageWidth output image width
     * @param imageHeight output image height
     * @return Tensor to feed TFLite
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun toTensor(yuvImage: YuvImage, imageWidth: Int, imageHeight: Int): FloatArray {
        if(!::rs.isInitialized)
            initRenderScript()

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
     * Converts Bitmap to a resized ByteArray.
     * It's also responsible for the YUV to RGB conversion needed to give as input to the model.
     * Uses RenderScript to make the conversion.
     * @param bitmap input bitmap
     * @param imageWidth output image width
     * @param imageHeight output image height
     * @return Bitmap in RGB format
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun toByteArray(bitmap: Bitmap, imageWidth: Int, imageHeight: Int): ByteArray {
        if(!::rs.isInitialized)
            initRenderScript()

        val builder = Type.Builder(rs, Element.RGBA_8888(rs))
        builder.setX(bitmap.width)
        builder.setY(bitmap.height)

        val inputAllocation = Allocation.createTyped(rs, builder.create())

        builder.setX(imageWidth)
        builder.setY(imageHeight)
        val resizedOutputAllocation = Allocation.createTyped(rs, builder.create())

        inputAllocation.copyFrom(bitmap)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resizeScript._inputImage = inputAllocation
            resizeScript.invoke_resize(resizedOutputAllocation)
        } else {
            resizeIntrinsic.setInput(inputAllocation)
            resizeIntrinsic.forEach_bicubic(resizedOutputAllocation)
        }

        val outputArray = ByteArray(imageWidth * imageHeight * 4)
        resizedOutputAllocation.copyTo(outputArray)
        val outputBmp = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)

        resizedOutputAllocation.copyTo(outputBmp)

        inputAllocation.destroy()
        resizedOutputAllocation.destroy()
        return outputArray
    }

    private fun initRenderScript() {
        rs = RenderScript.create(context)
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            resizeIntrinsic = ScriptIntrinsicResize.create(rs)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            resizeScript = ScriptC_bicubic_resize(rs)
    }
}