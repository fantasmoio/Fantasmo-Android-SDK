package com.fantasmo.sdk.utilities


import android.util.Log
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.FMResultConfidence
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import kotlin.math.*

class LocationFuser {
    private var TAG = "LocationFuser"

    private var locations = mutableListOf<Location>()

    fun reset() {
        locations = mutableListOf()
    }

    private fun geometricMean(locations: List<Location>): Location {
        var x = 0.0
        var y = 0.0

        for(location in locations){
            x += location.coordinate.latitude
            y += location.coordinate.longitude
        }
        x /= locations.size
        y /= locations.size

        val coordinate = Coordinate(x,y)
        return Location(0, coordinate, 0, 0, 0,0)

    }

    // Geometric Median Algorithm using Weiszfeld method
    // https://stackoverflow.com/questions/30299267/geometric-median-of-multidimensional-points/30299705#30299705
    private fun geometricMedian(locations: List<Location>): Location {
        val maxIterations = 200
        // prevent from throwing convergence error when there's no location
        if(locations.isEmpty()){
            Log.e(TAG,"Empty List")
            val coordinate = Coordinate(Double.NaN,Double.NaN)
            return Location(0, coordinate, 0, 0, 0,0)
        }

        // prevent from throwing convergence error when there's just one location
        if(locations.size == 1){
            val result = locations[0]
            Log.d(TAG,"ResultFromGeometricMedian: $result")
            return result
        }

        //Initialising 'median' to the centroid
        val centroid = geometricMean(locations)

        //If the init point is in the set of points, shift it
        for(location in locations){
            if(location.coordinate.latitude == centroid.coordinate.latitude &&
                location.coordinate.longitude == centroid.coordinate.longitude){
                centroid.coordinate.latitude += 0.1
                centroid.coordinate.longitude += 0.1
            }
        }

        // Boolean testing the convergence toward a global optimum
        var convergence = false
        // List recording the distance evolution
        val distances = mutableListOf<Double>()
        // Number of iterations
        var iteration = 0
        while(!convergence && iteration < maxIterations){
            var x = 0.0
            var y = 0.0
            var denominator = 0.0
            var d = 0.0
            for(location in locations){
                val distance = degreeDistance(location, centroid)
                x += location.coordinate.latitude / distance
                y += location.coordinate.longitude / distance
                denominator += 1.0/distance
                d += distance * distance
            }
            distances.add(d)

            if(denominator == 0.0){
                Log.d(TAG,"Couldn't compute a geometric median")
                val coordinate = Coordinate(0.0,0.0)
                return Location(0, coordinate, 0, 0, 0,0)
            }
            // Update to the new value of the median
            centroid.coordinate.latitude = x / denominator
            centroid.coordinate.longitude = y / denominator

            // Test the convergence over three steps for stability
            if(iteration>3){
                convergence = abs(distances[iteration] - distances[iteration-2]) < 0.1
            }
            iteration++
        }

        // When convergence or iterations limit is reached we assume that we found the median.
        if(iteration == maxIterations){
            Log.e(TAG,"Median did not converge after $maxIterations iterations!")
        }

        Log.d(TAG,"ResultFromGeometricMedian: $centroid")
        return centroid
    }

    private fun medianOfAbsoluteDistances(
        results: List<Location>,
        median: Location
    ): Double {
        val distances = mutableListOf<Double>()

        for (result in results) {
            val distance = abs(degreeDistance(result,median))
            distances.add(distance)
        }

        val distanceMedian = median(distances)
        return if (distanceMedian != 0.0) {
            distanceMedian
        } else {
            Double.NaN
        }
    }

    private fun classifyInliers(locations: List<Location>): List<Location> {
        if(locations.isEmpty()){
            Log.e(TAG,"Empty List. Could not classify inliers")
            return locations
        }

        if(locations.size == 1){
            return locations
        }

        val median = geometricMedian(locations)
        val mad = medianOfAbsoluteDistances(locations, median)

        val inliers = mutableListOf<Location>()
        for (result in locations) {
            val distance = abs(degreeDistance(result,median))
            if (0.6745 * distance / mad <= 3.5) {
                inliers.add(result)
            }
        }
        return inliers
    }

    private fun calculateConfidence(results: List<Location>): FMResultConfidence {
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
        locations.add(location)

        val inliers = classifyInliers(locations)
        val median = geometricMedian(inliers)
        val confidence = calculateConfidence(locations)

        return FMLocationResult(median, confidence, zones)
    }


    // Utility functions
    // Measure distance, treating lat and long as unitless Cartesian coordinates
    private fun degreeDistance(to: Location, from: Location): Double {
        val dLat = to.coordinate.latitude - from.coordinate.latitude
        val dLon = to.coordinate.longitude - from.coordinate.longitude
        return sqrt(dLat * dLat + dLon * dLon)
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