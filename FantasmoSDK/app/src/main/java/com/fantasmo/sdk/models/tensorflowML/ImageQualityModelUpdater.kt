package com.fantasmo.sdk.models.tensorflowML

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.network.ModelRequest
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max


internal class ImageQualityModelUpdater(val context: Context) {

    private val TAG = ImageQualityModelUpdater::class.java.simpleName

    var modelVersion = ""
    private set

    private val queue = Volley.newRequestQueue(context)
    private var downloadingModel = false

    private val compatList = CompatibilityList()
    private var loadingInterpreter = false

    /**
     * TFLite interpreter with the model loaded
     */

    var interpreter : Interpreter? = null
        get() {
            val modelAssetVersion = modelAssetVersion
            val remoteConfig = RemoteConfig.remoteConfig
            val modelUri = remoteConfig.imageQualityFilterModelUri
            val remoteModelVersion = remoteConfig.imageQualityFilterModelVersion

            // If there is no interpreter yet, looking through assets and files dir for a model
            if(field == null) {
                val remoteModelFile = remoteModelVersion?.let { findModelInFiles(it) }
                val remoteVersionIsNewer = modelAssetVersion == null ||
                        (remoteModelVersion != null &&
                        compareVersions(remoteModelVersion, modelAssetVersion) == 1)

                if(modelAssetVersion != null) {
                    Log.d(TAG, "Fetching local model $modelAssetVersion")
                    val loadStart = System.currentTimeMillis()
                    interpreter = if(remoteVersionIsNewer && remoteModelFile != null) {
                        Log.d(TAG, "From files dir")
                        modelVersion = remoteModelVersion
                        loadInterpreter(remoteModelFile, options)
                    } else {
                        Log.d(TAG, "From assets")
                        modelVersion = modelAssetVersion
                        modelAssetInterpreter
                    }
                    val loadTime = System.currentTimeMillis() - loadStart
                    Log.d(TAG, "Loading model took $loadTime ms")
                }

                if(modelUri != null && !downloadingModel && remoteModelFile == null && remoteVersionIsNewer) {
                    Log.d(TAG, "Downloading model $remoteModelVersion")
                    downloadModel(modelUri, context.filesDir, "image-quality-estimator-$remoteModelVersion.tflite")
                }
            }

            if(remoteModelVersion != null && compareVersions(remoteModelVersion, modelVersion) == 1) {
                val remoteModelFile = findModelInFiles(remoteModelVersion)
                if(remoteModelFile != null && !loadingInterpreter) {
                    modelVersion = remoteModelVersion
                    val oldInterpreter = field
                    interpreter = loadInterpreter(remoteModelFile, options)
                    oldInterpreter?.close()
                }
            }
            return field
        }
    private set


    /**
     * Optimize model inference performance by delegating to GPU or attributing a number of threads
      */

    private val options = Interpreter.Options().apply {
        if (compatList.isDelegateSupportedOnThisDevice) {
            Log.i(TAG, "Device has GPU support. Using GPU for inference.")
            // if the device has a supported GPU, add the GPU delegate
            val delegateOptions = compatList.bestOptionsForThisDevice
            this.addDelegate(GpuDelegate(delegateOptions))
        } else {
            Log.i(TAG, "Device does not have GPU support. Using CPU for inference.")
            // if the GPU is not supported, run on 4 threads
            this.numThreads = 4
        }
    }

    /**
     * Getting version of the bundled model asset by parsing its filename
     */

    private val modelAssetVersion : String?
        get() {
            val models = context.assets.list("mlmodel/")
            if (models != null && models.size == 1) {
                val modelAssetFilename = models[0]
                val splitFilename = modelAssetFilename.split('-')
                return splitFilename.last().removeSuffix(".tflite")
            }
            return null
        }

    /**
     * Getting the interpreter from the model in the bundled assets
     */

