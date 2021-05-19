//
//  FMNetworkManager.kt
//  FantasmoSDK
//
//  Copyright © 2021 Fantasmo. All rights reserved.
//
package com.fantasmo.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.Volley
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.LocalizeResponse
import com.fantasmo.sdk.models.ZoneInRadiusResponse
import com.google.gson.Gson
import org.json.JSONException

/**
 * Manager for network requests.
 */
class FMNetworkManager(
    val url: String,
    private val context: Context
) {
    private val TAG = "FMNetworkManager"

    private val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }

    /**
     * Method to upload an image with the given [imageData] and [parameters].
     */
    fun uploadImage(
        imageData: ByteArray,
        parameters: HashMap<String, String>,
        token: String,
        onCompletion: (LocalizeResponse) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        val multipartRequest: MultiPartRequest = object : MultiPartRequest(
            Method.POST, url,
            Response.Listener<NetworkResponse> { response ->
                val resultResponse = String(response.data)
                try {
                    val localizeResult =
                        Gson().fromJson(resultResponse, LocalizeResponse::class.java)
                    onCompletion(localizeResult)

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                processAndLogError(error)

                if (error.networkResponse != null) {
                    val errorResult = String(error.networkResponse.data)
                    val response = Gson().fromJson(errorResult, ErrorResponse::class.java)
                    onError(response)
                } else {
                    onError(ErrorResponse(404, "UnknownError"))
                }
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
                headers["Fantasmo-Key"] = token
                return headers
            }
        }

        // Adding request to the queue if there is a connection
        if (isInternetAvailable()) {
            requestQueue.add(multipartRequest)
        } else {
            Log.d(TAG, "No internet connection available")
        }
    }

    /**
     * Method to send a POST request to check whether a zone is in a provided radius.
     */
    fun zoneInRadiusRequest(
        url: String,
        parameters: HashMap<String, String>,
        token: String,
        onCompletion: (Boolean) -> Unit
    ) {
        val multipartRequest: MultiPartRequest = object : MultiPartRequest(
            Method.POST, url,
            Response.Listener<NetworkResponse> { response ->
                val resultResponse = String(response.data)
                Log.d(TAG, "zoneInRadiusRequest RESPONSE: $resultResponse")
                try {
                    val inRadius =
                        Gson().fromJson(resultResponse, ZoneInRadiusResponse::class.java)
                    onCompletion(inRadius.result.toBoolean())
                } catch (e: JSONException) {
                    onCompletion(false)
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                processAndLogError(error)
                onCompletion(false)
            }) {

            // Overriding getParams() to pass our parameters
            override fun getParams(): MutableMap<String, String> {
                return parameters
            }

            // Overriding getHeaders() to pass our parameters
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Fantasmo-Key"] = token
                return headers
            }
        }

        // Adding request to the queue if there is a connection
        if (isInternetAvailable()) {
            requestQueue.add(multipartRequest)
        } else {
            Log.d(TAG, "No internet connection available")
        }
    }

    /**
     * Method to process and log network error.
     */
    private fun processAndLogError(error: VolleyError) {
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
                Log.e(TAG, "JSONException: ${e.message}")
            }
        }
        Log.e(TAG, "Network Error: $errorMessage")
    }

    /**
     * Check for internet connection.
     */
    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
