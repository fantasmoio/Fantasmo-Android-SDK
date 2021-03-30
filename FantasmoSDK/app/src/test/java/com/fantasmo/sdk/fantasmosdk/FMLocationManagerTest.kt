package com.fantasmo.sdk.fantasmosdk

import android.content.Context
import com.android.volley.Request
import com.android.volley.ResponseDelivery
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.fantasmosdk.network.MockMultiPartRequest
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.volley.utils.ImmediateResponseDelivery
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito

class FMLocationManagerTest {

    private lateinit var fmLocationManager: FMLocationManager

    private var mDelivery: ResponseDelivery? = null
    private var mRequest: MockMultiPartRequest? = null
    private val url = "https://api.fantasmo.io/v1/parking.in.radius"
    private val radius = 10
    private val token = "8e785284ca284c01bd84116c0d18e8fd"

    @Before
    fun setUp() {
        val context = Mockito.mock(Context::class.java)
        fmLocationManager = FMLocationManager(context)

        mDelivery = ImmediateResponseDelivery()
        mRequest = MockMultiPartRequest(
            Request.Method.POST, url,
            {

            },
            {

            }
        )
    }

    @Test
    @Ignore
    fun testLocalize() {
        TODO("Not yet implemented")
    }

    @Test
    fun testIsZoneInRadius() {
        fmLocationManager.isConnected = false
        fmLocationManager.isSimulation = true
        fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
            Assert.assertEquals(it, true)
        }

        val radius2 = 100
        fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius2) {
            Assert.assertEquals(it, false)
        }
    }

    @Test
    fun connectAndStart() {
        fmLocationManager.connect("testToken", fmLocationListener)

        fmLocationManager.startUpdatingLocation()
        Assert.assertEquals(true, fmLocationManager.isConnected)
        Assert.assertEquals(FMLocationManager.State.LOCALIZING, fmLocationManager.state)
    }

    @Test
    fun stopUpdatingLocation() {
        fmLocationManager.stopUpdatingLocation()
        Assert.assertEquals(false, fmLocationManager.isConnected)
        Assert.assertEquals(FMLocationManager.State.STOPPED, fmLocationManager.state)
    }

    @Test
    fun setAnchor() {

    }

    @Test
    fun unsetAnchor() {

    }

    @Test
    fun anchorDeltaPoseForNullFrameTest() {
        val frame = Mockito.mock(Frame::class.java)

        val deltaFMPose = fmLocationManager.anchorDeltaPoseForFrame(frame)
        val position = FMPosition(0f, 0f, 0f)
        val orientation = FMOrientation(0f, 0f, 0f, 0f)

        Assert.assertEquals(position.x, deltaFMPose.position.x)
        Assert.assertEquals(position.y, deltaFMPose.position.y)
        Assert.assertEquals(position.z, deltaFMPose.position.z)

        Assert.assertEquals(orientation.x, deltaFMPose.orientation.x)
        Assert.assertEquals(orientation.y, deltaFMPose.orientation.y)
        Assert.assertEquals(orientation.z, deltaFMPose.orientation.z)
        Assert.assertEquals(orientation.w, deltaFMPose.orientation.w)
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

        Assert.assertEquals(expectedFMPose.position.x, anchorDeltaPose.position.x)
        Assert.assertEquals(expectedFMPose.position.y, anchorDeltaPose.position.y)
        Assert.assertEquals(expectedFMPose.position.z, anchorDeltaPose.position.z)

        Assert.assertEquals(expectedFMPose.orientation.x, anchorDeltaPose.orientation.x)
        Assert.assertEquals(expectedFMPose.orientation.y, anchorDeltaPose.orientation.y)
        Assert.assertEquals(expectedFMPose.orientation.z, anchorDeltaPose.orientation.z)
        Assert.assertEquals(expectedFMPose.orientation.w, anchorDeltaPose.orientation.w)
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