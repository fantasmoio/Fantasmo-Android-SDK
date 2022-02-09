package com.fantasmo.sdk.config

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
    @Test
    fun testRemoteConfigNullId() {
        RemoteConfig.updateConfig(InstrumentationRegistry.getInstrumentation().context, "null")
        assertEquals(RemoteConfig.remoteConfig.remoteConfigId, remoteConfig.remoteConfigId)
    }

    @Test
    fun testRemoteConfigEmptyArray() {
        RemoteConfig.updateConfig(InstrumentationRegistry.getInstrumentation().context, "[]")
        assertEquals(RemoteConfig.remoteConfig.remoteConfigId, remoteConfig.remoteConfigId)
    }

}