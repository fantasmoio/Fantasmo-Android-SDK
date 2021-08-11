package com.fantasmo.sdk.utilities

import android.util.Log
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.Location
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class LocationFuserExtension {

    companion object {

        private var TAG = "LocationFuserExtension"

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

        fun populationVariance(locations: List<Location>): Double {
            val size = locations.size
            if (size == 0) {
                return 0.0
            }
            val average = geometricMean(locations)
            val numerator = reduce(locations, average)

            return numerator / size
        }

        /**
         * Finds the geometric median using Weiszfeld method
         * It follows this: https://stackoverflow.com/a/30299705
         * @param locations: list of Locations to extract the geometric median
         * @return median Location
         */
        fun geometricMedian(locations: List<Location>): Location {
            val maxIterations = 200
            // Prevent from throwing convergence error when there's no location
            if (locations.isEmpty()) {
                Log.e(TAG, "Empty List")
                val coordinate = Coordinate(Double.NaN, Double.NaN)
                return Location(0, coordinate, 0, 0, 0, 0)
            }

            // Prevent from throwing convergence error when there's just one location
            if (locations.size == 1) {
                val result = locations[0]
                Log.d(TAG, "ResultFromGeometricMedian: $result")
                return result
            }

            // Initialising 'median' to the centroid
            val centroid = geometricMean(locations)

            val perturbation = 0.1
            // If the init point is in the set of points, shift it
            for (location in locations) {
                if (location.coordinate.latitude == centroid.coordinate.latitude &&
                    location.coordinate.longitude == centroid.coordinate.longitude
                ) {
                    centroid.coordinate.latitude += perturbation
                    centroid.coordinate.longitude += perturbation
                }
            }

            // Boolean testing the convergence toward a global optimum
            var convergence = false
            // List recording the distance evolution
            val distances = mutableListOf<Double>()
            // Number of iterations
            var iteration = 0
            val epsilon = 0.000001 // ≈ 0.11 m, used for convergence test
            while (!convergence && iteration < maxIterations) {
                var x = 0.0
                var y = 0.0
                var denominator = 0.0 // Weiszfeld denominator
                var sumSquareD = 0.0
                for (location in locations) {
                    val distance = degreeDistance(location, centroid)
                    x += location.coordinate.latitude / distance
                    y += location.coordinate.longitude / distance
                    denominator += 1.0 / distance
                    sumSquareD += distance * distance

                    if(distance.isNaN() || distance == 0.0 || denominator.isInfinite() || denominator.isNaN()){
                        Log.e(TAG,"Could not compute median due to numerical instability!")
                        return centroid
                    }
                }
                distances.add(sumSquareD)

                if (denominator == 0.0) {
                    Log.d(TAG, "Could not compute median due to denominator")
                    return centroid
                }

                // Update to the new value of the median
                centroid.coordinate.latitude = x / denominator
                centroid.coordinate.longitude = y / denominator

                // Test the convergence over three steps for stability
                if (iteration > 3) {
                    convergence = abs(distances[iteration] - distances[iteration - 2]) < epsilon
                }
                iteration++
            }

            // When convergence or iterations limit is reached we assume that we found the median.
            if (iteration == maxIterations) {
                Log.e(TAG, "Median did not converge after $maxIterations iterations!")
                val coordinate = Coordinate(Double.NaN, Double.NaN)
                return Location(0, coordinate, 0, 0, 0, 0)
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
        fun classifyInliers(locations: List<Location>): List<Location> {
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

        // Utility function, responsible for getting the median from a list of doubles
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

        /**
         * Reduce applies the provided operation to the collection elements sequentially
         * and return the accumulated result.The operation takes two arguments:
         * the previously accumulated value and the collection element
         */
        private fun reduce(locations: List<Location>, average: Location): Double {
            var total = 0.0
            var isFirst = true
            val averageLocation = convertToAndroidLocation(average)
            for (location in locations) {
                val locationA = convertToAndroidLocation(location)
                if (locations.indexOf(location) == 0 && isFirst) {
                    // The first index will be the value itself
                    total = locationA.distanceTo(averageLocation)
                        .toDouble()
                    isFirst = false
                } else {
                    // Else we accumulate from then
                    total += locationA.distanceTo(averageLocation)
                        .toDouble().pow(2.0)
                }
            }
            return total
        }

        private fun convertToLocation(location: android.location.Location): Location {
            val coordinate = Coordinate(location.latitude, location.longitude)
            return Location(location.altitude, coordinate, 0, 0, 0, 0)
        }

        private fun convertToAndroidLocation(location: Location): android.location.Location {
            val androidLocation = android.location.Location("")
            androidLocation.latitude = location.coordinate.latitude
            androidLocation.longitude = location.coordinate.longitude
            return androidLocation
        }
    }
}