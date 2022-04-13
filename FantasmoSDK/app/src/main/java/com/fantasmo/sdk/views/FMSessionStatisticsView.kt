package com.fantasmo.sdk.views

import android.graphics.Color
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.config.RemoteConfig.Companion.remoteConfig
import com.fantasmo.sdk.fantasmosdk.BuildConfig
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.filters.FMFrameFilterRejectionReason
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FMFrameEvaluationStatistics
import com.google.ar.core.TrackingFailureReason

/**
 * Debug View responsible for displaying the Statistics generated during the session.
 */
class FMSessionStatisticsView(arLayout: CoordinatorLayout) {

    private val TAG = FMSessionStatisticsView::class.java.simpleName
    private var sdkVersion: TextView = arLayout.findViewById(R.id.fantasmoSDKView)
    private var statusTv: TextView = arLayout.findViewById(R.id.statusTextView)
    private var currentWindowTv: TextView = arLayout.findViewById(R.id.currentWindowTextView)
    private var bestScoreTv: TextView = arLayout.findViewById(R.id.bestScoreTextView)
    private var framesEvaluatedTv: TextView = arLayout.findViewById(R.id.framesEvaluatedTextView)
    private var liveScoreTv: TextView = arLayout.findViewById(R.id.liveScoreTextView)
    private var framesRejectedTv: TextView = arLayout.findViewById(R.id.framesRejectedTextView)
    private var currentRejectionTv: TextView = arLayout.findViewById(R.id.currentRejectionTextView)
    private var lastResultTv: TextView = arLayout.findViewById(R.id.lastResultTextView)
    private var errorsTv: TextView = arLayout.findViewById(R.id.errorsTextView)
    private var deviceLocationTv: TextView = arLayout.findViewById(R.id.deviceLocationTextView)
    private var translationTv: TextView = arLayout.findViewById(R.id.translationTextView)
    private var totalTranslationTv: TextView = arLayout.findViewById(R.id.totalTranslationTextView)
    private var remoteConfigTv: TextView = arLayout.findViewById(R.id.remoteConfigTextView)
    private var eulerAnglesTv: TextView = arLayout.findViewById(R.id.eulerAnglesTextView)
    private var eulerAnglesSpreadXTv: TextView = arLayout.findViewById(R.id.eulerAnglesSpreadXTextView)
    private var eulerAnglesSpreadYTv: TextView = arLayout.findViewById(R.id.eulerAnglesSpreadYTextView)
    private var eulerAnglesSpreadZTv: TextView = arLayout.findViewById(R.id.eulerAnglesSpreadZTextView)
    private var movementTooFastTv: TextView = arLayout.findViewById(R.id.movementTooFastTextView)
    private var movementTooLittleTv: TextView = arLayout.findViewById(R.id.movementTooLittleTextView)
    private var pitchTooHighTv: TextView = arLayout.findViewById(R.id.pitchTooHighTextView)
    private var pitchTooLowTv: TextView = arLayout.findViewById(R.id.pitchTooLowTextView)
    private var insufficientFeaturesTv: TextView = arLayout.findViewById(R.id.insufficientFeaturesTextView)


    private var windowStart: Double? = null

    fun updateStats(
        fmFrame: FMFrame,
        info: AccumulatedARCoreInfo
    ) {
        val cameraTranslation = fmFrame.cameraPose?.translation
        translationTv.text =
            createStringDisplay(cameraTranslation)

        val cameraRotation = fmFrame.sensorAngles
        if(cameraRotation != null)
            eulerAnglesTv.text = createStringDisplay(cameraRotation)

        insufficientFeaturesTv.text =
            if (info.trackingStateFrameStatistics.framesWithLimitedTrackingStateByReason[TrackingFailureReason.INSUFFICIENT_FEATURES] == null) {
                "0"
            } else {
                info.trackingStateFrameStatistics.framesWithLimitedTrackingStateByReason[TrackingFailureReason.INSUFFICIENT_FEATURES].toString()
            }

        val stringDistance =
            String.format("%.2f", info.translationAccumulator.totalTranslation) + " m"
        totalTranslationTv.text = stringDistance
        val stringXSpread = "(${info.rotationAccumulator.pitch[0]}°, ${info.rotationAccumulator.pitch[1]}°), ${info.rotationAccumulator.pitch[2]}°"
        val stringYSpread = "(${info.rotationAccumulator.roll[0]}°, ${info.rotationAccumulator.roll[1]}°), ${info.rotationAccumulator.roll[2]}°"
        val stringZSpread = "(${info.rotationAccumulator.yaw[0]}°, ${info.rotationAccumulator.yaw[1]}°), ${info.rotationAccumulator.yaw[2]}°"
        eulerAnglesSpreadXTv.text = stringXSpread
        eulerAnglesSpreadYTv.text = stringYSpread
        eulerAnglesSpreadZTv.text = stringZSpread
    }

