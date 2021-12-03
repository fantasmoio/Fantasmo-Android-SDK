package com.fantasmo.sdk.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.os.Build
import android.renderscript.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.fantasmosdk.BuildConfig
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.ResourceExhaustedException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

    private var pixelCount: Int = -1
    private lateinit var yuvBuffer: ByteBuffer
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
    @Synchronized
    fun toBitmap(frame: Frame): Bitmap? {
        val output = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        val image = acquireFrameImage(frame) ?: return null

        // Ensure that the intermediate output byte buffer is allocated
        if (!::yuvBuffer.isInitialized) {
            pixelCount = image.cropRect.width() * image.cropRect.height()
            // Bits per pixel is an average for the whole image, so it's useful to compute the size
            // of the full buffer but should not be used to determine pixel offsets
            val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
            yuvBuffer = ByteBuffer.allocateDirect(pixelCount * pixelSizeBits / 8)
        }

        // Rewind the buffer; no need to clear it since it will be filled
        yuvBuffer.rewind()

        // Get the YUV data in byte array form using NV21 format
        imageToByteBuffer(image, yuvBuffer.array())
        GlobalScope.launch(defaultDispatcher) {
            FMUtility.setFrameQualityTest(image)
            image.close()
        }
        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized) {
            // Explicitly create an element with type NV21, since that's the pixel format we use
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvBuffer.array().size)
        }
        if (!::outputAllocation.isInitialized) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvBuffer.array())
        yuvToRgbIntrinsic.setInput(inputAllocation)
        yuvToRgbIntrinsic.forEach(outputAllocation)
        outputAllocation.copyTo(output)
        return output
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

    /**
     * Creates ByteBuffer from the Image acquired from the
     * ARFrame and resizes it to the specified model shape.
     * @param image Image acquired from ARFrame
     * @param outputBuffer Buffer to be filled with the image
     */
    private fun imageToByteBuffer(image: Image, outputBuffer: ByteArray) {

        if (BuildConfig.DEBUG && image.format != ImageFormat.YUV_420_888) {
            error("Assertion failed")
        }

        val imageCrop = image.cropRect
        val imagePlanes = image.planes

        imagePlanes.forEachIndexed { planeIndex, plane ->
            // How many values are read in input for each output value written
            // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
            //
            // Y Plane            U Plane    V Plane
            // ===============    =======    =======
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            val outputStride: Int

            // The index in the output buffer the next value will be written at
            // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
            //
            // First chunk        Second chunk
            // ===============    ===============
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            var outputOffset: Int

            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    // For NV21 format, U is in odd-numbered indices
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    // For NV21 format, V is in even-numbered indices
                    outputOffset = pixelCount
                }
                else -> {
                    // Image contains more than 3 planes, something strange is going on
                    return@forEachIndexed
                }
            }

            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // We have to divide the width and height by two if it's not the Y plane
            val planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                )
            }

            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()

            // Intermediate buffer used to store the bytes of each row
            val rowBuffer = ByteArray(plane.rowStride)

            // Size of each row in bytes
            val rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                // Take into account that the stride may include data from pixels other than this
                // particular plane and row, and that could be between pixels and not after every
                // pixel:
                //
                // |---- Pixel stride ----|                    Row ends here --> |
                // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                //
                // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                (planeWidth - 1) * pixelStride + 1
            }

            for (row in 0 until planeHeight) {
                // Move buffer position to the beginning of this row
                planeBuffer.position(
                    (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
                )

                if (pixelStride == 1 && outputStride == 1) {
                    // When there is a single stride value for pixel and output, we can just copy
                    // the entire row in a single step
                    planeBuffer.get(outputBuffer, outputOffset, rowLength)
                    outputOffset += rowLength
                } else {
                    // When either pixel or output have a stride > 1 we must copy pixel by pixel
                    planeBuffer.get(rowBuffer, 0, rowLength)
                    for (col in 0 until planeWidth) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
    }
}