    private val modelAssetInterpreter : Interpreter?
    get() {
        val models = context.assets.list("mlmodel/")
        if (models != null && models.size == 1) {
            val modelAssetFilename = models[0]
            val modelAsset =
                try {
                    val fileDescriptor: AssetFileDescriptor =
                        context.assets.openFd("mlmodel/$modelAssetFilename")
                    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                    val fileChannel: FileChannel = inputStream.channel
                    fileChannel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fileDescriptor.startOffset,
                        fileDescriptor.declaredLength
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
            return if (modelAsset != null)
                loadInterpreter(modelAsset, options)
            else
                null
        }
        return null
    }

    /**
     * Method to send a GET request in order to update the model.
     */
    private fun downloadModel(modelUri : String, dir : File, fileName : String) {
        downloadingModel = true
        val stringRequest = ModelRequest(
            Request.Method.GET, modelUri,
            { response ->
                try {
                    Log.d(TAG, "Model Network Response received, writing to file $fileName...")
                    val fileOutputStream = FileOutputStream(
                        File(dir,
                            fileName
                        )
                    )
                    fileOutputStream.write(response)
                    fileOutputStream.close()
                    Log.d(TAG, "Model File Successfully Downloaded.")
                } catch (e: IOException) {
                    Log.e(TAG, "Model File write failed: $e.")
                }
                downloadingModel = false
            },
            {
                Log.e(TAG, "Error Downloading Model.")
                downloadingModel = false
            }
        )
        queue.add(stringRequest)
    }

    /**
     * Given a model version, finds the corresponding file in files dir
     */

    private fun findModelInFiles(modelVersion : String) : File? {
        val fileName = "image-quality-estimator-$modelVersion.tflite"
        val file = File(context.filesDir, fileName)
        return if(file.exists())
            file
        else
            null
    }

    /**
     * Method that delivers an `Interpreter` with the model loaded onto it.
     *
     * @return `Interpreter` with model loaded
     */

    private fun loadInterpreter(file : File, options : Interpreter.Options) : Interpreter? {
        try {
            Log.d(TAG, "Loading interpreter in file $file")
            //Initialize interpreter an keep it in memory
            return Interpreter(file, options)
        } catch (ex: IOException) {
            //file does not exist
            Log.e(TAG, "Error on reading the model.")
            return null
        } catch (e: Error) {
            e.localizedMessage?.let { Log.e(TAG, it) }
            return null
        } catch (ex: Exception) {
            //could be delegate problem, trying again with CPU
            return if (compatList.isDelegateSupportedOnThisDevice) {
                try {
                    Log.d(
                        TAG,
                        "Falling back to CPU interpreter after exception loading GPU delegate"
                    )
                    Interpreter(
                        file,
                        Interpreter.Options().apply { this.numThreads = 4 })
                } catch (ex: Exception) {
                    ex.localizedMessage?.let { Log.e(TAG, it) }
                    null
                }
            } else {
                ex.localizedMessage?.let { Log.e(TAG, it) }
                null
            }
        }
    }

    /**
     * Method that delivers an `Interpreter` with the model loaded onto it.
     *
     * @return `Interpreter` with model loaded
     */

    private fun loadInterpreter(asset : MappedByteBuffer, options : Interpreter.Options) : Interpreter? {
        try {
            Log.d(TAG, "Loading interpreter in asset")
            //Initialize interpreter an keep it in memory
            return Interpreter(asset, options)
        } catch (ex: IOException) {
            //file does not exist
            Log.e(TAG, "Error on reading the model.")
            return null
        } catch (e: Error) {
            e.localizedMessage?.let { Log.e(TAG, it) }
            return null
        } catch (ex: Exception) {
            //could be delegate problem, trying again with CPU
            return if (compatList.isDelegateSupportedOnThisDevice) {
                try {
                    Log.d(
                        TAG,
                        "Falling back to CPU interpreter after exception loading GPU delegate"
                    )
                    Interpreter(
                        asset,
                        Interpreter.Options().apply { this.numThreads = 4 })
                } catch (ex: Exception) {
                    ex.localizedMessage?.let { Log.e(TAG, it) }
                    null
                }
            } else {
                ex.localizedMessage?.let { Log.e(TAG, it) }
                null
            }
        }
    }

    /**
     * Method that compares version strings for model update logic
     */

    private fun compareVersions(version1: String, version2: String): Int {
        if(version1 == "" && version2 == "")
            return 0
        if(version1 == "")
            return -1
        if(version2 == "")
            return 1

        var comparisonResult = 0
        val version1Splits = version1.split('.')
        val version2Splits = version2.split('.')
        val maxLengthOfVersionSplits = max(version1Splits.size, version2Splits.size)

        for (i in 0 until maxLengthOfVersionSplits) {
            val v1 = if (i < version1Splits.size) version1Splits[i].toInt() else 0
            val v2 = if (i < version2Splits.size) version2Splits[i].toInt() else 0
            val compare = v1.compareTo(v2)
            if (compare != 0) {
                comparisonResult = compare
                break
            }
        }
        return comparisonResult
    }
}
