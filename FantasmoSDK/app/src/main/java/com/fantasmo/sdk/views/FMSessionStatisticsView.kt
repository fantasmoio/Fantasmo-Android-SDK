package com.fantasmo.sdk.views

import android.graphics.Color
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.config.RemoteConfig.Companion.remoteConfig
import com.fantasmo.sdk.fantasmosdk.BuildConfig
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.google.ar.core.TrackingFailureReason

/**
 * Debug View responsible for displaying the Statistics generated during the session.
 */
class FMSessionStatisticsView(arLayout: CoordinatorLayout) {

    private val TAG = FMSessionStatisticsView::class.java.simpleName
    private var sdkVersion: TextView = arLayout.findViewById(R.id.fantasmoSDKView)
    private var statusTv: TextView = arLayout.findViewById(R.id.statusTextView)
    private var lastResultTv: TextView = arLayout.findViewById(R.id.lastResultTextView)
    private var localizeTimeTv: TextView = arLayout.findViewById(R.id.localizeTimeTextView)
    private var uploadTimeTv: TextView = arLayout.findViewById(R.id.uploadTimeTextView)
    private var deviceLocationTv: TextView = arLayout.findViewById(R.id.deviceLocationTextView)

    private var cameraTranslationTv: TextView = arLayout.findViewById(R.id.translationTextView)
    private var distanceTravelledTv: TextView =
        arLayout.findViewById(R.id.distanceTravelledTextView)

    private var cameraAnglesTv: TextView = arLayout.findViewById(R.id.cameraAnglesTextView)
    private var cameraAnglesSpreadTv: TextView =
        arLayout.findViewById(R.id.cameraAnglesSpreadTextView)

    private var remoteConfigTv: TextView = arLayout.findViewById(R.id.remoteConfigTextView)

    private var normalTv: TextView = arLayout.findViewById(R.id.normalTextView)
    private var limitedTv: TextView = arLayout.findViewById(R.id.limitedTextView)
    private var notAvailableTv: TextView = arLayout.findViewById(R.id.notAvailableTextView)
    private var excessiveMotionTv: TextView = arLayout.findViewById(R.id.excessiveMotionTextView)
    private var insufficientFeaturesTv: TextView =
        arLayout.findViewById(R.id.insufficientFeaturesTextView)
    private var pitchLowTv: TextView = arLayout.findViewById(R.id.pitchLowTextView)
    private var pitchHighTv: TextView = arLayout.findViewById(R.id.pitchHighTextView)
    private var blurryTv: TextView = arLayout.findViewById(R.id.blurryTextView)
    private var tooFastTv: TextView = arLayout.findViewById(R.id.tooFastTextView)
    private var tooLittleTv: TextView = arLayout.findViewById(R.id.tooLittleTextView)
    private var featuresTv: TextView = arLayout.findViewById(R.id.featuresTextView)

    private var imageQualityModelVersion: TextView = arLayout.findViewById(R.id.imageQualityVersionTextview)
    private var imageQualityInsufficient: TextView = arLayout.findViewById(R.id.imageQualityInsufficientTextView)
    private var imageQualityLastResult: TextView = arLayout.findViewById(R.id.lastResultIQTextView)
    private var imageGammaCorrection: TextView = arLayout.findViewById(R.id.imageGammaCorrection)

    private var frameErrorTv: TextView = arLayout.findViewById(R.id.frameErrorTextView)

