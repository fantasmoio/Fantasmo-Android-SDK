package com.fantasmo.sdk.utilities


import android.util.Log
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.FMResultConfidence
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import java.util.*
import kotlin.math.*

class LocationFuser {
    private var TAG = "LocationFuser"

    class InterimResult(
        var location: Location,
        var zones: List<FMZone>
    )

    private var results = mutableListOf<InterimResult>()

    fun reset() {
        results = mutableListOf()
    }

    private fun geometricMedian(results: List<InterimResult>): android.location.Location {
        return android.location.Location("")
    }

    private fun medianOfAbsoluteDistances(
        results: List<InterimResult>,
        median: android.location.Location
    ): Double {
        val distances = mutableListOf<Double>()

        for (result in results) {
            val androidLocation = convertToAndroidLocation(result.location)
            val distance = abs(androidLocation.distanceTo(median)).toDouble()
            distances.add(distance)
        }

        val distanceMedian = median(distances)
        return if (distanceMedian != 0.0) {
            distanceMedian
        } else {
            Double.NaN
        }
    }

    private fun classifyInliers(results: List<InterimResult>): MutableList<InterimResult> {
        val median = geometricMedian(results)
        val mad = medianOfAbsoluteDistances(results, median)

        val inliers = mutableListOf<InterimResult>()
        for (result in results) {
            val androidLocation = convertToAndroidLocation(result.location)
            val distance = abs(androidLocation.distanceTo(median))
            if (0.6745 * distance / mad <= 3.5) {
                inliers.add(result)
            }
        }
        return inliers
    }

    private fun calculateConfidence(results: List<InterimResult>): FMResultConfidence {
        return when (results.size) {
            1, 2 -> {
                FMResultConfidence.LOW
            }
            3, 4 -> {
                FMResultConfidence.MEDIUM
            }
            else -> {
                FMResultConfidence.HIGH
            }
        }
    }

    fun fusedResult(location: Location, zones: List<FMZone>): FMLocationResult{
        results.add(InterimResult(location,zones))

        val inliers = classifyInliers(results)
        val median = convertToLocation(geometricMedian(inliers))
        val confidence = calculateConfidence(results)

        //return FMLocationResult(median, confidence, zones)
        return FMLocationResult(location, FMResultConfidence.HIGH, zones)
    }


    //Utility functions
    private fun convertToLocation(location: android.location.Location): Location {
        val coordinate = Coordinate(location.latitude,location.longitude)
        return Location(location.altitude, coordinate, 0, 0, 0,0)
    }

    private fun convertToAndroidLocation(location: Location): android.location.Location {
        val androidLocation = android.location.Location("")
        androidLocation.latitude = location.coordinate.latitude
        androidLocation.longitude = location.coordinate.longitude
        return androidLocation
    }

    private fun median(distances: MutableList<Double>): Double {
        val size = distances.size
        if (size == 0) {
            return 0.0
        }

        distances.sort()

        return if (size % 2 != 0) {
            distances[size / 2]
        } else {
            (distances[size / 2] + distances[size / 2 - 1]) / 2.0
        }
    }
}