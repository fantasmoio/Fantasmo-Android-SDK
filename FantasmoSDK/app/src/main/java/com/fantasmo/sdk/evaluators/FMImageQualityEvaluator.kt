package com.fantasmo.sdk.evaluators

import android.content.Context
import android.os.Build
import android.renderscript.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.analytics.FMImageQualityUserInfo
import com.fantasmo.sdk.models.tensorflowML.ImageQualityModelUpdater
import com.fantasmo.sdk.utilities.YuvToRgbConverter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.exp

internal class FMImageQualityEvaluator(val context: Context) {
    val TAG: String = FMImageQualityEvaluator::class.java.simpleName

    companion object {
        // Factory constructor, returns the TFLite evaluator if supported
        fun makeEvaluator(context: Context) : FMFrameEvaluator {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FMImageQualityEvaluatorTFLite(context)
            } else {
                FMImageQualityEvaluatorNotSupported()
            }
        }
    }
}


/// Evaluator class for iOS versions that don't support CoreML
internal class FMImageQualityEvaluatorNotSupported : FMFrameEvaluator {
    override val TAG: String = FMImageQualityEvaluatorNotSupported::class.java.name

    override fun evaluate(fmFrame: FMFrame) : FMFrameEvaluation {
        val imageQualityUserInfo = FMImageQualityUserInfo(null, "device not supported")
        return FMFrameEvaluation(FMFrameEvaluationType.IMAGE_QUALITY_ESTIMATION, 1f, 0f, imageQualityUserInfo)
    }
}


@RequiresApi(Build.VERSION_CODES.N)
internal class FMImageQualityEvaluatorTFLite(val context: Context) :
    FMFrameEvaluator {

    enum class Error {
        NOT_SUPPORTED,
        FAILED_TO_CREATE_MODEL,
        FAILED_TO_CREATE_INPUT_ARRAY,
        FAILED_TO_RESIZE_PIXEL_BUFFER,
        NO_PREDICTION,
        INVALID_FEATURE_VALUE
    }

    override val TAG: String = FMImageQualityEvaluatorTFLite::class.java.name

    private val mlShape = intArrayOf(1, 3, 320, 240)
    private val imageHeight: Int = 320
    private val imageWidth: Int = 240

    private lateinit var rs: RenderScript

    private val yuvToRgbConverter = YuvToRgbConverter(context)

    /**
     * ImageQualityEstimatorModel initializer.
     */
    private var imageQualityModelUpdater = ImageQualityModelUpdater(context)
    val modelVersion
        get() = imageQualityModelUpdater.loadedModelVersion
    private var imageQualityModel: Interpreter? = null

    override fun evaluate(fmFrame: FMFrame): FMFrameEvaluation {
        val evaluationStart = System.currentTimeMillis()

        imageQualityModel = imageQualityModelUpdater.interpreter
        if (!::rs.isInitialized) {
            rs = RenderScript.create(context)
        }

        if (imageQualityModel == null) {
            Log.e(TAG, "Failed to get Model")
            return makeEvaluation(Error.FAILED_TO_CREATE_MODEL)
        }

        val yuvImage = fmFrame.yuvImage
        if (yuvImage == null) {
            // The frame being null means it's no longer available to send in the request
            Log.e(TAG, "Failed to create Input Array")
            return makeEvaluation(Error.FAILED_TO_CREATE_INPUT_ARRAY)
        } else {
            val rgbImage : FloatArray = yuvToRgbConverter.toTensor(yuvImage, imageWidth, imageHeight)

            val score = processImage(rgbImage)

            val evaluationTime = ((System.currentTimeMillis() - evaluationStart).toDouble() / 1000).toFloat()

            if (score != null) {
                Log.d(TAG, "IQE score: $score")
                return makeEvaluation(score, evaluationTime)
            }
            return makeEvaluation(Error.INVALID_FEATURE_VALUE)
        }
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

    fun makeEvaluation(score: Float, time: Float) : FMFrameEvaluation {
        return FMFrameEvaluation(FMFrameEvaluationType.IMAGE_QUALITY_ESTIMATION, score, time,
            FMImageQualityUserInfo(modelVersion))
    }

    fun makeEvaluation(error: Error) : FMFrameEvaluation {
        // We use a score of 1.0 so the frame is always accepted
        val imageQualityUserInfo = FMImageQualityUserInfo(modelVersion, error.name)
        return FMFrameEvaluation(FMFrameEvaluationType.IMAGE_QUALITY_ESTIMATION, 1.0f, 0f, imageQualityUserInfo)
    }
}
