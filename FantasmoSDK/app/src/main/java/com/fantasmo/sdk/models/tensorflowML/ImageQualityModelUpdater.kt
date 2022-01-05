package com.fantasmo.sdk.models.tensorflowML

import android.content.Context
import android.util.Log
import com.fantasmo.sdk.views.common.samplerender.SampleRender
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.IOException
import java.nio.channels.FileChannel

class ImageQualityModelUpdater(val context: Context) {

    private val TAG = ImageQualityModelUpdater::class.java.simpleName
    private val compatList = CompatibilityList()

    /**
     * TFLite interpreter with the model loaded
     */
    private lateinit var interpreter: Interpreter

    // Optimize model inference performance by delegating to GPU or attributing a number of threads
    private val options = Interpreter.Options().apply{
        if(compatList.isDelegateSupportedOnThisDevice){
            Log.i(TAG,"Device has GPU support. Using GPU for inference.")
            // if the device has a supported GPU, add the GPU delegate
            val delegateOptions = compatList.bestOptionsForThisDevice
            this.addDelegate(GpuDelegate(delegateOptions))
        } else {
            Log.i(TAG,"Device does not have GPU support. Using CPU for inference.")
            // if the GPU is not supported, run on 4 threads
            this.setNumThreads(4)
        }
    }

    /**
     * Method that delivers an `Interpreter` with the model loaded onto it.
     * First checks if the global interpreter has been loaded into memory.
     * In negative case, it will check if the model is present in the assets
     * folder. If it isn't present in the assets, returns null
     * @return `Interpreter` with model loaded
     */
    fun getInterpreter(): Interpreter? {
        return if (::interpreter.isInitialized) {
            interpreter
        } else {
            loadFromAssets()
        }
    }

    /**
     * Loads a model from the assets folder and returns an `Interpreter`
     * @return `Interpreter` with the model loaded
     */
    private fun loadFromAssets(): Interpreter? {
        val fileName = "image-quality-estimator.tflite"
        val assetFileName = "model/$fileName"
        return try {
            //File exists so do something with it
            val fileDescriptor = SampleRender.getAssets().openFd(assetFileName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val fileChannel = inputStream.channel
            val mappedByteBuffer =
                fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(mappedByteBuffer, options)
            return Interpreter(mappedByteBuffer, options)
        } catch (ex: IOException) {
            //file does not exist
            Log.e(TAG, "Error on getting the model from the Assets folder. Trying to download it")
            null
        }
    }
}