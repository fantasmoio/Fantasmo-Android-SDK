package com.fantasmo.sdk.utilities


import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.FMResultConfidence
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import kotlin.math.sqrt

class LocationFuser {

    private var locations = mutableListOf<Location>()

    fun reset() {
        locations = mutableListOf()
    }

    /**
     * Derives a confidence based on the standard deviation of locations
     * @param locations: list of Locations
     * @return FMResultConfidence
     */
    private fun standardDeviationConfidence(locations: List<Location>): FMResultConfidence{
        return if(locations.size>1){
            val variance = LocationFuserExtension.populationVariance(locations)

            when (sqrt(variance)){
                in 0.0..0.15->{
                    // within 15cm
                    FMResultConfidence.HIGH
                }
                in 0.15..0.5->{
                    // within 50cm
                    FMResultConfidence.MEDIUM
                }
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
    private fun confidence(locations: List<Location>): FMResultConfidence {
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
     * @param location: Location
     * @param zones: List of FMZones
     * @return FMLocationResult obtained from location fuse
     */
    fun fusedResult(location: Location, zones: List<FMZone>): FMLocationResult {
        locations.add(location)

        val inliers = LocationFuserExtension.classifyInliers(locations)
        val median = LocationFuserExtension.geometricMedian(inliers)
        val confidence = confidence(locations)

        return FMLocationResult(median, confidence, zones)
    }
}