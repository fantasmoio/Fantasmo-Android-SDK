package com.fantasmo.sdk.config

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

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

        var defaultConfig : RemoteConfig.Config
        init {
            val filePath = "config/default-config.json"
            val jsonString: String = try {
                val inputStream = InstrumentationRegistry.getInstrumentation().context.assets.open(filePath)
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                String(buffer, Charsets.UTF_8)
            } catch (e: IOException) {
                ""
            }
            defaultConfig = Gson().fromJson(jsonString, RemoteConfig.Config::class.java)
        }
    }

    @Test
    fun testUseStoredConfigWhenRemoteConfigIdIsNull() {
        RemoteConfig.updateConfig(InstrumentationRegistry.getInstrumentation().context, "null")

        // Fancy Java reflection stuff to get all fields in the class from kotlin
        val fields = RemoteConfig.Config::class.java.declaredFields
        for(field in fields) {
            field.isAccessible = true
            assertEquals(field.get(RemoteConfig.remoteConfig), field.get(defaultConfig))
        }
    }

    @Test
    fun testUseStoredConfigWhenRemoteConfigIdIsArray() {
        RemoteConfig.updateConfig(InstrumentationRegistry.getInstrumentation().context, "[]")
        
        // Fancy Java reflection stuff to get all fields in the class from kotlin
        val fields = RemoteConfig.Config::class.java.declaredFields
        for(field in fields) {
            field.isAccessible = true
            assertEquals(field.get(RemoteConfig.remoteConfig), field.get(defaultConfig))
        }
    }
}