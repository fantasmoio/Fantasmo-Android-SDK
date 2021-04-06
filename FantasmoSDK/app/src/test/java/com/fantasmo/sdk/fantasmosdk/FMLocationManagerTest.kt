package com.fantasmo.sdk.fantasmosdk

import android.content.Context
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.FMUtility
import com.fantasmo.sdk.models.*
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FMLocationManagerTest {

    private lateinit var fmLocationManager: FMLocationManager
    private lateinit var context: Context

    private val token = "8e785284ca284c01bd84116c0d18e8fd"

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        fmLocationManager = FMLocationManager(context)
    }

    @Test
    fun testLocalizeNotConnected() {
        fmLocationManager.isConnected = false

        val frame = mock(Frame::class.java)
        fmLocationManager.localize(frame)

        assertEquals(FMLocationManager.State.STOPPED, fmLocationManager.state)
    }

    @Test
    fun testIsZoneInRadius() {
        fmLocationManager.isConnected = false
        fmLocationManager.isSimulation = true

        var radius = 10
        fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
            assertEquals(true, it)
        }

        radius = 100
        fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
            assertEquals(false, it)
        }
    }

    @Test
    fun connectAndStart() {
        fmLocationManager.connect(token, fmLocationListener)

        fmLocationManager.startUpdatingLocation()
        assertEquals(true, fmLocationManager.isConnected)
        assertEquals(FMLocationManager.State.LOCALIZING, fmLocationManager.state)
    }

    @Test
    fun stopUpdatingLocation() {
        fmLocationManager.stopUpdatingLocation()
        assertEquals(false, fmLocationManager.isConnected)
        assertEquals(FMLocationManager.State.STOPPED, fmLocationManager.state)
    }

    @Test
    fun setAnchor() {
        val frame = mock(Frame::class.java)
        fmLocationManager.setAnchor(frame)

        val anchorFrame = fmLocationManager.javaClass.getDeclaredField("anchorFrame")
        anchorFrame.isAccessible = true

        assertNotNull(anchorFrame.get(fmLocationManager))
    }

    @Test
    fun unsetAnchor() {
        fmLocationManager.unsetAnchor()

        val anchorFrame = fmLocationManager.javaClass.getDeclaredField("anchorFrame")
        anchorFrame.isAccessible = true

        assertNull(anchorFrame.get(fmLocationManager))
    }

    @Test
    fun anchorDeltaPoseForNullFrameTest() {
        val frame = mock(Frame::class.java)
        val anchorFrame = mock(Frame::class.java)

        val deltaFMPose = FMUtility.anchorDeltaPoseForFrame(frame, anchorFrame)
        val position = FMPosition(0f, 0f, 0f)
        val orientation = FMOrientation(0f, 0f, 0f, 0f)

        assertEquals(position.x, deltaFMPose.position.x)
        assertEquals(position.y, deltaFMPose.position.y)
        assertEquals(position.z, deltaFMPose.position.z)

        assertEquals(orientation.x, deltaFMPose.orientation.x)
        assertEquals(orientation.y, deltaFMPose.orientation.y)
        assertEquals(orientation.z, deltaFMPose.orientation.z)
        assertEquals(orientation.w, deltaFMPose.orientation.w)
    }

    @Test
    fun anchorDeltaPoseForFrameTest() {
        // Test with random values to make sure calculation doesn't change
        val cameraPose =
            Pose(floatArrayOf(0.44f, 0.42f, 4.21f), floatArrayOf(-0.14f, -0.1434f, -0.03f, 0.956f))
        val anchorPose =
            Pose(floatArrayOf(4.02f, 1.42f, 0.21f), floatArrayOf(0.14f, 0.1434f, -0.03f, 0.456f))

        val anchorDeltaPose = FMPose.diffPose(anchorPose, cameraPose)
        val expectedFMPose = FMPose(
            FMPosition(-0.7664018f, -1.2556884f, 3.3475976f),
            FMOrientation(0.81345063f, 0.3877355f, -0.4324503f, 0.030760288f)
        )

        assertEquals(expectedFMPose.position.x, anchorDeltaPose.position.x)
        assertEquals(expectedFMPose.position.y, anchorDeltaPose.position.y)
        assertEquals(expectedFMPose.position.z, anchorDeltaPose.position.z)

        assertEquals(expectedFMPose.orientation.x, anchorDeltaPose.orientation.x)
        assertEquals(expectedFMPose.orientation.y, anchorDeltaPose.orientation.y)
        assertEquals(expectedFMPose.orientation.z, anchorDeltaPose.orientation.z)
        assertEquals(expectedFMPose.orientation.w, anchorDeltaPose.orientation.w)
    }

    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
            }

            override fun locationManager(location: Location, zones: List<FMZone>?) {
            }
        }
}