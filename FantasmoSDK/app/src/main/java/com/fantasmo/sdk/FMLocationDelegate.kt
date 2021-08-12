package com.fantasmo.sdk

import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location

enum class FMResultConfidence{
    LOW,
    MEDIUM,
    HIGH
}

fun abbreviation(confidence: FMResultConfidence):String{
    return when(confidence){
        FMResultConfidence.LOW -> "L"
        FMResultConfidence.MEDIUM -> "M"
        FMResultConfidence.HIGH -> "H"
    }
}

class FMLocationResult(
    var location: Location,
    var confidence: FMResultConfidence,
    var zones: List<FMZone>
    )

enum class FMBehaviorRequest(val displayName: String) {
    TILTUP("Tilt your device up"),
    TILTDOWN("Tilt your device down"),
    PANAROUND("Pan around the scene"),
    PANSLOWLY("Pan more slowly"),
    ACCEPTED("Accepted");
}

/**
 * The methods that you use to receive events from an associated
 * location manager object.
 */
interface FMLocationListener {

    /**
     * Tells the listener that new location data is available.
     * @param result: Location of the device (or anchor if set)
     */
    fun locationManager(result: FMLocationResult)

    /**
     * Tells the listener that an error has occurred.
     * @param error: The error reported.
     * @param metadata: Metadata related to the error.
     */
    fun locationManager(error: ErrorResponse, metadata: Any?)

    /**
     * Tells the listener that a request behavior has occurred.
     * @param didRequestBehavior: The behavior reported.
     */
    fun locationManager(didRequestBehavior: FMBehaviorRequest){}
}