package com.fantasmo.sdk.config

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException


class RemoteConfig {

    private val TAG = RemoteConfig::class.java.simpleName

    data class Config(
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

    fun updateConfig(context: Context): Config? {
        val jsonFileString = getJsonFromAssets(context)
        if (jsonFileString != null) {
            val configJsonObject = JSONObject(jsonFileString)
            val parkingInRadius = configJsonObject.getBoolean("parking_in_radius")
            if (!parkingInRadius) {
                val parkingReason = configJsonObject.getString("fantasmo_unavailable_reason")
                Log.e(TAG, "Failed Parking with Reason:$parkingReason")
            }
            val configJSON = configJsonObject.getString("config")

            return getConfig(configJSON)
        }

        return null
    }

    private fun getConfig(configJSON: String): Config {
        val config = JSONObject(configJSON)
        val frameAcceptanceThresholdTimeout = config.getString("frame_acceptance_threshold_timeout")
        val isBehaviorRequesterEnabled = config.getBoolean("is_behavior_requester_enabled")
        val isTrackingStateFilterEnabled = config.getBoolean("is_tracking_state_filter_enabled")
        val isMovementFilterEnabled = config.getBoolean("is_movement_filter_enabled")
        val movementFilterThreshold = config.getString("movement_filter_threshold")
        val isBlurFilterEnabled = config.getBoolean("is_blur_filter_enabled")
        val blurFilterVarianceThreshold = config.getString("blur_filter_variance_threshold")
        val blurFilterSuddenDropThreshold = config.getString("blur_filter_sudden_drop_threshold")
        val blurFilterAverageThroughputThreshold =
            config.getString("blur_filter_average_throughput_threshold")
        val isCameraPitchFilterEnabled = config.getBoolean("is_camera_pitch_filter_enabled")
        val cameraPitchFilterMaxUpwardTilt = config.getString("camera_pitch_filter_max_upward_tilt")
        val cameraPitchFilterMaxDownwardTilt =
            config.getString("camera_pitch_filter_max_downward_tilt")
        val isImageQualityFilterEnabled = config.getBoolean("is_image_quality_filter_enabled")
        val imageQualityFilterScoreThreshold =
            config.getString("image_quality_filter_score_threshold")
        val imageQualityFilterModelUri = config.getString("image_quality_filter_model_uri")
        val imageQualityFilterModelVersion = config.getString("image_quality_filter_model_version")

        val configObj = Config(
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
        Log.d(TAG, configObj.toString())
        return configObj
    }
}