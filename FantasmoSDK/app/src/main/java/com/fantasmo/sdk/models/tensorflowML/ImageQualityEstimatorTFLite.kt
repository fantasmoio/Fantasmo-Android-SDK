package com.fantasmo.sdk.models.tensorflowML

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.fantasmosdk.ml.ImageQualityEstimatorModel
import com.fantasmo.sdk.utilities.YuvToRgbConverter
import com.google.ar.core.Frame
import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

@RequiresApi(Build.VERSION_CODES.KITKAT)
class ImageQualityEstimatorTFLite(val context: Context) : ImageQualityEstimatorProtocol {

    private val TAG = ImageQualityEstimatorTFLite::class.java.simpleName
    private val mlShape = intArrayOf(1, 3, 320, 240)
    private val imageHeight: Int = 320
    private val imageWidth: Int = 240

    private val yuvToRgbConverter = YuvToRgbConverter(context, imageHeight, imageWidth)

    private val imageQualityModel: ImageQualityEstimatorModel by lazy {
        val compatList = CompatibilityList()

        val options = if(compatList.isDelegateSupportedOnThisDevice){
            Log.d(TAG, "This device is GPU Compatible ")
            Model.Options.Builder().setDevice(Model.Device.GPU).build()
        } else {
            Log.d(TAG, "This device is GPU Incompatible ")
            Model.Options.Builder().setNumThreads(4).build()
        }
        ImageQualityEstimatorModel.newInstance(context, options)
    }

    override fun estimateImageQuality(
        frame: Frame
    ): ImageQualityEstimationResult {

        val bitmapRGB = yuvToRgbConverter.toBitmap(frame)
        return if (bitmapRGB == null) {
            ImageQualityEstimationResult.ERROR("null image")
        } else {
            val rgb = getRGBValues(bitmapRGB)
            processImage(rgb)
        }
    }

    // O(3N) complexity
    private fun getRGBValues(bitmap: Bitmap): FloatArray {
        val pixels = IntArray(imageHeight * imageWidth)
        bitmap.getPixels(pixels, 0, imageWidth, 0, 0, imageWidth, imageHeight)

        // Store all the RED color space
        val r = FloatArray(imageHeight * imageWidth) { 0f }
        // Store all the GREEN color space
        val g = FloatArray(imageHeight * imageWidth) { 0f }
        // Store all the BLUE color space
        val b = FloatArray(imageHeight * imageWidth) { 0f }

        //Handle Red Color Space
        for ((index, pixel) in pixels.withIndex()) {
            var red = Color.red(pixel) / 255.0f
            // subtract mean, stddev normalization
            red = (red - 0.485f) / 0.229f
            // add the rgb values to the input array
            r[index] = red
        }
        //Handle Green Color Space
        for ((index, pixel) in pixels.withIndex()) {
            var green = Color.green(pixel) / 255.0f
            green = (green - 0.456f) / 0.224f
            g[index] = green
        }
        //Handle Blue Color Space
        for ((index, pixel) in pixels.withIndex()) {
            var blue = Color.blue(pixel) / 255.0f
            blue = (blue - 0.406f) / 0.225f
            b[index] = blue
        }
        return (r + g + b)
    }

    private fun processImage(rgb: FloatArray): ImageQualityEstimationResult {
        val tfBuffer = TensorBuffer.createFixedSize(mlShape, DataType.FLOAT32)
        tfBuffer.loadArray(rgb)

        val outputs = imageQualityModel.process(tfBuffer)
        val outputFeature = outputs.outputFeature0AsTensorBuffer

        return if (outputFeature.floatArray.size == 2) {
            val y1Exp = outputFeature.floatArray[0]
            val y2Exp = outputFeature.floatArray[1]
            if (!y1Exp.isNaN() && y1Exp.isFinite() && !y2Exp.isNaN() && y2Exp.isFinite()) {
                val score = 1 / (1 + y2Exp / y1Exp)
                ImageQualityEstimationResult.ESTIMATE(score)
            } else {
                ImageQualityEstimationResult.ERROR("Invalid feature values.")
            }
        } else {
            ImageQualityEstimationResult.ERROR("Invalid feature value.")
        }
    }
}