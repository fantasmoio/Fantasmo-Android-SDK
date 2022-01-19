package com.fantasmo.sdk.config

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


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
         * Overrides the default config with a remote Config
         */
        fun updateConfig(jsonString: String) {
            Log.i(TAG, "Received Remote Config.")
            remoteConfig = getConfigFromJSON(jsonString)
        }

        /**
         * Loads the default config from the assets folder
         * @param context Application context
         * @return Config SDK configuration
         */
        fun getDefaultConfig(context: Context): Config? {
            Log.i(TAG, "Getting default config.")
            val jsonFileString = getJsonFromAssets(context)
            if (jsonFileString != null) {
                return getConfigFromJSON(jsonFileString)
            }
            return null
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
         * file and create a Config object
         * @param jsonString `.json` file converted to String
         * @return Config object
         */
        private fun getConfigFromJSON(jsonString: String): Config {
            val configJSON = JSONObject(jsonString)
            val remoteConfigId = configJSON.optString("remote_config_id")
            val frameAcceptanceThresholdTimeout =
                configJSON.getString("frame_acceptance_threshold_timeout")
            val isBehaviorRequesterEnabled = configJSON.getBoolean("is_behavior_requester_enabled")
            val isTrackingStateFilterEnabled =
                configJSON.getBoolean("is_tracking_state_filter_enabled")
            val isMovementFilterEnabled = configJSON.getBoolean("is_movement_filter_enabled")
            val movementFilterThreshold = configJSON.getString("movement_filter_threshold")
            val isBlurFilterEnabled = configJSON.getBoolean("is_blur_filter_enabled")
            val blurFilterVarianceThreshold = configJSON.getString("blur_filter_variance_threshold")
            val blurFilterSuddenDropThreshold =
                configJSON.getString("blur_filter_sudden_drop_threshold")
            val blurFilterAverageThroughputThreshold =
                configJSON.getString("blur_filter_average_throughput_threshold")
            val isCameraPitchFilterEnabled = configJSON.getBoolean("is_camera_pitch_filter_enabled")
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
                imageQualityFilterModelUri = configJSON.getString("image_quality_filter_model_uri")
                imageQualityFilterModelVersion =
                    configJSON.getString("image_quality_filter_model_version")
            } catch (e: JSONException) {
                Log.d(TAG, "No model specified in remote config")
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
            remoteConfig = config
            return config
        }
    }
}