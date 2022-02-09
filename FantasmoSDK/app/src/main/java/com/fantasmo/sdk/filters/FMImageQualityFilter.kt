package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.tensorflowML.ImageQualityModelUpdater
import com.fantasmo.sdk.utilities.YuvToRgbConverter
import com.google.ar.core.Frame
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
class FMImageQualityFilter(imageQualityScoreThreshold: Float, val context: Context) :
    FMFrameFilter {
    override val TAG = FMImageQualityFilter::class.java.simpleName
    private val mlShape = intArrayOf(1, 3, 320, 240)
    private val imageHeight: Int = 320
    private val imageWidth: Int = 240

    val scoreThreshold = imageQualityScoreThreshold
    var lastImageQualityScore = 0f

    private val yuvToRgbConverter = YuvToRgbConverter(context)

    /**
     * ImageQualityEstimatorModel initializer.
     */
    private var imageQualityModelUpdater = ImageQualityModelUpdater(context)
    var modelVersion = imageQualityModelUpdater.modelVersion
    private var imageQualityModel: Interpreter? = null

    override fun accepts(fmFrame: FMFrame): FMFrameFilterResult {
        var before = SystemClock.elapsedRealtimeNanos()

        imageQualityModel = imageQualityModelUpdater.getInterpreter()

        var after = SystemClock.elapsedRealtimeNanos()
        var interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Getting interpreter took ${interval}ms")
        before = after

        if (imageQualityModel == null) {
            Log.e(TAG, "Failed to get Model")
            return FMFrameFilterResult.Accepted
        }

        after = SystemClock.elapsedRealtimeNanos()
        interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Acquiring frame image took ${interval}ms")
        before = after

        val rgbByteArray =  yuvToRgbConverter.toByteArray(fmFrame.yuvImage, imageWidth, imageHeight)
        after = SystemClock.elapsedRealtimeNanos()
        interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "YUV to RGB took ${interval}ms")
        before = after

        val rgbImage = getRGBValues(rgbByteArray)
        after = SystemClock.elapsedRealtimeNanos()
        interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Getting RGB values took ${interval}ms")
        before = after

        val result = processImage(rgbImage)
        after = SystemClock.elapsedRealtimeNanos()
        interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Processing the image took ${interval}ms")
        before = after

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
            /**
     * Gathers all the RGB values and stores them in an
     * FloatArray dividing by three main channels
     * @param bitmap Bitmap image already converted from YUV to RGB
     * @return FloatArray containing RGB values in float precision
     */
    private fun getRGBValues(inArray: ByteArray): FloatArray {
        // Image Height * Image Width * 3 RGB Channels
        val rgb = FloatArray(imageHeight * imageWidth * 3)
        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val pixel = inArray[x + y * imageWidth]
                // Get and convert rgb values to 0.0 - 1.0
                var r = inArray[(x + y * imageWidth) * 4] / 255.0f
                var g = inArray[(x + y * imageWidth) * 4 + 1] / 255.0f
                var b = inArray[(x + y * imageWidth) * 4 + 2] / 255.0f

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
        var before = SystemClock.elapsedRealtimeNanos()

        val tfBuffer = TensorBuffer.createFixedSize(mlShape, DataType.FLOAT32)

        var after = SystemClock.elapsedRealtimeNanos()
        var interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Creating tensor buffer took ${interval}ms")
        before = after

        tfBuffer.loadArray(rgb)

        after = SystemClock.elapsedRealtimeNanos()
        interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Loading RBG array took ${interval}ms")
        before = after

        val tfBufferOut = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)

        after = SystemClock.elapsedRealtimeNanos()
        interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Creating out buffer took ${interval}ms")
        before = after

        tfBufferOut.loadArray(floatArrayOf(0f, 0f))

        after = SystemClock.elapsedRealtimeNanos()
        interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Loading out buffer took ${interval}ms")
        before = after

        imageQualityModel!!.run(tfBuffer.buffer, tfBufferOut.buffer)
        after = SystemClock.elapsedRealtimeNanos()
        interval = ((after - before) / 1000L).toFloat() / 1000f
        Log.d(TAG, "Running image quality model took ${interval}ms")
        before = after

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