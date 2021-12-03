package com.fantasmo.sdk.utilities

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.fantasmo.sdk.network.ModelRequest
import com.fantasmo.sdk.views.common.samplerender.SampleRender
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

class ModelManager(val context: Context) {

    private val TAG = ModelManager::class.java.simpleName
    private val fileName = "image-quality-estimator.tflite"
    private val modelUrl =
        "url/$fileName"

    private var hasModel = false

    private val queue = Volley.newRequestQueue(context)

    private fun makeRequest() {
        val stringRequest = ModelRequest(
            Request.Method.GET, modelUrl,
            { response ->
                try {
                    val fileOutputStream = FileOutputStream(File(context.filesDir, fileName))
                    fileOutputStream.write(response)
                    fileOutputStream.close()
                    Log.d(TAG, "File Written successfully")
                } catch (e: IOException) {
                    Log.e(TAG, "File write failed: $e")
                }
                hasModel = true
            },
            {
                Log.e(TAG, "Error Downloading Model")
                hasModel = false
            }
        )

        queue.add(stringRequest)
    }

    fun getInterpreter(): Interpreter? {
        var result = loadFromAssets()
        if(result == null){
            result = loadFromURL()
        }
        return result
    }

    @Throws(IOException::class)
    private fun loadFromAssets(): Interpreter? {
        val fileName = "image-quality-estimator2.tflite"
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
            return Interpreter(mappedByteBuffer)
        } catch (ex: IOException) {
            //file does not exist
            null
        }
    }

    /**
     * Checks if device has GPU acceleration compatibility.
     * In negative case, creates model with 4 dedicated threads
     * */
    private lateinit var runtimeInterpreter: Interpreter
    private var firstRead = false

    @Throws(IOException::class)
    private fun loadFromURL(): Interpreter? {
        if (!hasModel) {
            makeRequest()
            return null
        }
        return try { //if (firstRead) {
                //File exists so do something with it
                val file = File(context.filesDir, fileName)
                firstRead = false
                Interpreter(file)
            } catch (ex: IOException) {
                //file does not exist
                Log.e(TAG, "Error on Getting the model")
                null
            }
        /*
        } else {
            runtimeInterpreter
        }*/
    }
}