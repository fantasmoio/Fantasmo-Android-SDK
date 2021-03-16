//
//  FMNetworkManager.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.network

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.Volley
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.LocalizeResponse
import com.google.gson.Gson
import org.json.JSONException

/**
 * Manager for network requests.
 */
class FMNetworkManager(
    val url: String,
    context: Context
) {

    private val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }

    /**
     * Method to upload an image with the given [imageData] and [parameters].
     */
    fun uploadImage(imageData: ByteArray, parameters: HashMap<String, String>, onCompletion: (LocalizeResponse) -> Unit, onError: (ErrorResponse) -> Unit) {
        val multipartRequest: MultiPartRequest = object : MultiPartRequest(
            Method.POST, url,
            Response.Listener<NetworkResponse> { response ->
                val resultResponse = String(response.data)
                try {
                    var localizeResult = Gson().fromJson(resultResponse, LocalizeResponse::class.java)
                    onCompletion(localizeResult)

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                val networkResponse = error.networkResponse
                var errorMessage = "Unknown error"
                if (networkResponse == null) {
                    if (error.javaClass == TimeoutError::class.java) {
                        errorMessage = "Request timeout"
                    } else if (error.javaClass == NoConnectionError::class.java) {
                        errorMessage = "Failed to connect server"
                    }
                } else {
                    val errorResult = String(networkResponse.data)
                    try {
                        val response = Gson().fromJson(errorResult, ErrorResponse::class.java)
                        onError(response)

                        when (networkResponse.statusCode) {
                            404 -> {
                                errorMessage = "Resource not found"
                            }
                            401 -> {
                                errorMessage = "${response.message} Authentication error"
                            }
                            400 -> {
                                errorMessage = "${response.message} Wrong parameters"
                            }
                            500 -> {
                                errorMessage = "${response.message} Something is wrong"
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                Log.e("Error", errorMessage)
                error.printStackTrace()
            }) {

            override fun getByteData(): MutableMap<String, FileDataPart> {
                val params = HashMap<String, FileDataPart>()
                params["image"] = FileDataPart("image.jpg", imageData, "image/jpeg")
                return params
            }

            // Overriding getParams() to pass our parameters
            override fun getParams(): MutableMap<String, String> {
                return parameters
            }

            // Overriding getHeaders() to pass our parameters
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Fantasmo-Key"] = "8e785284ca284c01bd84116c0d18e8fd"
                return headers
            }
        }

        // Adding request to the queue
        requestQueue.add(multipartRequest)
    }
}
