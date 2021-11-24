package com.fantasmo.sdk.models.tensorflowML

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.fantasmosdk.ml.ImageQualityEstimatorModel
import com.fantasmo.sdk.utilities.YuvToRgbConverter
import com.google.ar.core.Frame
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

@RequiresApi(Build.VERSION_CODES.KITKAT)
class ImageQualityEstimatorTFLite(val context: Context) : ImageQualityEstimatorProtocol {

    private val TAG = ImageQualityEstimatorTFLite::class.java.simpleName
    private val mlShape = intArrayOf(1, 3, 320, 240)
    private val imageHeight: Int = 320
    private val imageWidth: Int = 240

    private val yuvToRgbConverter = YuvToRgbConverter(context, imageHeight, imageWidth)
    private lateinit var imageQualityModel: ImageQualityEstimatorModel

    init {
        try {
            Log.d(TAG, "Entered ImageEstimateImageQuality")
            imageQualityModel = ImageQualityEstimatorModel.newInstance(context)
        } catch (e: Exception) {
            Log.e(TAG, e.stackTrace.toString())
        }
    }

    override fun estimateImageQuality(frame: Frame): ImageQualityEstimationResult {
        return ImageQualityEstimationResult.UNKNOWN
    }

    override fun estimateImageQuality(
        frame: Frame,
        callback: (ImageQualityEstimationResult) -> Unit
    ) {
        // Image Height * Image Width * 3 RGB Channels
        val rgb = FloatArray(imageHeight * imageWidth * 3) { 0f }

        val bitmapRGB = yuvToRgbConverter.toBitmap(frame)

        if (bitmapRGB == null) {
            callback(ImageQualityEstimationResult.ERROR("null image"))
            return
        }
        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val pixel = bitmapRGB.getPixel(x, y)
                // Get and convert rgb values to 0.0 - 1.0
                var r = Color.red(pixel) / 255.0f
                var g = Color.green(pixel) / 255.0f
                var b = Color.blue(pixel) / 255.0f

                // subtract mean, stddev normalization
                r = (r - 0.485f) / 0.229f
                g = (g - 0.456f) / 0.224f
                b = (b - 0.406f) / 0.225f

                // rotate 90 degrees clockwise
                val w = imageHeight - 1 - y
                val h = x

                // add the rgb values to the input array
                val rIndex =
                    0 * imageHeight * imageWidth + h * imageHeight + w
                val gIndex =
                    1 * imageHeight * imageWidth + h * imageHeight + w
                val bIndex =
                    2 * imageHeight * imageWidth + h * imageHeight + w

                rgb[rIndex] = r
                rgb[gIndex] = g
                rgb[bIndex] = b
            }
        }
        val tfBuffer = TensorBuffer.createFixedSize(mlShape, DataType.FLOAT32)
        tfBuffer.loadArray(rgb)

        val outputs2 = imageQualityModel.process(tfBuffer)
        val outputFeature = outputs2.outputFeature0AsTensorBuffer
        if(outputFeature.floatArray.size == 2){
            Log.d(TAG, outputFeature.floatArray.contentToString())
            val y1Exp = outputFeature.floatArray[0]
            val y2Exp = outputFeature.floatArray[1]
            val score = 1/(1 + y2Exp/y1Exp)
            callback(ImageQualityEstimationResult.ESTIMATE(score))
        } else{
            callback(ImageQualityEstimationResult.UNKNOWN)
        }

        return

    }
}