package com.fantasmo.sdk

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.os.Build
import android.view.Display
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.frameSequenceFilter.FMBlurFilterRule
import com.fantasmo.sdk.frameSequenceFilter.FMCameraPitchFilterRule
import com.fantasmo.sdk.frameSequenceFilter.FMFrameSequenceFilter
import com.fantasmo.sdk.frameSequenceFilter.FMMovementFilterRule
import com.fantasmo.sdk.models.*
import com.fantasmo.sdk.network.FMApi
import com.fantasmo.sdk.network.FMNetworkManager
import com.google.ar.core.*
import com.google.ar.core.Pose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FMLocationManagerTest {

    private lateinit var fmLocationManager: FMLocationManager
    private lateinit var context: Context

    private lateinit var spyFMLocationManager: FMLocationManager
    private lateinit var spyFMNetworkManager: FMNetworkManager
    private lateinit var spyFMApi: FMApi

    private val testScope = TestCoroutineScope()

    private lateinit var instrumentationContext: Context

    private val token = "API_KEY"

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context

        fmLocationManager = FMLocationManager(instrumentationContext)

        fmLocationManager.connect(token,fmLocationListener)
        fmLocationManager.coroutineScope = testScope

        spyFMLocationManager = spy(fmLocationManager)

        spyFMNetworkManager = spy(spyFMLocationManager.fmNetworkManager)
        spyFMApi = spy(spyFMLocationManager.fmApi)
    }

    @After
    fun cleanUp() {
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun testSetLocation(){
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude,longitude)
        assertNotNull(fmLocationManager.currentLocation)
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

    // Anchor Test
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
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)

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
            FMPosition(2.1176877f, -2.2993965f, 4.1739006f),
            FMOrientation(0.014999978f, 0.21088079f, 0.189076f, -0.39667243f)
        )

        assertEquals(expectedFMPose.position.x, anchorDeltaPose.position.x)
        assertEquals(expectedFMPose.position.y, anchorDeltaPose.position.y)
        assertEquals(expectedFMPose.position.z, anchorDeltaPose.position.z)

        assertEquals(expectedFMPose.orientation.x, anchorDeltaPose.orientation.x)
        assertEquals(expectedFMPose.orientation.y, anchorDeltaPose.orientation.y)
        assertEquals(expectedFMPose.orientation.z, anchorDeltaPose.orientation.z)
        assertEquals(expectedFMPose.orientation.w, anchorDeltaPose.orientation.w)
    }

    //IsZoneInRadius Test Batch
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
    fun testIsZoneInRadiusNoSimulation(){
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude,longitude)

        val radius = 20
        var returnValue = false
        testScope.runBlockingTest {
            spyFMLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
                returnValue = it
            }
            assertEquals(false, returnValue)
        }
        verify(spyFMLocationManager, times(2)).fmApi
        verify(spyFMLocationManager, times(1)).fmNetworkManager
    }

    @Test
    fun testIsZoneInRadiusSimulation(){
        fmLocationManager.isSimulation = true

        val radius = 20
        var returnValue = false
        testScope.runBlockingTest {
            spyFMLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
                returnValue = it
            }
            assertEquals(false, returnValue)
        }
        verify(spyFMLocationManager, times(2)).fmApi
        verify(spyFMLocationManager, times(1)).fmNetworkManager
    }

    @Test
    fun testZeroCoordIsZoneInRadius(){
        fmLocationManager.isSimulation = false
        val latitude = 0.0
        val longitude = 0.0
        fmLocationManager.setLocation(latitude,longitude)

        val radius = 20
        var returnValue = false
        val start = System.currentTimeMillis()
        testScope.runBlockingTest {
            fmLocationManager.isZoneInRadius(FMZone.ZoneType.PARKING, radius) {
                returnValue = it
            }
        }
        val stop = System.currentTimeMillis()
        val delta = stop-start

        assertTrue(delta >= 10000)
        assertEquals(false, returnValue)
    }

    // Should Localize
    @Test
    fun testShouldLocalizeFrameAccepted() {
        fmLocationManager.startUpdatingLocation()
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude, longitude)

        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val filter = FMFrameSequenceFilter(instrumentationContext)
        val fmBlurFilterRule = FMBlurFilterRule(instrumentationContext)
        fmLocationManager.frameFilter = filter

        val spyFMBlurFilterRule = spy(fmBlurFilterRule)

        filter.rules = listOf(FMMovementFilterRule(), FMCameraPitchFilterRule(),spyFMBlurFilterRule)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        val cameraPose = Pose(
            floatArrayOf(
                (-0.982).toFloat(),
                (-0.93).toFloat(),
                0.6F
            ),
            floatArrayOf(
                0.15F, 0.03F, 0.5F,
                (-0.005).toFloat()
            )
        )
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        doReturn(300.0).`when`(spyFMBlurFilterRule).calculateVariance(frame)

        assertEquals(true, fmLocationManager.shouldLocalize(frame))
    }

    @Test
    fun testShouldLocalizeFrameRejected() {
        fmLocationManager.startUpdatingLocation()
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude, longitude)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        val cameraPose = Pose(
            floatArrayOf(
                (-0.982).toFloat(),
                (-0.93).toFloat(),
                0.6F
            ),
            floatArrayOf(
                0.3F, 0.03F, 0.5F,
                (-0.005).toFloat()
            )
        )
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        assertEquals(false, fmLocationManager.shouldLocalize(frame))
    }

    // Localize Test Batch
    @Test
    fun testLocalizeNotConnected() {
        fmLocationManager.isConnected = false

        val frame = mock(Frame::class.java)
        fmLocationManager.localize(frame)

        assertEquals(FMLocationManager.State.STOPPED, fmLocationManager.state)
    }

    @Test
    fun testLocalizeZeroCoords(){
        fmLocationManager.startUpdatingLocation()
        fmLocationManager.isSimulation = false
        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        testScope.runBlockingTest {
            fmLocationManager.localize(frame)
            assertEquals(FMLocationManager.State.LOCALIZING, fmLocationManager.state)
        }
    }

    // Localize Test with mocked Frame
    @Test
    fun testLocalizeFrameRejected() {
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude, longitude)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        val cameraPose = Pose(
            floatArrayOf(
                (-0.982).toFloat(),
                (-0.93).toFloat(),
                0.6F
            ),
            floatArrayOf(
                0.45F, 0.03F, 0.5F,
                (-0.005).toFloat()
            )
        )
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        testScope.runBlockingTest {
            spyFMLocationManager.localize(frame)
        }
        verify(spyFMLocationManager, times(1)).fmApi
    }

    @Test
    fun testLocalizeFrameAccepted() {
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val fmLocationManager = FMLocationManager(instrumentationContext)
        fmLocationManager.connect(token, fmLocationListener)
        fmLocationManager.startUpdatingLocation()
        val testScope = TestCoroutineScope()
        fmLocationManager.coroutineScope = testScope

        val spyFMLocationManager = spy(fmLocationManager)

        val spyFMNetworkManager = spy(spyFMLocationManager.fmNetworkManager)
        val spyFMApi = spy(spyFMLocationManager.fmApi)

        fmLocationManager.startUpdatingLocation()
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude, longitude)

        val fmBlurFilterRule = FMBlurFilterRule(instrumentationContext)
        val spyFMBlurFilterRule = spy(fmBlurFilterRule)
        fmLocationManager.frameFilter.rules = listOf(FMMovementFilterRule(), FMCameraPitchFilterRule(),spyFMBlurFilterRule)

        val frame = mock(Frame::class.java)

        val image = mock(Image::class.java)
        `when`(frame.acquireCameraImage()).thenReturn(image)

        val imagePlanes = mock(Image.Plane::class.java)
        `when`(image.planes).thenReturn(arrayOf(imagePlanes,imagePlanes,imagePlanes))

        val buffer = mock(ByteBuffer::class.java)
        `when`(image.planes[0].buffer).thenReturn(buffer)
        `when`(image.planes[1].buffer).thenReturn(buffer)
        `when`(image.planes[2].buffer).thenReturn(buffer)

        val height = 480
        val width = 640
        `when`(image.height).thenReturn(height)
        `when`(image.width).thenReturn(width)

        val bitmapImage = mock(Bitmap::class.java)
        `when`(bitmapImage.height).thenReturn(height)
        `when`(bitmapImage.width).thenReturn(width)

        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        val cameraPose = Pose(
            floatArrayOf(
                (-0.982).toFloat(),
                (-0.93).toFloat(),
                0.6F
            ),
            floatArrayOf(
                0.15F, 0.03F, 0.5F,
                (-0.005).toFloat()
            )
        )
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        doReturn(300.0).`when`(spyFMBlurFilterRule).calculateVariance(frame)

        testScope.runBlockingTest {
            spyFMLocationManager.localize(frame)
        }

        verify(spyFMLocationManager, times(2)).fmApi
    }

    //Localize with FMApi call
    @Test
    fun testLocalizeSimulationFMApi() {
        val instrumentationContext2 = InstrumentationRegistry.getInstrumentation().context
        val fmLocationManager = FMLocationManager(instrumentationContext2)
        fmLocationManager.connect(token, fmLocationListener)
        fmLocationManager.startUpdatingLocation()
        val testScope = TestCoroutineScope()
        fmLocationManager.coroutineScope = testScope

        val spyFMLocationManager = spy(fmLocationManager)

        val spyFMNetworkManager2 = spy(spyFMLocationManager.fmNetworkManager)
        val spyFMApi2 = spy(spyFMLocationManager.fmApi)

        fmLocationManager.startUpdatingLocation()
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude, longitude)

        val fmBlurFilterRule = FMBlurFilterRule(instrumentationContext2)
        val spyFMBlurFilterRule = spy(fmBlurFilterRule)
        fmLocationManager.frameFilter.rules = listOf(FMMovementFilterRule(), FMCameraPitchFilterRule(),spyFMBlurFilterRule)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        val cameraPose = Pose(
            floatArrayOf(
                (-0.982).toFloat(),
                (-0.93).toFloat(),
                0.6F
            ),
            floatArrayOf(
                0.15F, 0.03F, 0.5F,
                (-0.005).toFloat()
            )
        )
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        val image = mock(Image::class.java)
        `when`(frame.acquireCameraImage()).thenReturn(image)

        val imagePlanes = mock(Image.Plane::class.java)
        `when`(image.planes).thenReturn(arrayOf(imagePlanes, imagePlanes, imagePlanes))

        val buffer = mock(ByteBuffer::class.java)
        `when`(image.planes[0].buffer).thenReturn(buffer)
        `when`(image.planes[1].buffer).thenReturn(buffer)
        `when`(image.planes[2].buffer).thenReturn(buffer)

        val height = 480
        val width = 640
        `when`(image.height).thenReturn(height)
        `when`(image.width).thenReturn(width)

        val display = mock(Display::class.java)
        `when`(context.display).thenReturn(display)

        `when`(display.rotation).thenReturn(Surface.ROTATION_0)

        `when`(frame.camera.displayOrientedPose).thenReturn(cameraPose)

        val imageIntrinsics = mock(CameraIntrinsics::class.java)
        `when`(frame.camera.imageIntrinsics).thenReturn(imageIntrinsics)
        `when`(frame.camera.imageIntrinsics.focalLength).thenReturn(
            floatArrayOf(
                1083.401611328125f,
                1083.401611328125f
            )
        )
        `when`(frame.camera.imageIntrinsics.principalPoint).thenReturn(
            floatArrayOf(
                481.0465087890625f,
                629.142822265625f
            )
        )

        doReturn(300.0).`when`(spyFMBlurFilterRule).calculateVariance(frame)

        testScope.runBlockingTest {
            spyFMLocationManager.localize(frame)
        }

        verify(spyFMLocationManager, times(1)).localize(frame)
        verify(spyFMLocationManager, times(1)).shouldLocalize(frame)
        verify(spyFMLocationManager, times(2)).fmApi
        verify(spyFMLocationManager, times(1)).fmNetworkManager
    }

    @Test
    fun testLocalizeNoSimulationFMApi() {
        val instrumentationContext3 = InstrumentationRegistry.getInstrumentation().context
        val fmLocationManager = FMLocationManager(instrumentationContext3)
        fmLocationManager.connect(token, fmLocationListener)
        fmLocationManager.startUpdatingLocation()
        val testScope = TestCoroutineScope()
        fmLocationManager.coroutineScope = testScope

        val spyFMLocationManager = spy(fmLocationManager)

        val spyFMNetworkManager3 = spy(spyFMLocationManager.fmNetworkManager)
        val spyFMApi3 = spy(spyFMLocationManager.fmApi)

        fmLocationManager.startUpdatingLocation()
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude, longitude)

        val fmBlurFilterRule = FMBlurFilterRule(instrumentationContext3)
        val spyFMBlurFilterRule = spy(fmBlurFilterRule)
        fmLocationManager.frameFilter.rules = listOf(FMMovementFilterRule(), FMCameraPitchFilterRule(),spyFMBlurFilterRule)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        val cameraPose = Pose(
            floatArrayOf(
                (-0.982).toFloat(),
                (-0.93).toFloat(),
                0.6F
            ),
            floatArrayOf(
                0.15F, 0.03F, 0.5F,
                (-0.005).toFloat()
            )
        )
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        val image = mock(Image::class.java)
        `when`(frame.acquireCameraImage()).thenReturn(image)

        val imagePlanes = mock(Image.Plane::class.java)
        `when`(image.planes).thenReturn(arrayOf(imagePlanes, imagePlanes, imagePlanes))

        val buffer = mock(ByteBuffer::class.java)
        `when`(image.planes[0].buffer).thenReturn(buffer)
        `when`(image.planes[1].buffer).thenReturn(buffer)
        `when`(image.planes[2].buffer).thenReturn(buffer)

        val height = 480
        val width = 640
        `when`(image.height).thenReturn(height)
        `when`(image.width).thenReturn(width)

        val display = mock(Display::class.java)
        `when`(context.display).thenReturn(display)

        `when`(display.rotation).thenReturn(Surface.ROTATION_0)

        `when`(frame.camera.displayOrientedPose).thenReturn(cameraPose)

        val imageIntrinsics = mock(CameraIntrinsics::class.java)
        `when`(frame.camera.imageIntrinsics).thenReturn(imageIntrinsics)
        `when`(frame.camera.imageIntrinsics.focalLength).thenReturn(
            floatArrayOf(
                1083.401611328125f,
                1083.401611328125f
            )
        )
        `when`(frame.camera.imageIntrinsics.principalPoint).thenReturn(
            floatArrayOf(
                481.0465087890625f,
                629.142822265625f
            )
        )

        doReturn(300.0).`when`(spyFMBlurFilterRule).calculateVariance(frame)

        testScope.runBlockingTest {
            spyFMLocationManager.localize(frame)
        }

        verify(spyFMLocationManager, times(1)).localize(frame)
        verify(spyFMLocationManager, times(1)).shouldLocalize(frame)
        verify(spyFMLocationManager, times(2)).fmApi
        verify(spyFMLocationManager, times(1)).fmNetworkManager
    }


    /**
     * Listener for the Fantasmo SDK Location results.
     */
    private val fmLocationListener: FMLocationListener =
        object : FMLocationListener {
            override fun locationManager(error: ErrorResponse, metadata: Any?) {
            }

            override fun locationManager(didRequestBehavior: FMBehaviorRequest) {
            }

            override fun locationManager(location: Location, zones: List<FMZone>?) {
            }
        }
}