    fun update(activeUploads: MutableList<FMFrame>) {
        activeUploads.forEach { frame ->
            val uploadText = "Uploading... "
            var infoText = "Score: "
            val score = frame.evaluation?.score
            infoText += if (score != null) {
                String.format("%.5f", score)
            } else {
                "n/a"
            }
            val gamma = frame.enhancedImageGamma
            if (gamma != null) {
                infoText += String.format(", Gamma: %.5f", gamma)
            }
        }
    }

    fun update(frameEvaluationStatistics: FMFrameEvaluationStatistics) {
        val window = if(frameEvaluationStatistics.windows.size == 0)
            null
        else
            frameEvaluationStatistics.windows.last()
        windowStart = window?.start
        framesEvaluatedTv.text = "${window?.evaluations ?: 0}"

        // update live score
        val liveScore = window?.currentScore
        val liveScoreText = if (liveScore != null) {
            String.format("%.5f", liveScore)
        } else {
            "n/a"
        }
        liveScoreTv.text = liveScoreText

        // update window best score
        val currentBestScore = window?.currentBestScore
        val bestScoreText = if(currentBestScore != null) {
            String.format("%.5f", currentBestScore)
        } else {
            "n/a"
        }
        bestScoreTv.text = bestScoreText

        // update window filter rejections
        framesRejectedTv.text = "${window?.rejections ?: 0}"
        currentRejectionTv.text = window?.currentRejectionReason?.name ?: ""

        // update total filter rejection counts
        pitchTooHighTv.text = "${frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.PITCH_TOO_HIGH] ?: 0}"
        pitchTooLowTv.text = "${frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.PITCH_TOO_LOW] ?: 0}"
        movementTooFastTv.text = "${frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.MOVING_TOO_FAST] ?: 0}"
        movementTooLittleTv.text = "${frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.MOVING_TOO_LITTLE] ?: 0}"
        insufficientFeaturesTv.text = "${frameEvaluationStatistics.totalRejections[FMFrameFilterRejectionReason.INSUFFICIENT_FEATURES] ?: 0}"
    }

    fun updateState(didChangeState: FMLocationManager.State) {
        when (didChangeState) {
            FMLocationManager.State.LOCALIZING -> {
                statusTv.setTextColor(Color.GREEN)
            }
            else -> {
                statusTv.setTextColor(Color.RED)
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
        val uploadTimeText = stringUploadTime
        uploadingStart = System.currentTimeMillis()

        val elapsedLocalizing = (System.currentTimeMillis() - localizingStart) / 1_000.0
        val stringLocalizeTime = String.format("%.2f", elapsedLocalizing) + "s"
        val localizeTimeText = stringLocalizeTime
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
        val fantasmo = "Fantasmo SDK " + BuildConfig.VERSION_NAME
        sdkVersion.text = fantasmo
        val stringZero = "0"
        val stringZeroS = "0.0s"
        val stringClear = ""
        val stringNA = "n/a"
        val stringZeroTranslation = "0.00, 0.00, 0.00"
        val stringZeroM = "0m"
        val stringZeroAngles = "0.00°, 0.00°, 0.00°"
        val stringZeroAngleSpread = "(0.00°, 0.00°), 0.00°"

        remoteConfigTv.text = remoteConfig.remoteConfigId

        statusTv.text = stringClear
        currentWindowTv.text = stringZeroS
        bestScoreTv.text = stringNA
        framesEvaluatedTv.text = stringZero
        liveScoreTv.text = stringNA
        framesRejectedTv.text = stringZero
        currentRejectionTv.text = stringClear
        lastResultTv.text = stringClear
        errorsTv.text = stringZero
        deviceLocationTv.text = stringClear
        translationTv.text = stringZeroTranslation
        totalTranslationTv.text = stringZeroM
        remoteConfigTv.text = stringClear
        eulerAnglesTv.text = stringZeroAngles
        eulerAnglesSpreadXTv.text = stringZeroAngleSpread
        eulerAnglesSpreadYTv.text = stringZeroAngleSpread
        eulerAnglesSpreadZTv.text = stringZeroAngleSpread
        movementTooFastTv.text = stringZero
        movementTooLittleTv.text = stringZero
        pitchTooHighTv.text = stringZero
        pitchTooLowTv.text = stringZero
        insufficientFeaturesTv.text = stringZero
    }
}
