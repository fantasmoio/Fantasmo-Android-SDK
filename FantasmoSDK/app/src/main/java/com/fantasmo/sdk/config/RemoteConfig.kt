package com.fantasmo.sdk.config

import android.content.Context
import android.util.Log
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.FMUtility.Constants.fileName
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.StringBuilder


class RemoteConfig {

    data class Config(
        var remoteConfigId: String,
        var frameAcceptanceThresholdTimeout: Float,
        var isBehaviorRequesterEnabled: Boolean,
        var isTrackingStateFilterEnabled: Boolean,
        var isMovementFilterEnabled: Boolean,
        var movementFilterThreshold: Float,
        var isBlurFilterEnabled: Boolean,
        var blurFilterVarianceThreshold: Float,
        var blurFilterSuddenDropThreshold: Float,
        var blurFilterAverageThroughputThreshold: Float,
        var isCameraPitchFilterEnabled: Boolean,
        var cameraPitchFilterMaxUpwardTilt: Float,
        var cameraPitchFilterMaxDownwardTilt: Float,
        var isImageQualityFilterEnabled: Boolean,
        var imageQualityFilterScoreThreshold: Float,
        var imageQualityFilterModelUri: String?,
        var imageQualityFilterModelVersion: String?
    )

    companion object {
        private val TAG = RemoteConfig::class.java.simpleName
        lateinit var remoteConfig: Config

        /**
         * Updates current `Config`
         * @param context Application context
         * @param jsonString `Json` read from the request response
         */
        fun updateConfig(context: Context, jsonString: String) {
            val config = getConfigFromJSON(jsonString)
            remoteConfig = if (config == null) {
                getConfig(context)!!
            } else {
                Log.i(TAG, "Received Valid Remote Config.")
                try {
                    val file = File(context.filesDir, fileName)
                    val fileWriter = FileWriter(file)
                    val bufferedWriter = BufferedWriter(fileWriter)
                    bufferedWriter.write(jsonString)
                    bufferedWriter.close()
                    Log.i(TAG, "Successfully saved new remote config.")
                } catch (e: IOException) {
                    Log.e(TAG, "Remote Config File write failed: $e.")
                }
                config
            }
        }

        /**
         * Loads a valid `Config` from both the assets or filesDir folder
         * @param context Application context
         * @return `Config` to be used throughout the SDK
         */
        private fun getConfig(context: Context): Config? {
            val file = File(context.filesDir, fileName)
            return if (!file.exists()) {
                getAssetDefaultConfig(context)
            } else {
                getPreviousSavedConfig(file)
            }
        }

        /**
         * Loads the default `Config` from the assets folder
         * @param context Application context
         * @return Default `Config` saved in the assets folder
         */
        private fun getAssetDefaultConfig(context: Context): Config? {
            Log.i(TAG, "Getting default config.")
            val jsonFileString = getJsonFromAssets(context)
            if (jsonFileString != null) {
                return getConfigFromJSON(jsonFileString)
            }
            return null
        }

        /**
         * Loads a valid `Config` from the both the assets or filesDir folder
         * @param file Previous Saved Config File
         * @return Previous valid saved `Config`
         */
        private fun getPreviousSavedConfig(file: File): Config? {
            Log.i(TAG, "Getting previously saved config.")
            try {
                val fileReader = FileReader(file)
                val bufferedReader = BufferedReader(fileReader)
                val stringBuilder = StringBuilder()
                var line = bufferedReader.readLine()
                while (line != null) {
                    stringBuilder.append("${line}\n")
                    line = bufferedReader.readLine()
                }
                bufferedReader.close()

                val response = stringBuilder.toString()
                return getConfigFromJSON(response)
            } catch (ex: Exception) {
                Log.e(TAG, "Error on reading previous config.")
                return null
            }
        }

        /**
         * Auxiliary method to read and load a config `.json` file from assets folder
         * @param context Application context
         * @return `.json` file converted to string
         */
        private fun getJsonFromAssets(context: Context): String? {
            val filePath = "config/default-config.json"
            val jsonString: String = try {
                val inputStream = context.assets.open(filePath)
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                String(buffer, Charsets.UTF_8)
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
            return jsonString
        }

        /**
         * Auxiliary method to parse values from the config `.json`
         * file and create a `Config` object
         * @param jsonString `.json` file converted to String
         * @return `Config` object
         */
        private fun getConfigFromJSON(jsonString: String): Config? {
            val configJSON = JSONObject(jsonString)
            try {
                val remoteConfigId = if (configJSON.optString("remote_config_id") != "" ||
                    configJSON.optString("remote_config_id") != "[]" ||
                    configJSON.optString("remote_config_id") != "null"
                ) {
                    configJSON.optString("remote_config_id")
                } else {
                    FMUtility.Constants.defaultConfigId
                }
                val frameAcceptanceThresholdTimeout =
                    configJSON.getString("frame_acceptance_threshold_timeout")
                val isBehaviorRequesterEnabled =
                    configJSON.getBoolean("is_behavior_requester_enabled")
                val isTrackingStateFilterEnabled =
                    configJSON.getBoolean("is_tracking_state_filter_enabled")
                val isMovementFilterEnabled = configJSON.getBoolean("is_movement_filter_enabled")
                val movementFilterThreshold = configJSON.getString("movement_filter_threshold")
                val isBlurFilterEnabled = configJSON.getBoolean("is_blur_filter_enabled")
                val blurFilterVarianceThreshold =
                    configJSON.getString("blur_filter_variance_threshold")
                val blurFilterSuddenDropThreshold =
                    configJSON.getString("blur_filter_sudden_drop_threshold")
                val blurFilterAverageThroughputThreshold =
                    configJSON.getString("blur_filter_average_throughput_threshold")
                val isCameraPitchFilterEnabled =
                    configJSON.getBoolean("is_camera_pitch_filter_enabled")
                val cameraPitchFilterMaxUpwardTilt =
                    configJSON.getString("camera_pitch_filter_max_upward_tilt")
                val cameraPitchFilterMaxDownwardTilt =
                    configJSON.getString("camera_pitch_filter_max_downward_tilt")
                val isImageQualityFilterEnabled =
                    configJSON.getBoolean("is_image_quality_filter_enabled")
                val imageQualityFilterScoreThreshold =
                    configJSON.getString("image_quality_filter_score_threshold")

                var imageQualityFilterModelUri: String? = null
                var imageQualityFilterModelVersion: String? = null
                try {
                    imageQualityFilterModelUri =
                        configJSON.getString("image_quality_filter_model_uri")
                    imageQualityFilterModelVersion =
                        configJSON.getString("image_quality_filter_model_version")
                } catch (e: JSONException) {
                    Log.i(TAG, "No model specified in remote config")
                }

                val config = Config(
                    remoteConfigId,
                    frameAcceptanceThresholdTimeout.toFloat(),
                    isBehaviorRequesterEnabled,
                    isTrackingStateFilterEnabled,
                    isMovementFilterEnabled,
                    movementFilterThreshold.toFloat(),
                    isBlurFilterEnabled,
                    blurFilterVarianceThreshold.toFloat(),
                    blurFilterSuddenDropThreshold.toFloat(),
                    blurFilterAverageThroughputThreshold.toFloat(),
                    isCameraPitchFilterEnabled,
                    cameraPitchFilterMaxUpwardTilt.toFloat(),
                    cameraPitchFilterMaxDownwardTilt.toFloat(),
                    isImageQualityFilterEnabled,
                    imageQualityFilterScoreThreshold.toFloat(),
                    imageQualityFilterModelUri,
                    imageQualityFilterModelVersion
                )

                return config

            } catch (e: JSONException) {
                Log.e(TAG, "Error Decoding Remote Json")
                return null
            }
        }
    }
}