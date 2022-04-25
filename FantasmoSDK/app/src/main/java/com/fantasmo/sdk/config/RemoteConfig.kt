package com.fantasmo.sdk.config

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.FMUtility.Constants.fileName
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.StringBuilder


class RemoteConfig {

    data class Config(
        @SerializedName("remote_config_id")
        var remoteConfigId: String,
        @SerializedName("is_behavior_requester_enabled")
        var isBehaviorRequesterEnabled: Boolean,
        @SerializedName("is_tracking_state_filter_enabled")
        var isTrackingStateFilterEnabled: Boolean,
        @SerializedName("is_movement_filter_enabled")
        var isMovementFilterEnabled: Boolean,
        @SerializedName("movement_filter_threshold")
        var movementFilterThreshold: Float,
        @SerializedName("is_camera_pitch_filter_enabled")
        var isCameraPitchFilterEnabled: Boolean,
        @SerializedName("camera_pitch_filter_max_upward_tilt")
        var cameraPitchFilterMaxUpwardTilt: Float,
        @SerializedName("camera_pitch_filter_max_downward_tilt")
        var cameraPitchFilterMaxDownwardTilt: Float,
        @SerializedName("is_image_enhancer_enabled")
        var isImageEnhancerEnabled: Boolean,
        @SerializedName("image_enhancer_target_brightness")
        var imageEnhancerTargetBrightness: Float,
        @SerializedName("image_quality_filter_model_uri")
        var imageQualityFilterModelUri: String?,
        @SerializedName("image_quality_filter_model_version")
        var imageQualityFilterModelVersion: String?,
        @SerializedName("min_localization_window_time")
        var minLocalizationWindowTime: Float,
        @SerializedName("max_localization_window_time")
        var maxLocalizationWindowTime: Float,
        @SerializedName("min_frame_evaluation_score")
        var minFrameEvaluationScore: Float,
        @SerializedName("min_frame_evaluation_high_quality_score")
        var minFrameEvaluationHighQualityScore: Float
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
            remoteConfig = if (config == null || !validateConfig(config)) {
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
            return try {
                Gson().fromJson(jsonString, Config::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Error Decoding Remote Json")
                null
            }
        }

        private fun validateConfig(config: Config) : Boolean {
            if(config.minLocalizationWindowTime == 0f)
                return false
            if(config.maxLocalizationWindowTime == 0f)
                return false
            if(config.minFrameEvaluationScore == 0f)
                return false
            if(config.minFrameEvaluationHighQualityScore == 0f)
                return false
            return true
        }
    }
}
