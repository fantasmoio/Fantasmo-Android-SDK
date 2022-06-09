package com.fantasmo.sdk.models.analytics

import com.fantasmo.sdk.FMDeviceAndHostInfo
import com.fantasmo.sdk.evaluators.FMFrameEvaluationType
import com.fantasmo.sdk.models.FMFrameRejectionReason
import com.fantasmo.sdk.models.Location

class FMImageQualityUserInfo(modelVersion: String?, val error: String? = null) {
    val modelVersion : String = modelVersion ?: ""
}

internal data class FMImageEnhancementInfo (val gamma: Float)

internal data class FMRotationSpread (val pitch: Float, val yaw: Float, val roll: Float)

internal data class FMLegacyFrameEvents (val excessiveTilt: Int,
                                val excessiveBlur: Int,
                                val excessiveMotion: Int,
                                val insufficientFeatures: Int,
                                val lossOfTracking: Int,
                                val total: Int)


internal data class FMFrameResolution (val height: Int,
                              val width: Int)


internal data class FMLocalizationAnalytics (
    val appSessionId: String?,
    val appSessionTags: List<String>?,
    val localizationSessionId: String?,
    val legacyFrameEvents: FMLegacyFrameEvents,
    val rotationSpread: FMRotationSpread,
    val totalDistance: Float,
    val magneticField: MagneticField?,
    val imageEnhancementInfo: FMImageEnhancementInfo?,
    val remoteConfigId: String
)

internal data class FMSessionFrameEvaluations (
    val count: Int,
    val type: FMFrameEvaluationType,
    val highestScore: Float,
    val lowestScore: Float,
    val averageScore : Float,
    val averageTime : Float,
    val imageQualityUserInfo: FMImageQualityUserInfo?
    )

internal data class FMSessionFrameRejections (
    val count : Int,
    val rejectionReasons : Map<FMFrameRejectionReason, Int>
    )

internal data class FMSessionAnalytics (
    val localizationSessionId: String,
    val appSessionId: String,
    val appSessionTags: List<String>,
    val totalFrames: Int,
    val totalFramesUploaded: Int,
    val frameEvaluations: FMSessionFrameEvaluations,
    val frameRejections: FMSessionFrameRejections,
    val locationResultCount: Int,
    val errorResultCount: Int,
    val totalTranslation: Float,
    val rotationSpread: FMRotationSpread,
    val timestamp: Float,
    val totalDuration: Float,
    val location: Location,
    val remoteConfigId: String,
    val qrCodeSkipped : Boolean = false,
    @Transient
    private val deviceAndHostInfo: FMDeviceAndHostInfo
) {
    val udid: String = deviceAndHostInfo.udid
    val deviceModel: String = deviceAndHostInfo.deviceModel
    val deviceOs: String = deviceAndHostInfo.deviceOs
    val deviceOsVersion: String = deviceAndHostInfo.deviceOsVersion
    val sdkVersion: String = deviceAndHostInfo.sdkVersion
    val hostAppBundleIdentifier: String = deviceAndHostInfo.hostAppBundleIdentifier
    val hostAppMarketingVersion: String = deviceAndHostInfo.hostAppMarketingVersion
    val hostAppBuild: String = deviceAndHostInfo.hostAppBuild
}