    fun updateStats(
        fmFrame: FMFrame,
        info: AccumulatedARCoreInfo,
        rejections: FrameFilterRejectionStatistics
    ) {
        val cameraTranslation = fmFrame.androidSensorPose?.translation
        cameraTranslationTv.text =
            createStringDisplay(cameraTranslation)

        val cameraRotation = fmFrame.androidSensorPose?.rotationQuaternion
        cameraAnglesTv.text = createStringDisplay(cameraRotation)

        normalTv.text =
            info.trackingStateFrameStatistics.framesWithNormalTrackingState.toString()
        limitedTv.text =
            info.trackingStateFrameStatistics.framesWithLimitedTrackingState.toString()
        notAvailableTv.text =
            info.trackingStateFrameStatistics.framesWithNotAvailableTracking.toString()
        excessiveMotionTv.text =
            if (info.trackingStateFrameStatistics.framesWithLimitedTrackingStateByReason[TrackingFailureReason.EXCESSIVE_MOTION] == null) {
                "0"
            } else {
                info.trackingStateFrameStatistics.framesWithLimitedTrackingStateByReason[TrackingFailureReason.EXCESSIVE_MOTION].toString()
            }
        insufficientFeaturesTv.text =
            if (info.trackingStateFrameStatistics.framesWithLimitedTrackingStateByReason[TrackingFailureReason.INSUFFICIENT_FEATURES] == null) {
                "0"
            } else {
                info.trackingStateFrameStatistics.framesWithLimitedTrackingStateByReason[TrackingFailureReason.INSUFFICIENT_FEATURES].toString()
            }

        pitchLowTv.text = rejections.excessiveTiltFrameCount.toString()
        pitchHighTv.text = rejections.insufficientTiltFrameCount.toString()
        blurryTv.text = rejections.excessiveBlurFrameCount.toString()
        tooFastTv.text = rejections.excessiveMotionFrameCount.toString()
        tooLittleTv.text = rejections.insufficientMotionFrameCount.toString()
        featuresTv.text = rejections.insufficientFeatures.toString()

        //TODO - Fix this
//        imageQualityModelVersion.text = info.modelVersion
//        imageQualityLastResult.text = String.format("%.5f", info.lastImageQualityScore)
//        imageQualityInsufficient.text = rejections.imageQualityFrameCount.toString()

        frameErrorTv.text = rejections.frameErrorCount.toString()

        val stringDistance =
            String.format("%.2f", info.translationAccumulator.totalTranslation) + " m"
        distanceTravelledTv.text = stringDistance
        val stringSpread =
            "[${info.rotationAccumulator.yaw[0]},${info.rotationAccumulator.yaw[1]}],${info.rotationAccumulator.yaw[2]}\n" +
                    "[${info.rotationAccumulator.pitch[0]},${info.rotationAccumulator.pitch[1]}],${info.rotationAccumulator.pitch[2]}\n" +
                    "[${info.rotationAccumulator.roll[0]},${info.rotationAccumulator.roll[1]}],${info.rotationAccumulator.roll[2]}"
        cameraAnglesSpreadTv.text = stringSpread
        val gamma = fmFrame.enhancedImageGamma
        imageGammaCorrection.text = if(gamma != null) {
            String.format("%.3f", gamma)
        } else {
            "None"
        }
    }

    fun updateState(didChangeState: FMLocationManager.State) {
        when (didChangeState) {
            FMLocationManager.State.LOCALIZING -> {
                statusTv.setTextColor(Color.GREEN)
            }
            FMLocationManager.State.UPLOADING -> {
                statusTv.setTextColor(Color.RED)
            }
            else -> {
                statusTv.setTextColor(Color.BLACK)
            }
        }
        statusTv.text = didChangeState.toString()
    }

    private var localizingStart: Long = System.currentTimeMillis()
    private var uploadingStart: Long = System.currentTimeMillis()

    fun updateResult(result: FMLocationResult) {
        val stringResult =
            "${result.location.coordinate.latitude},\n${result.location.coordinate.longitude} (${result.confidence})"
        lastResultTv.text = stringResult

        val elapsedUploading = (System.currentTimeMillis() - uploadingStart) / 1_000.0
        val stringUploadTime = String.format("%.2f", elapsedUploading) + "s"
        uploadTimeTv.text = stringUploadTime
        uploadingStart = System.currentTimeMillis()

        val elapsedLocalizing = (System.currentTimeMillis() - localizingStart) / 1_000.0
        val stringLocalizeTime = String.format("%.2f", elapsedLocalizing) + "s"
        localizeTimeTv.text = stringLocalizeTime
        localizingStart = System.currentTimeMillis()
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        val stringResult = "$latitude,$longitude"
        deviceLocationTv.text = stringResult
    }

    /**
     * Method to simplify task of creating a String to be shown in the screen
     * @param cameraAttr `FloatArray` of values and with size that equals 3
     */
    private fun createStringDisplay(cameraAttr: FloatArray?): String {
        return String.format("%.2f", cameraAttr?.get(0)) + ", " +
                String.format("%.2f", cameraAttr?.get(1)) + ", " +
                String.format("%.2f", cameraAttr?.get(2))
    }

    fun reset() {
        val fantasmo = "Fantasmo SDK: " + BuildConfig.VERSION_NAME
        sdkVersion.text = fantasmo
        val stringZero = "0"
        val stringZeroS = "0,0s"
        val stringClear = ""

        remoteConfigTv.text = remoteConfig.remoteConfigId

        lastResultTv.text = stringZero
        localizeTimeTv.text = stringZeroS
        uploadTimeTv.text = stringZeroS

        deviceLocationTv.text = stringClear
        cameraTranslationTv.text = stringClear
        distanceTravelledTv.text = stringClear
        cameraAnglesTv.text = stringClear
        cameraAnglesSpreadTv.text = stringZero

        normalTv.text = stringZero
        limitedTv.text = stringZero
        notAvailableTv.text = stringZero
        excessiveMotionTv.text = stringZero
        insufficientFeaturesTv.text = stringZero
        pitchLowTv.text = stringZero
        pitchHighTv.text = stringZero
        blurryTv.text = stringZero
        tooFastTv.text = stringZero
        tooLittleTv.text = stringZero
        featuresTv.text = stringZero
        frameErrorTv.text = stringZero

        imageQualityModelVersion.text = stringClear
        imageQualityLastResult.text = stringZero
        imageQualityInsufficient.text = stringZero
    }
}