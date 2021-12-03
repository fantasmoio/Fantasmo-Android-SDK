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

    private val queue = Volley.newRequestQueue(context)
    private var hasRequestedModel = false

    private fun makeRequest() {
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

    fun getInterpreter(): Interpreter? {
        return if(::interpreter.isInitialized){
            interpreter
        }else {
            var result = loadFromAssets()
            if (result == null) {
                result = loadFromURL()
            }
            result
        }
    }

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
            interpreter = Interpreter(mappedByteBuffer)
            return Interpreter(mappedByteBuffer)
        } catch (ex: IOException) {
            //file does not exist
            Log.e(TAG, "Error on getting the model from the Assets folder. Trying to download it")
            null
        }
    }

    /**
     * Checks if device has GPU acceleration compatibility.
     * In negative case, creates model with 4 dedicated threads
     */
    private lateinit var interpreter: Interpreter
    private var firstRead = true

    private fun loadFromURL(): Interpreter? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            if(!hasRequestedModel){
                Log.e(TAG,"Model file doesn't exist. Downloading it...")
                makeRequest()
            }
            return null
        } else {
            // There's no need to read from the file everytime we need to interpret the model
            return if (!firstRead) {
                interpreter
            } else {
                try {
                    //Initialize interpreter an keep it in memory
                    interpreter = Interpreter(file)
                    firstRead = false
                    Interpreter(file)
                } catch (ex: IOException) {
                    //file does not exist
                    Log.e(TAG, "Error on reading the model.")
                    null
                }
            }
        }
    }
}