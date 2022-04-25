package com.fantasmo.sdk

import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FMFrameEvaluationStatistics

/**
 * Class that describes the confidence level of a Localization Result
 */
enum class FMResultConfidence{
    LOW,
    MEDIUM,
    HIGH;

    fun description():String{
        return when(this){
            LOW -> "Low"
            MEDIUM -> "Medium"
            HIGH -> "High"
        }
    }

    fun abbreviation():String{
        return when(this){
            LOW -> "L"
            MEDIUM -> "M"
            HIGH -> "H"
        }
    }
}

/**
 * Class that holds a Localization Result with a location result,
 * confidence level result and the zones on it was performed.
 */
class FMLocationResult(
    var location: Location,
    var confidence: FMResultConfidence,
    var zones: List<FMZone>
    )

/**
 * Class that describes the behavior that a user should do in order
 * to get more accurate and correct frames to analyze.
 */
enum class FMBehaviorRequest(val description: String) {
    POINT_AT_BUILDINGS("Point at stores, signs and buildings around you to get a precise location"),
    TILT_UP("Tilt your device up"),
    TILT_DOWN("Tilt your device down"),
    PAN_AROUND("Pan around the scene"),
    PAN_SLOWLY("Pan more slowly");
}

/**
 * The methods that you use to receive events from an associated
 * location manager object.
 */
interface FMLocationListener {
     /**
     * Tells the listener that a new frame upload is starting.
     * @param frame `FMFrame` that is getting uploaded
     */

    fun didBeginUpload(frame: FMFrame) {}

    /**
     * Tells the listener that new location data is available.
     * @param result Location of the device (or anchor if set)
     */
    fun didUpdateLocation(result: FMLocationResult)

    /**
     * Tells the listener that an error has occurred.
     * @param error The error reported.
     * @param metadata Metadata related t the error.
     */
    fun didFailWithError(error: ErrorResponse, metadata: Any?)

    /**
     * Tells the listener that a request behavior has occurred.
     * @param behavior The behavior reported.
     */
    fun didRequestBehavior(behavior: FMBehaviorRequest){}

    /**
     * Tells the listener that the `FMLocationManager` has changed state
     * @param state The new state of the `FMLocationManager` instance
     */
    fun didChangeState(state: FMLocationManager.State){}

    /**
     * Tells the listener that there's an update on AR info
     * @param frame current AR frame
     * @param info `AccumulatedARCoreInfo` with all the info about movement and rotation
     */
    fun didUpdateFrame(frame: FMFrame, info: AccumulatedARCoreInfo) {}

    /**
     * Tells the listener that there's an update on the statistics
     * @param frameEvaluationStatistics `FMFrameEvaluationStatistics` with all the statistics
     */
    fun didUpdateFrameEvaluationStatistics(frameEvaluationStatistics: FMFrameEvaluationStatistics) {}
}
