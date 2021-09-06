package com.fantasmo.sdk

import android.os.Build
import com.fantasmo.sdk.models.Coordinate
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.utilities.LocationFuser
import com.fantasmo.sdk.utilities.LocationFuserExtension.Companion.classifyInliers
import com.fantasmo.sdk.utilities.LocationFuserExtension.Companion.degreeDistance
import com.fantasmo.sdk.utilities.LocationFuserExtension.Companion.geometricMedian
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class LocationFuserTest {

    @Test
    fun testGeometricMedian() {
        val locations = mutableListOf<Location>()

        val coordinate = Coordinate(0.0, 0.0)
        val location = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location)
        var median: Location = geometricMedian(locations)

        var coordinateExpected = Coordinate(0.0, 0.0)
        var expected = Location(0, coordinateExpected, 0, 0, 0, 0)
        assertTrue(degreeDistance(median, expected) < 0.001)

        locations.clear()
        val coordinate1 = Coordinate(10.0, 0.0)
        val location1 = Location(0, coordinate1, 0, 0, 0, 0)
        locations.add(location1)
        val coordinate2 = Coordinate(-10.0, 0.0)
        val location2 = Location(0, coordinate2, 0, 0, 0, 0)
        locations.add(location2)

        median = geometricMedian(locations)
        assertTrue(degreeDistance(median, expected) < 0.001)

        val coordinate3 = Coordinate(0.0, 10.0)
        val location3 = Location(0, coordinate3, 0, 0, 0, 0)
        locations.add(location3)

        coordinateExpected = Coordinate(0.0, 5.77)
        expected = Location(0, coordinateExpected, 0, 0, 0, 0)

        median = geometricMedian(locations)
        assertTrue(degreeDistance(median, expected) < 0.01)

        locations.clear()
        val coordinate4 = Coordinate(10.0, 10.0)
        val location4 = Location(0, coordinate4, 0, 0, 0, 0)
        locations.add(location4)
        val coordinate5 = Coordinate(20.0, 10.0)
        val location5 = Location(0, coordinate5, 0, 0, 0, 0)
        locations.add(location5)
        val coordinate6 = Coordinate(10.0, 20.0)
        val location6 = Location(0, coordinate6, 0, 0, 0, 0)
        locations.add(location6)
        val coordinate7 = Coordinate(20.0, 20.0)
        val location7 = Location(0, coordinate7, 0, 0, 0, 0)
        locations.add(location7)

        coordinateExpected = Coordinate(15.0, 15.0)
        expected = Location(0, coordinateExpected, 0, 0, 0, 0)

        median = geometricMedian(locations)
        assertTrue(degreeDistance(median, expected) < 0.01)

        val coordinate8 = Coordinate(15.0, 15.0)
        val location8 = Location(0, coordinate8, 0, 0, 0, 0)
        locations.add(location8)
        median = geometricMedian(locations)
        assertTrue(degreeDistance(median, expected) < 0.01)
    }

    @Test
    fun testGeometricMedianColinear(){
        val locations = mutableListOf<Location>()

        val coordinate = Coordinate(0.0, 0.0)
        val location = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location)
        val coordinate1 = Coordinate(0.0, 10.0)
        val locations1 = Location(0, coordinate1, 0, 0, 0, 0)
        locations.add(locations1)

        var median: Location = geometricMedian(locations)

        var coordinateExpected = Coordinate(0.0, 5.0)
        var expected = Location(0, coordinateExpected, 0, 0, 0, 0)
        assertTrue(degreeDistance(median, expected) < 0.01)

        val coordinate2 = Coordinate(0.0, 20.0)
        val locations2 = Location(0, coordinate2, 0, 0, 0, 0)
        locations.add(locations2)
        median = geometricMedian(locations)
        coordinateExpected = Coordinate(0.0, 10.0)
        expected = Location(0, coordinateExpected, 0, 0, 0, 0)

        assertTrue(degreeDistance(median, expected) < 0.01)
    }

    @Test
    fun testLocationFusion(){
        val fuser = LocationFuser()
        var result: FMLocationResult

        var coordinate = Coordinate(0.0, 0.0)
        var location = Location(0, coordinate, 0, 0, 0, 0)
        result = fuser.fusedResult(location,listOf())

        var coordinateExpected = Coordinate(0.0, 0.0)
        var expected = Location(0, coordinateExpected, 0, 0, 0, 0)

        assertTrue(degreeDistance(result.location, expected) < 0.01)
        assertEquals(result.confidence, FMResultConfidence.LOW)

        coordinate = Coordinate(0.0, 10.0)
        location = Location(0, coordinate, 0, 0, 0, 0)
        result = fuser.fusedResult(location,listOf())

        coordinateExpected = Coordinate(0.0, 5.0)
        expected = Location(0, coordinateExpected, 0, 0, 0, 0)
        assertTrue(degreeDistance(result.location, expected) < 0.01)
        assertEquals(result.confidence, FMResultConfidence.LOW)

        coordinate = Coordinate(0.0, 20.0)
        location = Location(0, coordinate, 0, 0, 0, 0)
        result = fuser.fusedResult(location,listOf())

        coordinateExpected = Coordinate(0.0, 10.0)
        expected = Location(0, coordinateExpected, 0, 0, 0, 0)
        assertTrue(degreeDistance(result.location, expected) < 0.01)
        assertEquals(result.confidence, FMResultConfidence.MEDIUM)
    }

    @Test
    fun testInliers(){
        val locations = mutableListOf<Location>()

        val coordinate = Coordinate(0.0, 0.0)
        val location = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location)
        val coordinate2 = Coordinate(0.0, 0.01)
        val location2 = Location(0, coordinate2, 0, 0, 0, 0)
        locations.add(location2)
        val coordinate3 = Coordinate(0.0, 0.02)
        val location3 = Location(0, coordinate3, 0, 0, 0, 0)
        locations.add(location3)
        var inliers: List<Location> = classifyInliers(locations)
        assertEquals(inliers.size,3)

        val coordinate4 = Coordinate(1.0, 0.0)
        val location4 = Location(0, coordinate4, 0, 0, 0, 0)
        locations.add(location4)

        inliers = classifyInliers(locations)
        assertEquals(inliers.size,3)

        val location5 = Location(0, coordinate4, 0, 0, 0, 0)
        locations.add(location5)
        inliers = classifyInliers(locations)
        assertEquals(inliers.size,3)
    }

    @Test
    fun testLocationFusionOutliers(){
        val fuser = LocationFuser()
        var result: FMLocationResult

        val coordinate = Coordinate(0.0, 0.0)
        val location = Location(0, coordinate, 0, 0, 0, 0)
        result = fuser.fusedResult(location,listOf())

        val coordinate1 = Coordinate(0.0, 0.01)
        val location1 = Location(0, coordinate1, 0, 0, 0, 0)
        result = fuser.fusedResult(location1,listOf())

        val coordinate2 = Coordinate(0.0, 0.02)
        val location2 = Location(0, coordinate2, 0, 0, 0, 0)
        result = fuser.fusedResult(location2,listOf())

        val coordinate3 = Coordinate(1.0, 0.0)
        val location3 = Location(0, coordinate3, 0, 0, 0, 0)
        result = fuser.fusedResult(location3,listOf())

        val coordinateExpected = Coordinate(0.0, 0.01)
        val expected = Location(0, coordinateExpected, 0, 0, 0, 0)

        assertTrue(degreeDistance(result.location, expected) < 0.01)
        assertEquals(result.confidence, FMResultConfidence.MEDIUM)

        val coordinate4 = Coordinate(1.0, 0.0)
        val location4 = Location(0, coordinate4, 0, 0, 0, 0)
        result = fuser.fusedResult(location4,listOf())

        assertTrue(degreeDistance(result.location, expected) < 0.01)
        assertEquals(result.confidence, FMResultConfidence.HIGH)
    }

    @Test
    fun testLocationFusionRealData(){
        val fuser = LocationFuser()
        var result: FMLocationResult

        val coordinate = Coordinate(48.826571, 2.327442)
        val location = Location(0, coordinate, 0, 0, 0, 0)
        result = fuser.fusedResult(location,listOf())

        val coordinate1 = Coordinate(48.826571, 2.327438)
        val location1 = Location(0, coordinate1, 0, 0, 0, 0)
        result = fuser.fusedResult(location1,listOf())

        val coordinate2 = Coordinate(48.826578, 2.327439)
        val location2 = Location(0, coordinate2, 0, 0, 0, 0)
        result = fuser.fusedResult(location2,listOf())

        val coordinate3 = Coordinate(48.826589,  2.327399)
        val location3 = Location(0, coordinate3, 0, 0, 0, 0)
        result = fuser.fusedResult(location3,listOf())

        val coordinate4 = Coordinate(48.826602, 2.327396)
        val location4 = Location(0, coordinate4, 0, 0, 0, 0)
        result = fuser.fusedResult(location4,listOf())

        assertEquals(result.confidence, FMResultConfidence.HIGH)

        val coordinate5 = Coordinate(48.826588, 2.327391)
        val location5 = Location(0, coordinate5, 0, 0, 0, 0)
        result = fuser.fusedResult(location5,listOf())

        val coordinate6 = Coordinate(48.826576, 2.327437)
        val location6 = Location(0, coordinate6, 0, 0, 0, 0)
        result = fuser.fusedResult(location6,listOf())

        val coordinate7 = Coordinate(48.826580, 2.327411)
        val location7 = Location(0, coordinate7, 0, 0, 0, 0)
        result = fuser.fusedResult(location7,listOf())

        val coordinate8 = Coordinate(48.826581, 2.327449)
        val location8 = Location(0, coordinate8, 0, 0, 0, 0)
        result = fuser.fusedResult(location8,listOf())

        val coordinate9 = Coordinate(48.826575, 2.327381)
        val location9 = Location(0, coordinate9, 0, 0, 0, 0)
        result = fuser.fusedResult(location9,listOf())

        val coordinate10 = Coordinate(48.826578, 2.327449)
        val location10 = Location(0, coordinate10, 0, 0, 0, 0)
        result = fuser.fusedResult(location10,listOf())

        val coordinate11 = Coordinate(48.826599, 2.327395)
        val location11 = Location(0, coordinate11, 0, 0, 0, 0)
        result = fuser.fusedResult(location11,listOf())

        val coordinate12 = Coordinate(48.826598, 2.327391)
        val location12 = Location(0, coordinate12, 0, 0, 0, 0)
        result = fuser.fusedResult(location12,listOf())

        val coordinate13 = Coordinate(48.826579, 2.327437)
        val location13 = Location(0, coordinate13, 0, 0, 0, 0)
        result = fuser.fusedResult(location13,listOf())

        val coordinate14 = Coordinate(48.826578, 2.327443)
        val location14 = Location(0, coordinate14, 0, 0, 0, 0)
        result = fuser.fusedResult(location14,listOf())

        val coordinateExpected = Coordinate(48.82, 2.32)
        val expected = Location(0, coordinateExpected, 0, 0, 0, 0)

        assertTrue(degreeDistance(result.location, expected) < 0.01)
        assertEquals(result.confidence, FMResultConfidence.HIGH)
    }

    @Test
    fun testLocationFusionRealDataOutlier(){
        val fuser = LocationFuser()
        var result: FMLocationResult

        val coordinate = Coordinate(48.826571, 2.327442)
        val location = Location(0, coordinate, 0, 0, 0, 0)
        result = fuser.fusedResult(location,listOf())

        val coordinate1 = Coordinate(48.826571, 2.327438)
        val location1 = Location(0, coordinate1, 0, 0, 0, 0)
        result = fuser.fusedResult(location1,listOf())

        val coordinate2 = Coordinate(48.826578, 2.327439)
        val location2 = Location(0, coordinate2, 0, 0, 0, 0)
        result = fuser.fusedResult(location2,listOf())

        val coordinate3 = Coordinate(48.826589,  2.327399)
        val location3 = Location(0, coordinate3, 0, 0, 0, 0)
        result = fuser.fusedResult(location3,listOf())

        val coordinate4 = Coordinate(48.826602, 2.327396)
        val location4 = Location(0, coordinate4, 0, 0, 0, 0)
        result = fuser.fusedResult(location4,listOf())

        assertEquals(result.confidence, FMResultConfidence.HIGH)

        val coordinate5 = Coordinate(45.0, 2.327391)
        val location5 = Location(0, coordinate5, 0, 0, 0, 0)
        result = fuser.fusedResult(location5,listOf())

        val coordinate6 = Coordinate(45.0, 2.327437)
        val location6 = Location(0, coordinate6, 0, 0, 0, 0)
        result = fuser.fusedResult(location6,listOf())

        val coordinate7 = Coordinate(45.0, 2.327411)
        val location7 = Location(0, coordinate7, 0, 0, 0, 0)
        result = fuser.fusedResult(location7,listOf())

        val coordinate8 = Coordinate(48.826581, 2.327449)
        val location8 = Location(0, coordinate8, 0, 0, 0, 0)
        result = fuser.fusedResult(location8,listOf())

        val coordinate9 = Coordinate(48.826575, 2.327381)
        val location9 = Location(0, coordinate9, 0, 0, 0, 0)
        result = fuser.fusedResult(location9,listOf())

        val coordinate10 = Coordinate(48.826578, 2.327449)
        val location10 = Location(0, coordinate10, 0, 0, 0, 0)
        result = fuser.fusedResult(location10,listOf())

        val coordinate11 = Coordinate(48.826599, 2.327395)
        val location11 = Location(0, coordinate11, 0, 0, 0, 0)
        result = fuser.fusedResult(location11,listOf())

        val coordinate12 = Coordinate(48.826598, 2.327391)
        val location12 = Location(0, coordinate12, 0, 0, 0, 0)
        result = fuser.fusedResult(location12,listOf())

        val coordinate13 = Coordinate(48.826579, 2.327437)
        val location13 = Location(0, coordinate13, 0, 0, 0, 0)
        result = fuser.fusedResult(location13,listOf())

        val coordinate14 = Coordinate(48.826578, 2.327443)
        val location14 = Location(0, coordinate14, 0, 0, 0, 0)
        result = fuser.fusedResult(location14,listOf())

        val coordinateExpected = Coordinate(48.82, 2.32)
        val expected = Location(0, coordinateExpected, 0, 0, 0, 0)

        assertTrue(degreeDistance(result.location, expected) < 0.01)
        assertEquals(result.confidence, FMResultConfidence.HIGH)
    }

    @Test
    fun testConfidence(){
        val locations = mutableListOf<Location>()
        val method = LocationFuser().javaClass.getDeclaredMethod("standardDeviationConfidence",List::class.java)
        method.isAccessible = true

        val method2 = LocationFuser().javaClass.getDeclaredMethod("confidence",List::class.java)
        method2.isAccessible = true

        val coordinate = Coordinate(10.0, 10.0)
        val location = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location)
        assertEquals(method.invoke(LocationFuser(),locations), FMResultConfidence.LOW)
        assertEquals(method2.invoke(LocationFuser(),locations), FMResultConfidence.LOW)

        val coordinate1 = Coordinate(10.0, 10.0000001)
        val locations1 = Location(0, coordinate1, 0, 0, 0, 0)
        locations.add(locations1)
        assertEquals(method.invoke(LocationFuser(),locations), FMResultConfidence.HIGH)
        assertEquals(method2.invoke(LocationFuser(),locations), FMResultConfidence.HIGH)

        locations.clear()
        val coordinate3 = Coordinate(10.0, 10.0000002)
        val location2 = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location2)
        val locations3 = Location(0, coordinate3, 0, 0, 0, 0)
        locations.add(locations3)
        assertEquals(method.invoke(LocationFuser(),locations), FMResultConfidence.HIGH)
        assertEquals(method2.invoke(LocationFuser(),locations), FMResultConfidence.HIGH)

        locations.clear()
        val location4 = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location4)
        val coordinate5 = Coordinate(10.0, 10.000004)
        val locations5 = Location(0, coordinate5, 0, 0, 0, 0)
        locations.add(locations5)
        assertEquals(method.invoke(LocationFuser(),locations), FMResultConfidence.MEDIUM)
        assertEquals(method2.invoke(LocationFuser(),locations), FMResultConfidence.MEDIUM)

        locations.clear()
        val location6 = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location6)
        val coordinate7 = Coordinate(10.0, 10.000010)
        val locations7 = Location(0, coordinate7, 0, 0, 0, 0)
        locations.add(locations7)
        assertEquals(method.invoke(LocationFuser(),locations), FMResultConfidence.LOW)
        assertEquals(method2.invoke(LocationFuser(),locations), FMResultConfidence.LOW)

        val location8 = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location8)
        assertEquals(method2.invoke(LocationFuser(),locations), FMResultConfidence.MEDIUM)

        val location9 = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location9)
        assertEquals(method2.invoke(LocationFuser(),locations), FMResultConfidence.MEDIUM)

        val location10 = Location(0, coordinate, 0, 0, 0, 0)
        locations.add(location10)
        assertEquals(method2.invoke(LocationFuser(),locations), FMResultConfidence.HIGH)
    }

    @Test
    fun testAbbreviation(){
        var confidence = FMResultConfidence.LOW
        assertEquals("L",confidence.abbreviation())

        confidence = FMResultConfidence.MEDIUM
        assertEquals("M",confidence.abbreviation())

        confidence = FMResultConfidence.HIGH
        assertEquals("H",confidence.abbreviation())
    }
}