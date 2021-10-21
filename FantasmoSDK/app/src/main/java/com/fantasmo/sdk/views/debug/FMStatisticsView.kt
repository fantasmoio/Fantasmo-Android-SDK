package com.fantasmo.sdk.views.debug

import android.graphics.Color
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.fantasmosdk.R
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.google.ar.core.Frame

class FMStatisticsView(arLayout: CoordinatorLayout) {

    private lateinit var filterRejectionTv: TextView
    private lateinit var anchorDeltaTv: TextView
    private var cameraTranslationTv: TextView = arLayout.findViewById(R.id.translationTextView)
    private var cameraAnglesTv: TextView = arLayout.findViewById(R.id.cameraAnglesTextView)
    private var lastResultTv: TextView = arLayout.findViewById(R.id.lastResultTextView)

    private var statusTv: TextView = arLayout.findViewById(R.id.statusTextView)
    private var localizeTv: TextView = arLayout.findViewById(R.id.localizeTimeTextView)
    private var uploadTv: TextView = arLayout.findViewById(R.id.uploadTimeTextView)
    private var distanceTravelledTv: TextView = arLayout.findViewById(R.id.distanceTravelledTextView)
    private var cameraAnglesSpreadTv: TextView = arLayout.findViewById(R.id.cameraAnglesSpreadTextView)
    private var normalTv: TextView = arLayout.findViewById(R.id.normalTextView)
    private var limitedTv: TextView = arLayout.findViewById(R.id.limitedTextView)
    private var notAvailableTv: TextView = arLayout.findViewById(R.id.notAvailableTextView)
    private var excessiveMotionTv: TextView = arLayout.findViewById(R.id.excessiveMotionTextView)
    private var insufficientFeaturesTv: TextView = arLayout.findViewById(R.id.insufficientFeaturesTextView)
    private var pitchLowTv: TextView = arLayout.findViewById(R.id.pitchLowTextView)
    private var pitchHighTv: TextView = arLayout.findViewById(R.id.pitchHighTextView)
    private var blurryTv: TextView = arLayout.findViewById(R.id.blurryTextView)
    private var tooFastTv: TextView = arLayout.findViewById(R.id.tooFastTextView)
    private var tooLittleTv: TextView = arLayout.findViewById(R.id.tooLittleTextView)
    private var featuresTv: TextView = arLayout.findViewById(R.id.featuresTextView)
    private var deviceLocationTv: TextView = arLayout.findViewById(R.id.deviceLocationTextView)

    fun updateStats(frame: Frame, info: AccumulatedARCoreInfo, rejections: FrameFilterRejectionStatistics) {
        val cameraTranslation = frame.androidSensorPose?.translation
        cameraTranslationTv.text =
            createStringDisplay(cameraTranslation)

        val cameraRotation = frame.androidSensorPose?.rotationQuaternion
        cameraAnglesTv.text = createStringDisplay(cameraRotation)

        normalTv.text =
            info.trackingStateFrameStatistics.framesWithNormalTrackingState.toString()
        limitedTv.text =
            info.trackingStateFrameStatistics.framesWithLimitedTrackingState.toString()
        notAvailableTv.text =
            info.trackingStateFrameStatistics.framesWithNotAvailableTracking.toString()
        excessiveMotionTv.text = rejections.excessiveMotionFrameCount.toString()
        insufficientFeaturesTv.text = rejections.insufficientFeatures.toString()
        pitchLowTv.text = rejections.excessiveTiltFrameCount.toString()
        pitchHighTv.text = rejections.insufficientTiltFrameCount.toString()
        blurryTv.text = rejections.excessiveBlurFrameCount.toString()
        //tooFastTv.text = rejections.excessiveMotionFrameCount.toString()
        tooLittleTv.text = rejections.insufficientMotionFrameCount.toString()
        //featuresTv.text = rejections.insufficientFeatures.toString()
        val stringDistance = String.format("%.2f", info.translationAccumulator.totalTranslation) + " m"
        distanceTravelledTv.text = stringDistance
        val stringSpread =
            "[${info.rotationAccumulator.yaw[0]},${info.rotationAccumulator.yaw[1]}],${info.rotationAccumulator.yaw[2]}\n" +
                    "[${info.rotationAccumulator.pitch[0]},${info.rotationAccumulator.pitch[1]}],${info.rotationAccumulator.pitch[2]}\n" +
                    "[${info.rotationAccumulator.roll[0]},${info.rotationAccumulator.roll[1]}],${info.rotationAccumulator.roll[2]}"
        cameraAnglesSpreadTv.text = stringSpread
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

    fun updateResult(result: FMLocationResult) {
        val stringResult =
            "${result.location.coordinate.latitude},\n${result.location.coordinate.longitude} (${result.confidence})"
        lastResultTv.text = stringResult
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        val stringResult = "$latitude,$longitude"
        deviceLocationTv.text = stringResult
    }

    /**
     * Method to simplify task of creating a String to be shown in the screen
     * */
    private fun createStringDisplay(cameraAttr: FloatArray?): String {
        return String.format("%.2f", cameraAttr?.get(0)) + ", " +
                String.format("%.2f", cameraAttr?.get(1)) + ", " +
                String.format("%.2f", cameraAttr?.get(2))
    }
}