package com.fantasmo.sdk.requests

import android.content.Context
import android.media.Image
import android.view.Display
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.ResponseDelivery
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationListener
import com.fantasmo.sdk.FMLocationManager
import com.fantasmo.sdk.models.ErrorResponse
import com.fantasmo.sdk.models.FMZone
import com.fantasmo.sdk.models.Location
import com.fantasmo.sdk.utils.ImmediateResponseDelivery
import com.google.ar.core.*
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class LocalizeTest {

    private lateinit var fmLocationManager: FMLocationManager
    private lateinit var context: Context
    private val token = "API_KEY"

    private val testScope = TestCoroutineScope()

    private var mDelivery: ResponseDelivery? = null

    private lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        context = Mockito.mock(Context::class.java)
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        mDelivery = ImmediateResponseDelivery()
        fmLocationManager = FMLocationManager(instrumentationContext)
        fmLocationManager.connect(token,fmLocationListener)
        fmLocationManager.startUpdatingLocation()
        fmLocationManager.coroutineScope = testScope
    }

    @After
    fun cleanUp() {
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun testLocalizeZeroCoords(){
        fmLocationManager.isSimulation = false
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        testScope.runBlockingTest {
            fmLocationManager.localize(frame)
            assertEquals(false, fmLocationManager.testRequest)
        }
    }

    @Test
    fun testLocalizeFrameAccepted(){
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude,longitude)

        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
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
        val pose2 = Mockito.mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        testScope.runBlockingTest {
            fmLocationManager.localize(frame)
            assertEquals(true, fmLocationManager.testRequest)
        }
    }

    @Test
    fun testLocalizeFrameRejected(){
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude,longitude)

        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
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
        val pose2 = Mockito.mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        testScope.runBlockingTest {
            fmLocationManager.localize(frame)
            assertEquals(false, fmLocationManager.testRequest)
        }
    }

    @Test
    fun testLocalizeSimulationFMApi(){
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = true
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude,longitude)

        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
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
        val pose2 = Mockito.mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        val image = Mockito.mock(Image::class.java)
        `when`(frame.acquireCameraImage()).thenReturn(image)

        val imagePlanes = Mockito.mock(Image.Plane::class.java)
        `when`(image.planes).thenReturn(arrayOf(imagePlanes,imagePlanes,imagePlanes))

        val buffer = Mockito.mock(ByteBuffer::class.java)
        `when`(image.planes[0].buffer).thenReturn(buffer)
        `when`(image.planes[1].buffer).thenReturn(buffer)
        `when`(image.planes[2].buffer).thenReturn(buffer)

        val height = 480
        val width = 640
        `when`(image.height).thenReturn(height)
        `when`(image.width).thenReturn(width)

        val display = Mockito.mock(Display::class.java)
        `when`(context.display).thenReturn(display)

        `when`(display.rotation).thenReturn(Surface.ROTATION_0)

        `when`(frame.camera.displayOrientedPose).thenReturn(cameraPose)

        val imageIntrinsics = Mockito.mock(CameraIntrinsics::class.java)
        `when`(frame.camera.imageIntrinsics).thenReturn(imageIntrinsics)
        `when`(frame.camera.imageIntrinsics.focalLength).thenReturn(floatArrayOf(1083.401611328125f, 1083.401611328125f))
        `when`(frame.camera.imageIntrinsics.principalPoint).thenReturn(floatArrayOf(481.0465087890625f, 629.142822265625f))

        testScope.runBlockingTest {
            fmLocationManager.localize(frame)
            assertEquals(true, fmLocationManager.testRequest)
        }
    }

    @Test
    fun testLocalizeNoSimulationFMApi(){
        fmLocationManager.isConnected = true
        fmLocationManager.isSimulation = false
        val latitude = 48.12863302178715
        val longitude = 11.572371166069702
        fmLocationManager.setLocation(latitude,longitude)

        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
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
        val pose2 = Mockito.mock(Pose::class.java)
        `when`(frame.androidSensorPose).thenReturn(pose2)
        `when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(cameraPose.rotationQuaternion)

        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(cameraPose.translation)

        val image = Mockito.mock(Image::class.java)
        `when`(frame.acquireCameraImage()).thenReturn(image)

        val imagePlanes = Mockito.mock(Image.Plane::class.java)
        `when`(image.planes).thenReturn(arrayOf(imagePlanes,imagePlanes,imagePlanes))

        val buffer = Mockito.mock(ByteBuffer::class.java)
        `when`(image.planes[0].buffer).thenReturn(buffer)
        `when`(image.planes[1].buffer).thenReturn(buffer)
        `when`(image.planes[2].buffer).thenReturn(buffer)

        val height = 480
        val width = 640
        `when`(image.height).thenReturn(height)
        `when`(image.width).thenReturn(width)

        val display = Mockito.mock(Display::class.java)
        `when`(context.display).thenReturn(display)

        `when`(display.rotation).thenReturn(Surface.ROTATION_0)

        `when`(frame.camera.displayOrientedPose).thenReturn(cameraPose)

        val imageIntrinsics = Mockito.mock(CameraIntrinsics::class.java)
        `when`(frame.camera.imageIntrinsics).thenReturn(imageIntrinsics)
        `when`(frame.camera.imageIntrinsics.focalLength).thenReturn(floatArrayOf(1083.401611328125f, 1083.401611328125f))
        `when`(frame.camera.imageIntrinsics.principalPoint).thenReturn(floatArrayOf(481.0465087890625f, 629.142822265625f))

        testScope.runBlockingTest {
            fmLocationManager.localize(frame)
            assertEquals(true, fmLocationManager.testRequest)
        }
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