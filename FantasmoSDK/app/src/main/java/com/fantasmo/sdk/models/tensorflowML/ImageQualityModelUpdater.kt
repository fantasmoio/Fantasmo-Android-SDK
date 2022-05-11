package com.fantasmo.sdk.models.tensorflowML

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.network.ModelRequest
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageQualityModelUpdater(val context: Context) {

    private val TAG = ImageQualityModelUpdater::class.java.simpleName
    private var fileName: String? = null
    var modelVersion = ""
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

    /**
     * Method to send a GET request in order to update the model.
     */
    private fun downloadModel() {
        hasRequestedModel = true
        val stringRequest = ModelRequest(
            Request.Method.GET, modelUrl,
            { response ->
                try {
                    Log.d(TAG, "Model Network Response received, writing to file...")
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
     * Checks if there's a config change. If true, it will request a new
     * model. Else if there's no config change it will try to load a interpreter
     * that has been loaded into memory. In negative case, it will check if the
     * updates are loaded and use those instead.
     * @return `Interpreter` with model loaded
     */
    fun getInterpreter(): Interpreter? {
        val remoteConfig = RemoteConfig.remoteConfig
        val modelUri = remoteConfig.imageQualityFilterModelUri
        val remoteModelVersion = remoteConfig.imageQualityFilterModelVersion
        if (modelUri != null &&
            remoteModelVersion != null &&
            remoteModelVersion != modelVersion
        ) {
            modelUrl = modelUri
            modelVersion = remoteModelVersion
            fileName = "image-quality-estimator-$modelVersion.tflite"
            hasRequestedUpdate = true
            Log.d(TAG, "Received model version: $modelVersion")
            return checkForUpdates()
        } else {
            return if (::interpreter.isInitialized) {
                interpreter
            } else {
                checkForUpdates()
            }
        }
    }

    /**
     * Loads a model from the `RemoteConfig` model uri field and returns an `Interpreter`.
     * First checks if it has a model present in the app data folder. If it has a model
     * it will load that model. If it doesn't have it will get from the `RemoteConfig`
     * model uri
     * @return `Interpreter` with the model loaded
     */
    private fun checkForUpdates(): Interpreter? {
        if(fileName == null) {
            return null
        }
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            if (!hasRequestedModel) {
                if (hasRequestedUpdate) {
                    Log.d(TAG, "New Model version: $modelVersion. Downloading it...")
                    hasRequestedUpdate = false
                } else {
                    Log.e(TAG, "Model file doesn't exist. Downloading it...")
                }
                downloadModel()
            }
            return null
        } else {
            // There's no need to read from the file everytime we need to interpret the model
            return if (!firstRead) {
                interpreter
            } else {
                try {
                    Log.d(TAG, "Model file present in file ${context.filesDir}/$fileName")
                    //Initialize interpreter an keep it in memory
                    interpreter = Interpreter(file, options)
                    modelVersion = RemoteConfig.remoteConfig.imageQualityFilterModelVersion ?: ""
                    firstRead = false
                    interpreter
                } catch (ex: IOException) {
                    //file does not exist
                    Log.e(TAG, "Error on reading the model.")
                    null
                } catch (e: Error) {
                    Log.e(TAG, e.localizedMessage)
                    null
                } catch (ex: Exception) {
                    //could be delegate problem, trying again with CPU
                    if (compatList.isDelegateSupportedOnThisDevice) {
                        try {
                            interpreter = Interpreter(
                                file,
                                Interpreter.Options().apply { this.setNumThreads(4) })
                            modelVersion = RemoteConfig.remoteConfig.imageQualityFilterModelVersion ?: ""
                            firstRead = false
                            interpreter
                        } catch(ex: Exception) {
                            Log.e(TAG, ex.localizedMessage)
                            null
                        }
                    }
                    Log.e(TAG, ex.localizedMessage)
                    null
                }
            }
        }
    }
}