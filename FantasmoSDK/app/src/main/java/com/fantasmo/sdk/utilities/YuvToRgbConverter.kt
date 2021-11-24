package com.fantasmo.sdk.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.renderscript.*
import android.util.Log
import com.fantasmo.sdk.FMUtility
import com.google.ar.core.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class YuvToRgbConverter(
    val context: Context,
    private val imageHeight: Int,
    private val imageWidth: Int
) {
    private val TAG = YuvToRgbConverter::class.java.simpleName
    private val rs = RenderScript.create(context)
    private val yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    fun toBitmap(frame: Frame): Bitmap? {

        val frameToByteArray = FMUtility.acquireFrameImage(frame)
        if (frameToByteArray == null) {
            Log.e(TAG, "Null ByteArray")
            return null
        } else {
            val reducedFrame = reduceFrame(frameToByteArray)

            val yuvType = Type.Builder(rs, Element.U8(rs)).setX(reducedFrame.size)
            val inData = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)

            val rgbaType =
                Type.Builder(rs, Element.RGBA_8888(rs)).setX(imageWidth).setY(imageHeight)
            val outData = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)

            inData.copyFrom(frameToByteArray)

            yuvToRgbIntrinsic.setInput(inData)
            yuvToRgbIntrinsic.forEach(outData)

            val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            outData.copyTo(bitmap)
            return bitmap
        }
    }

    /**
     * Convert bitmap to byte array using ByteBuffer.
     */
    private fun reduceFrame(frameToByteArray: ByteArray): ByteArray {

        val originalBitmap =
            BitmapFactory.decodeByteArray(frameToByteArray, 0, frameToByteArray.size)
        val reducedBitmap =
            Bitmap.createScaledBitmap(originalBitmap, imageWidth, imageHeight, true)

        val size: Int = reducedBitmap.rowBytes * reducedBitmap.height
        val byteBuffer = ByteBuffer.allocate(size)
        reducedBitmap.copyPixelsToBuffer(byteBuffer)
        return byteBuffer.array()
    }
}