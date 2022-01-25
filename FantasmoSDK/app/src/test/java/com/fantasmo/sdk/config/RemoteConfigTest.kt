package com.fantasmo.sdk.config

class RemoteConfigTest {

    companion object {
        val remoteConfig = RemoteConfig.Config(
            remoteConfigId = "default-android_17.01.22",
            frameAcceptanceThresholdTimeout = 1.0f,
            isBehaviorRequesterEnabled = false,
            isTrackingStateFilterEnabled = true,
            isMovementFilterEnabled = true,
            movementFilterThreshold = 0.001f,
            isBlurFilterEnabled = false,
            blurFilterVarianceThreshold = 250.0f,
            blurFilterSuddenDropThreshold = 0.4f,
            blurFilterAverageThroughputThreshold = 0.25f,
            isCameraPitchFilterEnabled = true,
            cameraPitchFilterMaxUpwardTilt = 30.0f,
            cameraPitchFilterMaxDownwardTilt = 65.0f,
            isImageQualityFilterEnabled = false,
            imageQualityFilterScoreThreshold = 0.0f,
            imageQualityFilterModelUri = null,
            imageQualityFilterModelVersion = "0.1.0"
        )

        val remoteConfigDisabledFilters = RemoteConfig.Config(
            remoteConfigId = "default-android_17.01.22",
            frameAcceptanceThresholdTimeout = 1.0f,
            isBehaviorRequesterEnabled = false,
            isTrackingStateFilterEnabled = false,
            isMovementFilterEnabled = false,
            movementFilterThreshold = 0.001f,
            isBlurFilterEnabled = false,
            blurFilterVarianceThreshold = 250.0f,
            blurFilterSuddenDropThreshold = 0.4f,
            blurFilterAverageThroughputThreshold = 0.25f,
            isCameraPitchFilterEnabled = false,
            cameraPitchFilterMaxUpwardTilt = 30.0f,
            cameraPitchFilterMaxDownwardTilt = 65.0f,
            isImageQualityFilterEnabled = false,
            imageQualityFilterScoreThreshold = 0.0f,
            imageQualityFilterModelUri = null,
            imageQualityFilterModelVersion = "0.1.0"
        )
    }
}