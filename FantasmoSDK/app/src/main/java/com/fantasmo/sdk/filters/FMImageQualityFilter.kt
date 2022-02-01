package com.fantasmo.sdk.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.models.tensorflowML.ImageQualityModelUpdater
import com.fantasmo.sdk.utilities.YuvToRgbConverter
import com.google.ar.core.Frame
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

@RequiresApi(Build.VERSION_CODES.KITKAT)
class FMImageQualityFilter(imageQualityScoreThreshold: Float, val context: Context) :
    FMFrameFilter {
    private val TAG = FMImageQualityFilter::class.java.simpleName
    private val mlShape = intArrayOf(1, 3, 320, 240)
    private val imageHeight: Int = 320
    private val imageWidth: Int = 240

    val scoreThreshold = imageQualityScoreThreshold
    var lastImageQualityScore = 0f

    private val yuvToRgbConverter = YuvToRgbConverter(context, imageHeight, imageWidth)

    /**
     * ImageQualityEstimatorModel initializer.
     */
    private var imageQualityModelUpdater = ImageQualityModelUpdater(context)
    var modelVersion = imageQualityModelUpdater.modelVersion
    private var imageQualityModel: Interpreter? = null

    override fun accepts(arFrame: Frame): FMFrameFilterResult {
        imageQualityModel = imageQualityModelUpdater.getInterpreter()
        if (imageQualityModel == null) {
            Log.e(TAG, "Failed to get Model")
            return FMFrameFilterResult.Accepted
        }
        val bitmapRGB = yuvToRgbConverter.toBitmap(arFrame)
        if (bitmapRGB == null) {
            // The frame being null means it's no longer available to send in the request
            Log.e(TAG, "Failed to create Input Array")
            return FMFrameFilterResult.Rejected(FMFilterRejectionReason.FRAMEERROR)
        } else {
            val rgb = getRGBValues(bitmapRGB)
            val iqeResult = processImage(rgb)
            if (iqeResult != null) {
                lastImageQualityScore = iqeResult
                Log.d(TAG, "IQE: $iqeResult")
                return if (iqeResult >= scoreThreshold) {
                    FMFrameFilterResult.Accepted
                } else {
                    FMFrameFilterResult.Rejected(FMFilterRejectionReason.IMAGEQUALITYSCOREBELOWTHRESHOLD)
                }
            }
        }
        return FMFrameFilterResult.Accepted
    }

    /**
     * Gathers all the RGB values and stores them in an
     * FloatArray dividing by three main channels
     * @param bitmap Bitmap image already converted from YUV to RGB
     * @return FloatArray containing RGB values in float precision
     */
    private fun getRGBValues(bitmap: Bitmap): FloatArray {
        // Image Height * Image Width * 3 RGB Channels
        val rgb = FloatArray(imageHeight * imageWidth * 3) { 0f }
        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val pixel = bitmap.getPixel(x, y)
                // Get and convert rgb values to 0.0 - 1.0
                var r = Color.red(pixel) / 255.0f
                var g = Color.green(pixel) / 255.0f
                var b = Color.blue(pixel) / 255.0f

                // subtract mean, stddev normalization
                r = (r - 0.485f) / 0.229f
                g = (g - 0.456f) / 0.224f
                b = (b - 0.406f) / 0.225f

                rgb[x + imageWidth * y] = r
                rgb[imageHeight * imageWidth + imageWidth * y + x] = g
                rgb[ 2 * imageHeight * imageWidth + imageWidth * y + x] = b
            }
        }
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
            val y1Exp = tfBufferOut.floatArray[0]
            val y2Exp = tfBufferOut.floatArray[1]
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