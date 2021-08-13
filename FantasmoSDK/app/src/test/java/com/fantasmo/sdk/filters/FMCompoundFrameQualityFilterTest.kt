package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.filters.primeFilters.*
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FMCompoundFrameQualityFilterTest {

    @Test
    fun testShouldForceAcceptTrue() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCompoundFrameQualityFilter(context)
        val frame = Mockito.mock(Frame::class.java)

        filter.timestampOfPreviousApprovedFrame = 1L
        val timestamp = 80000000000
        Mockito.`when`(frame.timestamp).thenReturn(timestamp)

        assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            filter.accepts(frame)
        )
    }

    @Test
    fun testShouldForceAcceptFalse() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCompoundFrameQualityFilter(context)
        val frame = Mockito.mock(Frame::class.java)
        val pose = Pose(
            floatArrayOf(
                (-0.01).toFloat(),
                (-0.01).toFloat(),
                0.01F
            ),
            floatArrayOf(
                0.01F, 0.01F, 0.01F,
                (-0.01).toFloat()
            )
        )
        filter.timestampOfPreviousApprovedFrame = 0L
        val timestamp = 80000000000
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

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOFAST),
            filter.accepts(frame)
        )
    }

    @Test
    fun testFrameCheck() {
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val filter = FMCompoundFrameQualityFilter(instrumentationContext)

        val fmBlurFilterRule = FMBlurFilter(instrumentationContext)
        val spyFMBlurFilterRule = Mockito.spy(fmBlurFilterRule)

        filter.filters = listOf(
            FMMovementFilter(),
            FMCameraPitchFilter(instrumentationContext),spyFMBlurFilterRule)

        val frame = Mockito.mock(Frame::class.java)
        val pose = Pose(
            floatArrayOf(
                (-0.92).toFloat(),
                (-0.92).toFloat(),
                0.63F
            ),
            floatArrayOf(
                0.1F, 0.03F, 0.6F,
                (-0.005).toFloat()
            )
        )
        filter.timestampOfPreviousApprovedFrame = 1L
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

        Mockito.doReturn(300.0).`when`(spyFMBlurFilterRule).calculateVariance(frame)

        assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            filter.accepts(frame)
        )
    }
}