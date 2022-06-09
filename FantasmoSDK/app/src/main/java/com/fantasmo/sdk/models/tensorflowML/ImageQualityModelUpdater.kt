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

    private var latestLocalVersion = ""

    val modelVersion : String
    get() {return latestLocalVersion}

    private val queue = Volley.newRequestQueue(context)
    private var downloadingModel = false

    private val compatList = CompatibilityList()

    /**
     * TFLite interpreter with the model loaded
     */

    var interpreter : Interpreter? = null
        get() {
            val remoteConfig = RemoteConfig.config(context)
            val modelUri = remoteConfig.imageQualityFilterModelUri ?: ""
            val remoteConfigModelVersion = remoteConfig.imageQualityFilterModelVersion ?: ""

            // If there is no interpreter yet, looking through assets and files dir for a model
            if(field == null) {
                val downloadedModels =
                    context.filesDir.listFiles { file -> file.extension == "tflite" }
                val downloadedVersions = downloadedModels?.map { getVersionFromFileName(it.name) }
                val highestDownloadedVersion =
                    downloadedVersions?.maxWithOrNull(VersionComparator) ?: ""

                val bundledModelDirList = context.assets.list("mlmodel/")
                val bundledModel = if (bundledModelDirList != null && bundledModelDirList.size == 1)
                    bundledModelDirList[0]
                else null
                val bundledModelVersion = bundledModel?.let { getVersionFromFileName(it) } ?: ""

                latestLocalVersion =
                    maxOf(highestDownloadedVersion, bundledModelVersion, VersionComparator)

                if (latestLocalVersion == bundledModelVersion) {
                    Log.d(TAG, "Loading bundled model version $latestLocalVersion")
                    interpreter = bundledModel?.let { loadBundledModelInterpreter(it) }
                } else {
                    if (downloadedVersions != null) {
                        Log.d(TAG, "Loading downloaded model version $latestLocalVersion")
                        val downloadedModelFile =
                            downloadedModels[downloadedVersions.indexOf(highestDownloadedVersion)]
                        interpreter = loadInterpreter(downloadedModelFile, options)
                    }
                }

                if (!downloadingModel && VersionComparator.compare(remoteConfigModelVersion, latestLocalVersion) == 1) {
                    Log.d(TAG, "Downloading model $remoteConfigModelVersion")

                    downloadModel(
                        modelUri,
                        context.filesDir,
                        "image-quality-estimator-$remoteConfigModelVersion.tflite"
                    )
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
     * Getting the interpreter from the model in the bundled assets
     */

    private fun loadBundledModelInterpreter(fileName: String) : Interpreter? {
        val modelAsset =
            try {
                val fileDescriptor: AssetFileDescriptor =
                    context.assets.openFd("mlmodel/$fileName")
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
        else null
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
     * Given a file name, get the corresponding model version
     */

    private fun getVersionFromFileName(fileName : String) : String {
        val splits = fileName.split('-')
        return splits.last().removeSuffix(".tflite")
    }

    /**
     * Method that delivers an `Interpreter` with the model loaded onto it.
     *
     * @return `Interpreter` with model loaded
     */

    private fun loadInterpreter(file : File, options : Interpreter.Options) : Interpreter? {
        val fileInputStream = FileInputStream(file)
        val fileChannel: FileChannel = fileInputStream.channel
        return loadInterpreter(fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            0,
            fileChannel.size()
        ), options)
    }

    /**
     * Method that delivers an `Interpreter` with the model loaded onto it.
     *
     * @return `Interpreter` with model loaded
     */

    private fun loadInterpreter(byteBuffer : MappedByteBuffer, options : Interpreter.Options) : Interpreter? {
        try {
            //Initialize interpreter an keep it in memory
            return Interpreter(byteBuffer, options)
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
                        byteBuffer,
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


    class VersionComparator {
        companion object : Comparator<String> {
            override fun compare(version1: String, version2: String): Int {
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
    }
}
