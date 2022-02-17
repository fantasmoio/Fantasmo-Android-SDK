package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.renderscript.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.tensorflowML.ImageQualityModelUpdater
import com.fantasmo.sdk.utilities.YuvToRgbConverter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.exp

@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
class FMImageQualityFilter(imageQualityScoreThreshold: Float, val context: Context) :
    FMFrameFilter {
    override val TAG = FMImageQualityFilter::class.java.simpleName
    private val mlShape = intArrayOf(1, 3, 320, 240)
    private val imageHeight: Int = 320
    private val imageWidth: Int = 240

    val scoreThreshold = imageQualityScoreThreshold
    var lastImageQualityScore = 0f
    private lateinit var rs : RenderScript

    private val yuvToRgbConverter = YuvToRgbConverter(context)

    /**
     * ImageQualityEstimatorModel initializer.
     */
    private var imageQualityModelUpdater = ImageQualityModelUpdater(context)
    var modelVersion = imageQualityModelUpdater.modelVersion
    private var imageQualityModel: Interpreter? = null
    private lateinit var colorMatrixIntrinsic: ScriptIntrinsicColorMatrix
    private lateinit var scalingMatrix: Matrix3f

    override fun accepts(fmFrame: FMFrame): FMFrameFilterResult {

        imageQualityModel = imageQualityModelUpdater.getInterpreter()
        if(!::rs.isInitialized) {
            rs = RenderScript.create(context)
            colorMatrixIntrinsic = ScriptIntrinsicColorMatrix.create(rs)
            scalingMatrix = Matrix3f()
            scalingMatrix.scale(1 / 0.229f, 1 / 0.224f, 1 / 0.225f)
            colorMatrixIntrinsic.setColorMatrix(scalingMatrix)
            colorMatrixIntrinsic.setAdd(-0.485f / 0.229f, -0.456f / 0.224f, -0.406f / 0.225f, 0f)
        }

        if (imageQualityModel == null) {
            Log.e(TAG, "Failed to get Model")
            return FMFrameFilterResult.Accepted
        }

        val yuvImage = fmFrame.yuvImage
        if (yuvImage == null) {
            // The frame being null means it's no longer available to send in the request
            Log.e(TAG, "Failed to create Input Array")
            return FMFrameFilterResult.Rejected(FMFilterRejectionReason.FRAMEERROR)
        } else {
            val rgbByteArray =
                yuvToRgbConverter.toByteArray(yuvImage, imageWidth, imageHeight)

            val rgbImage = getRGBValues(rgbByteArray)
            val result = processImage(rgbImage)

            if (result != null) {
                lastImageQualityScore = result
                Log.d(TAG, "IQE: $result")
                return if (result >= scoreThreshold) {
                    FMFrameFilterResult.Accepted
                } else {
                    FMFrameFilterResult.Rejected(FMFilterRejectionReason.IMAGEQUALITYSCOREBELOWTHRESHOLD)
                }
            }
            return FMFrameFilterResult.Accepted
        }
    }
            /**
     * Gathers all the RGB values and stores them in an
     * FloatArray dividing by three main channels
     * @param bitmap Bitmap image already converted from YUV to RGB
     * @return FloatArray containing RGB values in float precision
     */
    private fun getRGBValues(inArray: ByteArray): FloatArray {
        val inputAllocation = Allocation.createSized(rs, Element.RGBA_8888(rs), imageHeight * imageWidth)
        inputAllocation.copyFrom(inArray)
        val outputAllocation = Allocation.createSized(rs, Element.F32_3(rs), imageHeight * imageWidth)
        colorMatrixIntrinsic.forEach(inputAllocation, outputAllocation)
        val rgbMixed = FloatArray(imageHeight * imageWidth * 4)
        outputAllocation.copyTo(rgbMixed)
        // Image Height * Image Width * 3 RGB Channels
        val rgb = FloatArray(imageHeight * imageWidth * 3)
        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                // Get and convert rgb values to 0.0 - 1.0
                val r = rgbMixed[(x + y * imageWidth) * 4]
                val g = rgbMixed[(x + y * imageWidth) * 4 + 1]
                val b = rgbMixed[(x + y * imageWidth) * 4 + 2]

                rgb[x + imageWidth * y] = r
                rgb[imageHeight * imageWidth + imageWidth * y + x] = g
                rgb[ 2 * imageHeight * imageWidth + imageWidth * y + x] = b
            }
        }
        inputAllocation.destroy()
        outputAllocation.destroy()
        return rgb
    }

    /**
     * Uses the RGB values floatArray and passes it as input for the TensorFlowLite model
     * @param rgb FloatArray containing RGB values
     * @return Float result of the model inference
     */
    private fun processImage(rgb: FloatArray): Float? {

        val tfBuffer = TensorBuffer.createFixedSize(mlShape, DataType.FLOAT32)
        tfBuffer.loadArray(rgb)

        val tfBufferOut = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)
        tfBufferOut.loadArray(floatArrayOf(0f, 0f))

        imageQualityModel!!.run(tfBuffer.buffer, tfBufferOut.buffer)

        return if (tfBufferOut.floatArray.size == 2) {
            val y1Exp = exp(tfBufferOut.floatArray[0])
            val y2Exp = exp(tfBufferOut.floatArray[1])
            if (!y1Exp.isNaN() && y1Exp.isFinite() && !y2Exp.isNaN() && y2Exp.isFinite()) {
                val score = 1 / (1 + y2Exp / y1Exp)
                score
            } else {
                null
            }
        } else {
            null
        }
    }
}