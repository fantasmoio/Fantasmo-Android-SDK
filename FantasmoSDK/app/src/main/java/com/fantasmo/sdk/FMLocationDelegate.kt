package com.fantasmo.sdk

import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.google.ar.core.Frame

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
    POINTATBUILDINGS("Point at stores, signs and buildings around you to get a precise location"),
    TILTUP("Tilt your device up"),
    TILTDOWN("Tilt your device down"),
    PANAROUND("Pan around the scene"),
    PANSLOWLY("Pan more slowly");
}

/**
 * The methods that you use to receive events from an associated
 * location manager object.
 */
interface FMLocationListener {

    /**
     * Tells the listener that new location data is available.
     * @param result Location of the device (or anchor if set)
     */
    fun locationManager(result: FMLocationResult)

    /**
     * Tells the listener that an error has occurred.
     * @param error The error reported.
     * @param metadata Metadata related t the error.
     */
    fun locationManager(error: ErrorResponse, metadata: Any?)

    /**
     * Tells the listener that a request behavior has occurred.
     * @param didRequestBehavior The behavior reported.
     */
    fun locationManager(didRequestBehavior: FMBehaviorRequest){}

    /**
     * Tells the listener that the `FMLocationManager` has changed state
     * @param didChangeState The new state of the `FMLocationManager` instance
     */
    fun locationManager(didChangeState: FMLocationManager.State){}

    /**
     * Tells the listener that there's an update on the statistics
     * @param didUpdateFrame current AR frame
     * @param info `AccumulatedARCoreInfo` with all the statistics about movement and rotation
     * @param rejections `FrameFilterRejectionsStatistics with all the statistics regarding frame rejection
     */
    fun locationManager(didUpdateFrame: Frame, info: AccumulatedARCoreInfo, rejections: FrameFilterRejectionStatistics) {}
}
