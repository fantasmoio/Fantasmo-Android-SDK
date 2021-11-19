package com.fantasmo.sdk.models.tensorflowML

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.FMUtility
import com.google.ar.core.Frame
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions

@RequiresApi(Build.VERSION_CODES.KITKAT)
class ImageQualityEstimatorMLKit(context: Context) : ImageQualityEstimatorProtocol {

    private val TAG = ImageQualityEstimatorMLKit::class.java.simpleName

    private val flowerModel = LocalModel.Builder()
        .setAssetFilePath("models/FlowerModel.tflite")
        .build()

    override fun estimateImageQuality(frame: Frame): ImageQualityEstimationResult {
        return ImageQualityEstimationResult.UNKNOWN
    }

    override fun estimateImageQuality(frame: Frame, callback: (ImageQualityEstimationResult) -> Unit) {
        Log.d(TAG, "Entered EstimateImageQuality")

        // TODO 2: Convert Image to Bitmap then to TensorImage
        val frameToByteArray = FMUtility.frameToByteArray
        if(frameToByteArray == null){
            callback(ImageQualityEstimationResult.ERROR("null image"))
            return
        }

        val bitmap = BitmapFactory.decodeByteArray(frameToByteArray, 0, frameToByteArray.size)
        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(flowerModel)
            .setConfidenceThreshold(0.5f)
            .setMaxResultCount(5)
            .build()
        val labeler = ImageLabeling.getClient(customImageLabelerOptions)
        val image = InputImage.fromBitmap(bitmap, 0)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                if(labels.size == 0){
                    callback(ImageQualityEstimationResult.UNKNOWN)
                }
                // Task completed successfully
                for (label in labels) {
                    val text = label.text
                    val confidence = label.confidence
                    val index = label.index
                    Log.d(TAG,"$text: $confidence at index: $index")
                    callback(ImageQualityEstimationResult.ESTIMATE(confidence))
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                callback(ImageQualityEstimationResult.ERROR(e.stackTraceToString()))
            }
    }
}