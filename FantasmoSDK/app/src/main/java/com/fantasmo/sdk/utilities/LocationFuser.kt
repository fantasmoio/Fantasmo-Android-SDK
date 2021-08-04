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

    /**
     * Measures distance treating lat and long as unitless Cartesian coordinates
     * @param pointA: Starting Location point
     * @param pointB: Ending Location point
     * @return distance between the two points
     */
    private fun degreeDistance(pointA: Location, pointB: Location): Double {
        val dLat = pointA.coordinate.latitude - pointB.coordinate.latitude
        val dLon = pointA.coordinate.longitude - pointB.coordinate.longitude
        return sqrt(dLat * dLat + dLon * dLon)
    }

    /**
     * Measures the geometric mean from a group of Locations
     * @param locations: list of Locations to extract mean
     * @return mean Location
     * */
    private fun geometricMean(locations: List<Location>): Location {
        var x = 0.0
        var y = 0.0

        for (location in locations) {
            x += location.coordinate.latitude
            y += location.coordinate.longitude
        }
        x /= locations.size
        y /= locations.size

        val coordinate = Coordinate(x, y)
        return Location(0, coordinate, 0, 0, 0, 0)

    }

    /**
     * Finds the geometric median using Weiszfeld method
     * It follows this: https://stackoverflow.com/a/30299705
     * @param locations: list of Locations to extract the geometric median
     * @return median Location
     */
    private fun geometricMedian(locations: List<Location>): Location {
        val maxIterations = 200
        // prevent from throwing convergence error when there's no location
        if (locations.isEmpty()) {
            Log.e(TAG, "Empty List")
            val coordinate = Coordinate(Double.NaN, Double.NaN)
            return Location(0, coordinate, 0, 0, 0, 0)
        }

        // prevent from throwing convergence error when there's just one location
        if (locations.size == 1) {
            val result = locations[0]
            Log.d(TAG, "ResultFromGeometricMedian: $result")
            return result
        }

        //Initialising 'median' to the centroid
        val centroid = geometricMean(locations)

        //If the init point is in the set of points, shift it
        for (location in locations) {
            if (location.coordinate.latitude == centroid.coordinate.latitude &&
                location.coordinate.longitude == centroid.coordinate.longitude
            ) {
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
        while (!convergence && iteration < maxIterations) {
            var x = 0.0
            var y = 0.0
            var denominator = 0.0
            var sumSquareD = 0.0
            for (location in locations) {
                val distance = degreeDistance(location, centroid)
                x += location.coordinate.latitude / distance
                y += location.coordinate.longitude / distance
                denominator += 1.0 / distance
                sumSquareD += distance * distance
            }
            distances.add(sumSquareD)

            if (denominator == 0.0) {
                Log.d(TAG, "Couldn't compute a geometric median")
                val coordinate = Coordinate(0.0, 0.0)
                return Location(0, coordinate, 0, 0, 0, 0)
            }
            // Update to the new value of the median
            centroid.coordinate.latitude = x / denominator
            centroid.coordinate.longitude = y / denominator

            // Test the convergence over three steps for stability
            if (iteration > 3) {
                convergence = abs(distances[iteration] - distances[iteration - 2]) < 0.1
            }
            iteration++
        }

        // When convergence or iterations limit is reached we assume that we found the median.
        if (iteration == maxIterations) {
            Log.e(TAG, "Median did not converge after $maxIterations iterations!")
        }

        Log.d(TAG, "ResultFromGeometricMedian: $centroid")
        return centroid
    }

    /**
     * Measures the distance between the median Location and all the Location results
     * Then it returns the median distance from all of those distances
     * @param locations: list of Locations
     * @return median distance
     */
    private fun medianOfAbsoluteDistances(
        locations: List<Location>,
        median: Location
    ): Double {
        val distances = mutableListOf<Double>()

        for (location in locations) {
            val distance = abs(degreeDistance(location, median))
            distances.add(distance)
        }

        val distanceMedian = median(distances)
        return if (distanceMedian != 0.0) {
            distanceMedian
        } else {
            Double.NaN
        }
    }

    /**
     * Classifies all Locations as Inlier or Outlier
     * @param locations: list of locations
     * @return list of Locations classified as Inlier
     */
    private fun classifyInliers(locations: List<Location>): List<Location> {
        if (locations.isEmpty()) {
            Log.e(TAG, "Empty List. Could not classify inliers")
            return locations
        }

        if (locations.size == 1) {
            return locations
        }

        val median = geometricMedian(locations)
        val mad = medianOfAbsoluteDistances(locations, median)

        val inliers = mutableListOf<Location>()
        for (location in locations) {
            val distance = abs(degreeDistance(location, median))
            if (0.6745 * distance / mad <= 3.5) {
                inliers.add(location)
            }
        }
        return inliers
    }

    /**
     * Measures confidence level of the resulting Locations
     * @param locations: list of Locations
     * @return FMResultConfidence
     */
    private fun calculateConfidence(locations: List<Location>): FMResultConfidence {
        return when (locations.size) {
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

    fun fusedResult(location: Location, zones: List<FMZone>): FMLocationResult {
        locations.add(location)

        val inliers = classifyInliers(locations)
        val median = geometricMedian(inliers)
        val confidence = calculateConfidence(locations)

        return FMLocationResult(median, confidence, zones)
    }

    // Utility functions
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