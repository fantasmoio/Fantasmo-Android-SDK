package com.fantasmo.sdk.models.tensorflowML

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.network.ModelRequest
import com.fantasmo.sdk.views.common.samplerender.SampleRender
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

class ImageQualityModelUpdater(val context: Context) {

    private val TAG = ImageQualityModelUpdater::class.java.simpleName
    private val fileName = "image-quality-estimator.tflite"
    var modelVersion = "0.0.0"
    private var modelUrl = ""
    private val queue = Volley.newRequestQueue(context)
    private var hasRequestedModel = false
    private var hasRequestedUpdate = false

    private val compatList = CompatibilityList()

    /**
     * TFLite interpreter with the model loaded
     */
    private lateinit var interpreter: Interpreter
    private var firstRead = true

    // Optimize model inference performance by delegating to GPU or attributing a number of threads
    private val options = Interpreter.Options().apply {
        if (compatList.isDelegateSupportedOnThisDevice) {
            Log.i(TAG, "Device has GPU support. Using GPU for inference.")
            // if the device has a supported GPU, add the GPU delegate
            val delegateOptions = compatList.bestOptionsForThisDevice
            this.addDelegate(GpuDelegate(delegateOptions))
        } else {
            Log.i(TAG, "Device does not have GPU support. Using CPU for inference.")
            // if the GPU is not supported, run on 4 threads
            this.setNumThreads(4)
        }
    }

    init {
        val remoteConfig = RemoteConfig.remoteConfig
        if (remoteConfig.imageQualityFilterModelUri != null &&
            remoteConfig.imageQualityFilterModelVersion != null &&
            remoteConfig.imageQualityFilterModelVersion != modelVersion
        ) {
            modelUrl = remoteConfig.imageQualityFilterModelUri!!
            modelVersion = remoteConfig.imageQualityFilterModelVersion!!
            hasRequestedUpdate = true
            Log.d(TAG, "Updating model to version $modelVersion")
        } else {
            Log.d(TAG, "No model specified in remote config")
        }
    }

    /**
     * Method to send a GET request in order to update the model.
     */
    private fun makeRemoteModelRequest() {
        hasRequestedModel = true
        val stringRequest = ModelRequest(
            Request.Method.GET, modelUrl,
            { response ->
                try {
                    val fileOutputStream = FileOutputStream(File(context.filesDir, fileName))
                    fileOutputStream.write(response)
                    fileOutputStream.close()
                    Log.d(TAG, "Model File Successfully Downloaded.")
                } catch (e: IOException) {
                    Log.e(TAG, "Model File write failed: $e.")
                    hasRequestedModel = false
                }
            },
            {
                Log.e(TAG, "Error Downloading Model.")
                hasRequestedModel = false
            }
        )

        queue.add(stringRequest)
    }

    /**
     * Method that delivers an `Interpreter` with the model loaded onto it.
     * First checks if the global interpreter has been loaded into memory.
     * In negative case, it will check if the model is present in the assets
     * folder. If it isn't present in the assets, this method will make a
     * request to get a remote model.
     * @return `Interpreter` with model loaded
     */
    fun getInterpreter(): Interpreter? {
        return if (::interpreter.isInitialized) {
            interpreter
        } else {
            var result = loadFromAssets()
            if (result == null) {
                result = loadFromURL()
            }
            result
        }
    }

    /**
     * Loads a model from the assets folder and returns an `Interpreter`
     * @return `Interpreter` with the model loaded
     */
    private fun loadFromAssets(): Interpreter? {
        val fileName = "image-quality-estimator-0.1.0.tflite"
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

    /**
     * Loads a model from the `RemoteConfig` model uri field and returns an `Interpreter`.
     * First checks if it has a model present in the app data folder. If it has a model
     * it will load that model. If it doesn't have it will get from the `RemoteConfig`
     * model uri
     * @return `Interpreter` with the model loaded
     */
    private fun loadFromURL(): Interpreter? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            if (!hasRequestedModel) {
                if (hasRequestedUpdate) {
                    Log.d(TAG, "New Model update. Downloading it...")
                    hasRequestedUpdate = false
                } else {
                    Log.e(TAG, "Model file doesn't exist. Downloading it...")
                }
                makeRemoteModelRequest()
            }
            return null
        } else {
            // There's no need to read from the file everytime we need to interpret the model
            return if (!firstRead) {
                interpreter
            } else {
                try {
                    Log.d(TAG, "Model present in App data.")
                    //Initialize interpreter an keep it in memory
                    interpreter = Interpreter(file, options)
                    firstRead = false
                    Interpreter(file, options)
                } catch (ex: IOException) {
                    //file does not exist
                    Log.e(TAG, "Error on reading the model.")
                    null
                }
            }
        }
    }
}