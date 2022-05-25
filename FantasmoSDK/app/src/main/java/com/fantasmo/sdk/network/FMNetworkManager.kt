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
import android.os.Build
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.IsLocalizationAvailableResponse
import com.fantasmo.sdk.models.LocalizeResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.json.JSONException
import org.json.JSONObject

/**
 * Manager for network requests.
 */
internal class FMNetworkManager(
    private val context: Context
) {
    private val TAG = FMNetworkManager::class.java.simpleName

    private val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext).apply {
            start()
        }
    }

    private val oneOffQueues: MutableList<RequestQueue> = mutableListOf()

    lateinit var multipartRequest: MultiPartRequest

    /**
     * Method to upload an image with the given [imageData] and [parameters].
     */
    fun uploadImage(
        url: String,
        imageData: ByteArray,
        parameters: HashMap<String, String>,
        token: String,
        onCompletion: (LocalizeResponse) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        Log.i(TAG, "$url $parameters")
        multipartRequest = object : MultiPartRequest(
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
                val response = processAndLogError(error)

                if (response != null) {
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
            val queue = Volley.newRequestQueue(context)
            oneOffQueues.add(queue)
            multipartRequest.tag = this
            queue.add(multipartRequest)
        } else {
            Log.w(TAG, "No internet connection available")
        }
    }

    /**
     * Method to send a POST request to check whether a zone is in a provided radius.
     */
    fun isLocalizationAvailableRequest(
        url: String,
        parameters: HashMap<String, String>,
        token: String,
        onCompletion: (Boolean) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        Log.i(TAG, "$url $parameters")
        multipartRequest = object : MultiPartRequest(
            Method.POST, url,
            Response.Listener<NetworkResponse> { response ->
                val resultResponse = String(response.data)
                Log.d(TAG, "IsLocalizationAvailableRequest RESPONSE: $resultResponse")
                try {
                    val isLocalizationAvailableResponse =
                        Gson().fromJson(resultResponse, IsLocalizationAvailableResponse::class.java)
                    onCompletion(isLocalizationAvailableResponse.available.toBoolean())
                } catch (e: JSONException) {
                    onCompletion(false)
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                val response = processAndLogError(error)
                if (response != null) {
                    onError(response)
                } else {
                    onError(ErrorResponse(404, "UnknownError"))
                }
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
            Log.w(TAG, "No internet connection available")
        }
    }

    fun sendInitializationRequest(
        url: String,
        parameters: HashMap<String, String>,
        token: String,
        onCompletion: (Boolean) -> Unit,
        onError: (ErrorResponse) -> Unit
    ) {
        Log.i(TAG, "$url $parameters")
        multipartRequest = object : MultiPartRequest(
            Method.POST, url,
            Response.Listener<NetworkResponse> { response ->
                val resultResponse = String(response.data)
                Log.d(TAG, "sendInitializationRequest RESPONSE: $resultResponse")
                try {
                    val initializeResponse = JSONObject(resultResponse)
                    val onCompletionResult = initializeResponse.getBoolean("parking_in_radius")
                    if (!onCompletionResult) {
                        val reason = initializeResponse.getString("fantasmo_unavailable_reason")
                        val reasonError = ErrorResponse(0, reason)
                        onError(reasonError)
                    } else {
                        val configString = initializeResponse.optString("config")
                        RemoteConfig.updateConfig(context, configString)
                    }
                    onCompletion(onCompletionResult)
                } catch (e: JSONException) {
                    onError(ErrorResponse(0, "JSONException"))
                }
            },
            Response.ErrorListener { error ->
                val response = processAndLogError(error)
                if (response != null) {
                    onError(response)
                } else {
                    onError(ErrorResponse(404, "UnknownError"))
                }
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
            Log.w(TAG, "No internet connection available")
        }
    }

    /**
     * Method to process and log network error.
     */
    private fun processAndLogError(error: VolleyError) : ErrorResponse? {
        val networkResponse = error.networkResponse
        var errorMessage = "Unknown error"
        var response : ErrorResponse? = null
        if (networkResponse == null) {
            if (error.javaClass == TimeoutError::class.java) {
                errorMessage = "Request timeout"
            } else if (error.javaClass == NoConnectionError::class.java) {
                errorMessage = "Failed to connect server"
            }
        } else {
            val errorResult = String(networkResponse.data)
            try {
                response = Gson().fromJson(errorResult, ErrorResponse::class.java)
                val debugMessage = response.message ?: response.detail ?: ""

                when (networkResponse.statusCode) {
                    404 -> {
                        errorMessage = "Resource not found"
                    }
                    403 -> {
                        errorMessage = "Forbidden"
                    }
                    401 -> {
                        errorMessage = "$debugMessage Authentication error"
                    }
                    400 -> {
                        errorMessage = "$debugMessage Wrong parameters"
                    }
                    500 -> {
                        errorMessage = "$debugMessage Something is wrong"
                    }
                }
            } catch (e: JSONException) {
                Log.e(TAG, "JSONException: ${e.message}")
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "JSONSyntaxException: ${e.message}")
            }
        }
        Log.e(TAG, "Network Error: $errorMessage")
        return response
    }

    /**
     * Check for internet connection.
     */
    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCapabilities = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {connectivityManager.activeNetwork ?: return false} else return false
        val actNw =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun sendSessionAnalyticsRequest(
        url: String,
        sessionAnalyticsParams: String,//java.util.HashMap<String, String>,
        token: String,
        onCompletion: (Boolean) -> Unit,
        onError: (ErrorResponse) -> Unit
    )   {
        Log.i(TAG, "$url $sessionAnalyticsParams")
        val jsonRequest = object : JsonObjectRequest( Method.POST,
            url,
            JSONObject(sessionAnalyticsParams),//.toMap()),
            Response.Listener { response ->
                val resultResponse = response.toString()
                Log.d(TAG, "IsLocalizationAvailableRequest RESPONSE: $resultResponse")
                try {
                    val isLocalizationAvailableResponse =
                        Gson().fromJson(resultResponse, IsLocalizationAvailableResponse::class.java)
                    onCompletion(isLocalizationAvailableResponse.available.toBoolean())
                } catch (e: JSONException) {
                    onCompletion(false)
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                val response = processAndLogError(error)
                if (response != null) {
                    onError(response)
                } else {
                    onError(ErrorResponse(404, "UnknownError"))
                }
            }) {
            // Overriding getHeaders() to pass our parameters
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Fantasmo-Key"] = token
                return headers
            }
        }

        // Adding request to the queue if there is a connection
        if (isInternetAvailable()) {
            requestQueue.add(jsonRequest)
        } else {
            Log.w(TAG, "No internet connection available")
        }
    }

    fun stopAllLocalizeRequests() {
        oneOffQueues.forEach{ it -> it.cancelAll(this)}
        oneOffQueues.clear()
    }
}
