package com.fantasmo.sdk.utilities


import android.util.Log
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.FMResultConfidence
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import kotlin.math.sqrt

class LocationFuser {

    private val TAG = "LocationFuser"
    private var locations = mutableListOf<Location>()

    fun reset() {
        locations = mutableListOf()
    }

    /**
     * Derives a confidence based on the standard deviation of locations
     * @param locations: list of Locations
     * @return FMResultConfidence
     */
    fun standardDeviationConfidence(locations: List<Location>): FMResultConfidence{
        return if(locations.size>1){
            val variance = LocationFuserExtension.populationVariance(locations)

            when (sqrt(variance)){
                // within 15cm
                in 0.0..0.15->{
                    FMResultConfidence.HIGH
                }
                // within 50cm
                in 0.15..0.5->{
                    FMResultConfidence.MEDIUM
                }
                // more than 50 cm
                else -> FMResultConfidence.LOW
            }
        }else FMResultConfidence.LOW
    }

    /**
     * Measures confidence level based on a series of Location measurements
     * If the standard deviation of measurements is sufficiently low, confidence is high.
     * Otherwise, confidence increases with the number of samples.
     * @param locations: list of Locations
     * @return FMResultConfidence
     */
    fun confidence(locations: List<Location>): FMResultConfidence {
        val standardDeviationConfidence = standardDeviationConfidence(locations)

        return when (locations.size) {
            1, 2 -> {
                maxOf(standardDeviationConfidence,FMResultConfidence.LOW)
            }
            3, 4 -> {
                maxOf(standardDeviationConfidence,FMResultConfidence.MEDIUM)
            }
            else -> {
                FMResultConfidence.HIGH
            }
        }
    }

    /**
     * Method responsible to fuse locations in order to
     * improve accuracy during localization session
     * @param location: New location to be combined with previous observations
     * @param zones: List of FMZones at this Location
     * @return FMLocationResult obtained from location fuse
     */
    fun fusedResult(location: Location, zones: List<FMZone>): FMLocationResult {
        locations.add(location)

        val inliers = LocationFuserExtension.classifyInliers(locations)
        val median = LocationFuserExtension.geometricMedian(inliers)
        val confidence = confidence(locations)

        if(median.coordinate.latitude.isNaN() || median.coordinate.longitude.isNaN()){
            Log.e(TAG,"Image fusion error encountered!")
            return FMLocationResult(location, confidence, zones)
        }

        return FMLocationResult(median, confidence, zones)
    }
}