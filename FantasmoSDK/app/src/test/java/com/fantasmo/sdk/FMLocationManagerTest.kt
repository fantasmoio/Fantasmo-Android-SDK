package com.fantasmo.sdk

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.os.Build
import android.view.Display
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.config.RemoteConfigTest
import com.fantasmo.sdk.filters.*
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMOrientation
import com.fantasmo.sdk.models.FMPose
import com.fantasmo.sdk.models.FMPosition
import com.fantasmo.sdk.models.analytics.AccumulatedARCoreInfo
import com.fantasmo.sdk.models.analytics.FrameFilterRejectionStatistics
import com.fantasmo.sdk.models.analytics.MotionManager
import com.fantasmo.sdk.network.FMApi
import com.fantasmo.sdk.network.FMNetworkManager
import com.google.ar.core.*
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
    private lateinit var spyMotionManager: MotionManager

    private val testScope = TestCoroutineScope()

    private lateinit var instrumentationContext: Context

    private val latitude = 48.12863302178715
    private val longitude = 11.572371166069702

    private val focalLength = floatArrayOf(
        1083.401611328125f,
        1083.401611328125f
    )

    private val principalPoint = floatArrayOf(
        481.0465087890625f,
        629.142822265625f
    )

    private val height = 480
    private val width = 640

    private val location = android.location.Location("")

    private val token = "API_KEY"

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context

        RemoteConfig.remoteConfig = RemoteConfigTest.remoteConfig

        location.latitude = latitude
        location.longitude = longitude
        fmLocationManager = FMLocationManager(instrumentationContext)

        fmLocationManager.connect(token, fmLocationListener)
        val fieldCoroutineScope = fmLocationManager.javaClass.getDeclaredField("coroutineScope")
        fieldCoroutineScope.isAccessible = true
        fieldCoroutineScope.set(fmLocationManager, testScope)

        spyMotionManager = MotionManager(instrumentationContext)
        val methodDisableSensor = spyMotionManager.javaClass.getDeclaredMethod("disableSensor")
        methodDisableSensor.isAccessible = true
        methodDisableSensor.invoke(spyMotionManager)

        val fieldMotionManager = fmLocationManager.javaClass.getDeclaredField("motionManager")
        fieldMotionManager.isAccessible = true
        fieldMotionManager.set(fmLocationManager, spyMotionManager)

        spyFMLocationManager = spy(fmLocationManager)

        spyFMApi = spy(spyFMLocationManager.fmApi)
        spyFMNetworkManager = spy(spyFMApi.fmNetworkManager)
    }

    @After
    fun cleanUp() {
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun testSetLocation() {
        fmLocationManager.setLocation(location)
        assertNotNull(fmLocationManager.currentLocation)
    }

    @Test
    fun connectAndStart() {
        fmLocationManager.connect(token, fmLocationListener)

        fmLocationManager.startUpdatingLocation("AppSessionIdExample")
        val fieldIsConnected = fmLocationManager.javaClass.getDeclaredField("isConnected")
        fieldIsConnected.isAccessible = true
        val result = fieldIsConnected.get(fmLocationManager)

        assertEquals(true, result)
        assertEquals(FMLocationManager.State.LOCALIZING, fmLocationManager.state)
    }

    @Test
    fun stopUpdatingLocation() {
        fmLocationManager.stopUpdatingLocation()

        val fieldIsConnected = fmLocationManager.javaClass.getDeclaredField("isConnected")
        fieldIsConnected.isAccessible = true
        val result = fieldIsConnected.get(fmLocationManager)

        assertEquals(false, result)
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

    /**
     * Asserts counters to check if they are not
     * being updated because filters are disabled
     */
    @Test
    fun testShouldLocalizeFiltersDisabled() {
        RemoteConfig.remoteConfig = RemoteConfigTest.remoteConfigDisabledFilters
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val fmLocationManager = FMLocationManager(instrumentationContext)
        fmLocationManager.connect(token, fmLocationListener)

        val fieldMotionManager = fmLocationManager.javaClass.getDeclaredField("motionManager")
        fieldMotionManager.isAccessible = true
        fieldMotionManager.set(fmLocationManager, spyMotionManager)

        fmLocationManager.startUpdatingLocation("AppSessionIdExample")

        val fieldCoroutineScope = fmLocationManager.javaClass.getDeclaredField("coroutineScope")
        fieldCoroutineScope.isAccessible = true
        fieldCoroutineScope.set(fmLocationManager, testScope)

        val spyFMLocationManager = spy(fmLocationManager)

        val spyFMApi = spy(spyFMLocationManager.fmApi)
        val spyFMNetworkManager = spy(spyFMApi.fmNetworkManager)

        fmLocationManager.isSimulation = false

        fmLocationManager.setLocation(location)

        val filter2 = FMFrameFilterChain(instrumentationContext)

        val testFilter = fmLocationManager.javaClass.getDeclaredField("frameFilterChain")
        testFilter.isAccessible = true
        testFilter.set(spyFMLocationManager, filter2)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)
        `when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)

        val cameraPose = getAcceptedPose()
        `when`(frame.camera.pose).thenReturn(cameraPose)
        `when`(frame.androidSensorPose).thenReturn(cameraPose)

        val accumulatedARCoreInfo = AccumulatedARCoreInfo()
        val fieldAccumulatedARCoreInfo = fmLocationManager.javaClass.getDeclaredField("accumulatedARCoreInfo")
        fieldAccumulatedARCoreInfo.isAccessible = true
        fieldAccumulatedARCoreInfo.set(fmLocationManager,accumulatedARCoreInfo)
        fmLocationManager.session(frame)
        assertEquals(1, accumulatedARCoreInfo.elapsedFrames)

        val frameFilterStats = FrameFilterRejectionStatistics()
        val fieldFrameStatistics = fmLocationManager.javaClass.getDeclaredField("frameEventAccumulator")
        fieldFrameStatistics.isAccessible = true
        fieldFrameStatistics.set(fmLocationManager,frameFilterStats)
        val field = frameFilterStats.javaClass.getDeclaredField("totalFrameCount")
        field.isAccessible = true
        val fieldValue = field.get(frameFilterStats)

        assertEquals(0, fieldValue)
    }

    /**
     * Asserts counters are being updated as a frame is accepted for localization
     */
    @Test
    fun testShouldLocalizeFrameAccepted() {
        fmLocationManager.startUpdatingLocation("AppSessionIdExample")
        fmLocationManager.isSimulation = false
        fmLocationManager.setLocation(location)

        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val fmBlurFilterRule = FMBlurFilter(
            RemoteConfigTest.remoteConfig.blurFilterVarianceThreshold,
            RemoteConfigTest.remoteConfig.blurFilterSuddenDropThreshold,
            RemoteConfigTest.remoteConfig.blurFilterAverageThroughputThreshold,
            instrumentationContext
        )
        val spyFMBlurFilterRule = spy(fmBlurFilterRule)

        val context = mock(Context::class.java)

        val frameFilter = FMFrameFilterChain(instrumentationContext)
        frameFilter.filters = listOf(
            FMMovementFilter(RemoteConfigTest.remoteConfig.movementFilterThreshold),
            FMCameraPitchFilter(
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
                context
            )
        ) as MutableList<FMFrameFilter>

        val fieldFrameFilter = fmLocationManager.javaClass.getDeclaredField("frameFilterChain")
        fieldFrameFilter.isAccessible = true
        fieldFrameFilter.set(fmLocationManager, frameFilter)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)
        `when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)

        val cameraPose = getAcceptedPose()
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        `when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        val display = mock(Display::class.java)
        `when`(context.display).thenReturn(display)
        `when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        val accumulatedARCoreInfo = AccumulatedARCoreInfo()
        val fieldAccumulatedARCoreInfo = fmLocationManager.javaClass.getDeclaredField("accumulatedARCoreInfo")
        fieldAccumulatedARCoreInfo.isAccessible = true
        fieldAccumulatedARCoreInfo.set(fmLocationManager,accumulatedARCoreInfo)
        fmLocationManager.session(frame)
        assertEquals(1, accumulatedARCoreInfo.elapsedFrames)

        val frameFilterStats = FrameFilterRejectionStatistics()
        val fieldFrameStatistics = fmLocationManager.javaClass.getDeclaredField("frameEventAccumulator")
        fieldFrameStatistics.isAccessible = true
        fieldFrameStatistics.set(fmLocationManager,frameFilterStats)
        val field = frameFilterStats.javaClass.getDeclaredField("totalFrameCount")
        field.isAccessible = true
        val fieldValue = field.get(frameFilterStats)

        assertEquals(0, fieldValue)
    }

    /**
     * Asserts counters are being updated as a frame is not accepted for localization
     */
    @Test
    fun testShouldLocalizeFrameRejected() {
        fmLocationManager.startUpdatingLocation("AppSessionIdExample")
        fmLocationManager.isSimulation = true
        fmLocationManager.setLocation(location)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)
        `when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)

        val cameraPose = getRejectedPose()
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        `when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        val context = mock(Context::class.java)
        val display = mock(Display::class.java)
        `when`(context.display).thenReturn(display)
        `when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        val frameFilter = FMFrameFilterChain(instrumentationContext)
        frameFilter.filters = listOf(
            FMMovementFilter(RemoteConfigTest.remoteConfig.movementFilterThreshold),
            FMCameraPitchFilter(
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
                context
            )
        ) as MutableList<FMFrameFilter>

        val fieldFrameFilter = fmLocationManager.javaClass.getDeclaredField("frameFilterChain")
        fieldFrameFilter.isAccessible = true
        fieldFrameFilter.set(fmLocationManager, frameFilter)

        val lastAcceptTime = System.nanoTime()
        val fieldLastAcceptTime = frameFilter.javaClass.getDeclaredField("lastAcceptTime")
        fieldLastAcceptTime.isAccessible = true
        fieldLastAcceptTime.set(frameFilter, lastAcceptTime)

        val accumulatedARCoreInfo = AccumulatedARCoreInfo()
        val fieldAccumulatedARCoreInfo = fmLocationManager.javaClass.getDeclaredField("accumulatedARCoreInfo")
        fieldAccumulatedARCoreInfo.isAccessible = true
        fieldAccumulatedARCoreInfo.set(fmLocationManager,accumulatedARCoreInfo)
        fmLocationManager.session(frame)
        assertEquals(1, accumulatedARCoreInfo.elapsedFrames)

        val frameFilterStats = FrameFilterRejectionStatistics()
        val fieldFrameStatistics = fmLocationManager.javaClass.getDeclaredField("frameEventAccumulator")
        fieldFrameStatistics.isAccessible = true
        fieldFrameStatistics.set(fmLocationManager,frameFilterStats)
        val field = frameFilterStats.javaClass.getDeclaredField("totalFrameCount")
        field.isAccessible = true
        val fieldValue = field.get(frameFilterStats)

        assertEquals(0, fieldValue)
    }

    // Localize Test Batch
    @Test
    fun testLocalizeNotConnected() {

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)
        val cameraPose = getAcceptedPose()
        `when`(frame.camera.pose).thenReturn(cameraPose)
        `when`(frame.androidSensorPose).thenReturn(cameraPose)

        fmLocationManager.session(frame)

        assertEquals(FMLocationManager.State.STOPPED, fmLocationManager.state)
    }

    // Localize Test with mocked Frame
    @Test
    fun testLocalizeFrameRejected() {
        fmLocationManager.startUpdatingLocation("AppSessionIdExample")
        fmLocationManager.isSimulation = false

        fmLocationManager.setLocation(location)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        val cameraPose = getRejectedPose()

        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        testScope.runBlockingTest {
            spyFMLocationManager.session(frame)
        }
        verify(spyFMLocationManager, times(1)).fmApi
    }

    @Test
    fun testLocalizeFrameAccepted() {
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val fmLocationManager = FMLocationManager(instrumentationContext)
        fmLocationManager.connect(token, fmLocationListener)

        val fieldMotionManager = fmLocationManager.javaClass.getDeclaredField("motionManager")
        fieldMotionManager.isAccessible = true
        fieldMotionManager.set(fmLocationManager, spyMotionManager)

        fmLocationManager.startUpdatingLocation("AppSessionIdExample")

        val fieldCoroutineScope = fmLocationManager.javaClass.getDeclaredField("coroutineScope")
        fieldCoroutineScope.isAccessible = true
        fieldCoroutineScope.set(fmLocationManager, testScope)

        val spyFMLocationManager = spy(fmLocationManager)

        val spyFMApi = spy(spyFMLocationManager.fmApi)
        val spyFMNetworkManager = spy(spyFMApi.fmNetworkManager)

        fmLocationManager.isSimulation = false

        fmLocationManager.setLocation(location)

        val fmBlurFilter = FMBlurFilter(
            RemoteConfigTest.remoteConfig.blurFilterVarianceThreshold,
            RemoteConfigTest.remoteConfig.blurFilterSuddenDropThreshold,
            RemoteConfigTest.remoteConfig.blurFilterAverageThroughputThreshold,
            instrumentationContext
        )

        val fieldRenderScriptContext = fmBlurFilter.javaClass.getDeclaredField("rs")
        fieldRenderScriptContext.isAccessible = true
        fieldRenderScriptContext.set(fmBlurFilter, null)

        val fieldColorIntrinsic = fmBlurFilter.javaClass.getDeclaredField("colorIntrinsic")
        fieldColorIntrinsic.isAccessible = true
        fieldColorIntrinsic.set(fmBlurFilter, null)

        val fieldConvolve = fmBlurFilter.javaClass.getDeclaredField("convolve")
        fieldConvolve.isAccessible = true
        fieldConvolve.set(fmBlurFilter, null)

        val spyFMBlurFilterRule = spy(fmBlurFilter)
        val context = mock(Context::class.java)

        val filter2 = FMFrameFilterChain(instrumentationContext)
        filter2.filters = listOf(
            FMMovementFilter(RemoteConfigTest.remoteConfig.movementFilterThreshold),
            FMCameraPitchFilter(
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
                context)
        ) as MutableList<FMFrameFilter>

        val testFilter = fmLocationManager.javaClass.getDeclaredField("frameFilterChain")
        testFilter.isAccessible = true
        testFilter.set(spyFMLocationManager, filter2)

        val frame = mock(Frame::class.java)

        val image = mock(Image::class.java)
        `when`(frame.acquireCameraImage()).thenReturn(image)

        val imagePlanes = mock(Image.Plane::class.java)
        `when`(image.planes).thenReturn(arrayOf(imagePlanes, imagePlanes, imagePlanes))

        val buffer = mock(ByteBuffer::class.java)
        `when`(image.planes[0].buffer).thenReturn(buffer)
        `when`(image.planes[1].buffer).thenReturn(buffer)
        `when`(image.planes[2].buffer).thenReturn(buffer)

        `when`(image.height).thenReturn(height)
        `when`(image.width).thenReturn(width)

        val bitmapImage = mock(Bitmap::class.java)
        `when`(bitmapImage.height).thenReturn(height)
        `when`(bitmapImage.width).thenReturn(width)

        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)
        `when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)

        val cameraPose = getAcceptedPose()
        val pose2 = mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        `when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        `when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        val display = mock(Display::class.java)
        `when`(context.display).thenReturn(display)
        `when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        val intrinsics = mock(CameraIntrinsics::class.java)
        `when`(frame.camera.imageIntrinsics).thenReturn(intrinsics)
        val imageDimensions = intArrayOf(height, width)
        `when`(frame.camera.imageIntrinsics.imageDimensions).thenReturn(imageDimensions)

        `when`(frame.camera.imageIntrinsics.focalLength).thenReturn(focalLength)
        `when`(frame.camera.imageIntrinsics.principalPoint).thenReturn(principalPoint)

        spyFMLocationManager.session(frame)

        verify(spyFMLocationManager, times(2)).fmApi
        verify(spyFMApi, times(1)).fmNetworkManager
    }

    /**
     * Localize with FMApi call
     */
    @Test
    fun testLocalizeSimulationFMApi() {
        val instrumentationContext2 = InstrumentationRegistry.getInstrumentation().context
        val fmLocationManager = FMLocationManager(instrumentationContext2)
        fmLocationManager.connect(token, fmLocationListener)

        val testMotionManager = fmLocationManager.javaClass.getDeclaredField("motionManager")
        testMotionManager.isAccessible = true
        testMotionManager.set(fmLocationManager, spyMotionManager)

        fmLocationManager.startUpdatingLocation("AppSessionIdExample")

        val testCoroutineScope = fmLocationManager.javaClass.getDeclaredField("coroutineScope")
        testCoroutineScope.isAccessible = true
        testCoroutineScope.set(fmLocationManager, testScope)

        val spyFMLocationManager = spy(fmLocationManager)

        val spyFMApi2 = spy(spyFMLocationManager.fmApi)
        val spyFMNetworkManager2 = spy(spyFMApi2.fmNetworkManager)

        fmLocationManager.isSimulation = false

        fmLocationManager.setLocation(location)

        val filter2 = FMFrameFilterChain(instrumentationContext)
        filter2.filters = listOf(
            FMMovementFilter(RemoteConfigTest.remoteConfig.movementFilterThreshold),
            FMCameraPitchFilter(
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
                context)
        ) as MutableList<FMFrameFilter>

        val testFilter = fmLocationManager.javaClass.getDeclaredField("frameFilterChain")
        testFilter.isAccessible = true
        testFilter.set(spyFMLocationManager, filter2)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)
        `when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)

        val cameraPose = getAcceptedPose()

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

        `when`(image.height).thenReturn(height)
        `when`(image.width).thenReturn(width)

        val display = mock(Display::class.java)
        `when`(context.display).thenReturn(display)

        `when`(display.rotation).thenReturn(Surface.ROTATION_0)

        `when`(frame.camera.displayOrientedPose).thenReturn(cameraPose)

        val imageIntrinsics = mock(CameraIntrinsics::class.java)
        `when`(frame.camera.imageIntrinsics).thenReturn(imageIntrinsics)
        `when`(frame.camera.imageIntrinsics.focalLength).thenReturn(focalLength)
        `when`(frame.camera.imageIntrinsics.principalPoint).thenReturn(principalPoint)

        val imageDimensions = intArrayOf(height, width)
        `when`(frame.camera.imageIntrinsics.imageDimensions).thenReturn(imageDimensions)

        spyFMLocationManager.session(frame)

        verify(spyFMLocationManager, times(1)).session(frame)
        verify(spyFMLocationManager, times(2)).fmApi
        verify(spyFMApi2, times(1)).fmNetworkManager
    }

    @Test
    fun testLocalizeNoSimulationFMApi() {
        val instrumentationContext3 = InstrumentationRegistry.getInstrumentation().context
        val fmLocationManager = FMLocationManager(instrumentationContext3)
        fmLocationManager.connect(token, fmLocationListener)

        val field = fmLocationManager.javaClass.getDeclaredField("motionManager")
        field.isAccessible = true
        field.set(fmLocationManager, spyMotionManager)

        fmLocationManager.startUpdatingLocation("AppSessionIdExample")

        val testCoroutineScope = fmLocationManager.javaClass.getDeclaredField("coroutineScope")
        testCoroutineScope.isAccessible = true
        testCoroutineScope.set(fmLocationManager, testScope)

        val spyFMLocationManager = spy(fmLocationManager)

        val spyFMApi3 = spy(spyFMLocationManager.fmApi)
        val spyFMNetworkManager3 = spy(spyFMApi3.fmNetworkManager)

        fmLocationManager.isSimulation = false
        fmLocationManager.setLocation(location)

        val filter2 = FMFrameFilterChain(instrumentationContext)
        filter2.filters = listOf(
            FMMovementFilter(RemoteConfigTest.remoteConfig.movementFilterThreshold),
            FMCameraPitchFilter(
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
                context
            )
        ) as MutableList<FMFrameFilter>

        val testFilter = fmLocationManager.javaClass.getDeclaredField("frameFilterChain")
        testFilter.isAccessible = true
        testFilter.set(spyFMLocationManager, filter2)

        val frame = mock(Frame::class.java)
        val camera = mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)
        `when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)

        val cameraPose = getAcceptedPose()

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

        `when`(image.height).thenReturn(height)
        `when`(image.width).thenReturn(width)

        val display = mock(Display::class.java)
        `when`(context.display).thenReturn(display)

        `when`(display.rotation).thenReturn(Surface.ROTATION_0)

        `when`(frame.camera.displayOrientedPose).thenReturn(cameraPose)

        val imageIntrinsics = mock(CameraIntrinsics::class.java)
        `when`(frame.camera.imageIntrinsics).thenReturn(imageIntrinsics)
        `when`(frame.camera.imageIntrinsics.focalLength).thenReturn(focalLength)
        `when`(frame.camera.imageIntrinsics.principalPoint).thenReturn(principalPoint)

        val imageDimensions = intArrayOf(height, width)
        `when`(frame.camera.imageIntrinsics.imageDimensions).thenReturn(imageDimensions)

        spyFMLocationManager.session(frame)

        verify(spyFMLocationManager, times(1)).session(frame)
        verify(spyFMLocationManager, times(2)).fmApi
        verify(spyFMApi3, times(1)).fmNetworkManager
    }

    private fun getAcceptedPose(): Pose {
        return Pose(
            floatArrayOf(
                -0.982F,
                -0.93F,
                0.6F
            ),
            floatArrayOf(
                -0.024842054F,
                0.0032415544F,
                0.004167135F,
                0.9996774F,
            )
        )
    }

    private fun getRejectedPose(): Pose {
        return Pose(
            floatArrayOf(
                -0.982F,
                -0.93F,
                0.6F
            ),
            floatArrayOf(
                0.25F,
                0.01F,
                0.01F,
                (-0.01).toFloat()
            )
        )
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

            override fun locationManager(result: FMLocationResult) {
            }
        }
}