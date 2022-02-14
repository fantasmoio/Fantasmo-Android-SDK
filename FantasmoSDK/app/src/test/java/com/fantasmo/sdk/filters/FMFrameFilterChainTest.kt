package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.config.RemoteConfigTest
import com.fantasmo.sdk.models.FMFrame
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FMFrameFilterChainTest {

    private lateinit var filter : FMFrameFilterChain
    private lateinit var context : Context

    @Before
    fun setUp() {
        RemoteConfig.remoteConfig = RemoteConfigTest.remoteConfig
        context = Mockito.mock(Context::class.java)
        filter = FMFrameFilterChain(context)
    }

    @Test
    fun testShouldForceAcceptTrue() {
        val frame = Mockito.mock(FMFrame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)

        val lastAcceptTime = 1L
        val fieldLastAcceptTime = filter.javaClass.getDeclaredField("lastAcceptTime")
        fieldLastAcceptTime.isAccessible = true
        fieldLastAcceptTime.set(filter, lastAcceptTime)

        assertEquals(
            FMFrameFilterResult.Accepted,
            filter.accepts(frame)
        )
    }

    @Test
    fun testShouldForceAcceptFalse() {
        val frame = Mockito.mock(FMFrame::class.java)
        val pose = Pose(
            floatArrayOf(
                (-0.001).toFloat(),
                (-0.001).toFloat(),
                0.001F
            ),
            floatArrayOf(
                0.01F, 0.01F, 0.01F,
                (-0.01).toFloat()
            )
        )
        val lastAcceptTime = System.nanoTime()
        val fieldLastAcceptTime = filter.javaClass.getDeclaredField("lastAcceptTime")
        fieldLastAcceptTime.isAccessible = true
        fieldLastAcceptTime.set(filter, lastAcceptTime)

        val camera = Mockito.mock(Camera::class.java)
        val pose2 = Mockito.mock(Pose::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.pose).thenReturn(pose2)
        Mockito.`when`(frame.camera.pose.translation).thenReturn(pose.translation)
        Mockito.`when`(frame.androidSensorPose).thenReturn(pose2)
        Mockito.`when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        assertEquals(
            FMFilterRejectionReason.MOVINGTOOFAST,
            filter.accepts(frame).getRejectedReason()
        )
    }

       /**
     * Ignoring Test due to Renderscript failure
     * Cannot replicate context to create Renderscript environment
     * using Robolectric and Mockito testing libraries
     */
    @Ignore
    fun testFrameCheck() {
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val filter = FMFrameFilterChain(instrumentationContext)

        val fmBlurFilterRule = FMBlurFilter(
            RemoteConfigTest.remoteConfig.blurFilterVarianceThreshold,
            RemoteConfigTest.remoteConfig.blurFilterSuddenDropThreshold,
            RemoteConfigTest.remoteConfig.blurFilterAverageThroughputThreshold,
            instrumentationContext
        )
        val spyFMBlurFilterRule = Mockito.spy(fmBlurFilterRule)

        filter.filters = listOf(
            FMMovementFilter(RemoteConfigTest.remoteConfig.movementFilterThreshold),
            FMCameraPitchFilter(
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
                RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
                instrumentationContext
            )
        ) as MutableList<FMFrameFilter>

        val frame = Mockito.mock(FMFrame::class.java)
        val pose = getAcceptedPose()

        val lastAcceptTime = 1L
        val fieldLastAcceptTime = filter.javaClass.getDeclaredField("lastAcceptTime")
        fieldLastAcceptTime.isAccessible = true
        fieldLastAcceptTime.set(filter, lastAcceptTime)

        val timestamp = 6000000000
        Mockito.`when`(frame.timestamp).thenReturn(timestamp)
        val camera = Mockito.mock(Camera::class.java)
        val pose2 = Mockito.mock(Pose::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.pose).thenReturn(pose2)
        Mockito.`when`(frame.camera.pose.translation).thenReturn(pose.translation)
        Mockito.`when`(frame.androidSensorPose).thenReturn(pose2)
        Mockito.`when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val context = Mockito.mock(Context::class.java)
        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        assertEquals(
            FMFrameFilterResult.Accepted,
            filter.accepts(frame)
        )
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
}