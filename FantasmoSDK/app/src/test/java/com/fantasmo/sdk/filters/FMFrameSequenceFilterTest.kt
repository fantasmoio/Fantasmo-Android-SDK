package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.fantasmo.sdk.frameSequenceFilter.*
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
class FMFrameSequenceFilterTest {

    @Test
    fun testShouldForceAcceptTrue() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMFrameSequenceFilter(context)
        val frame = Mockito.mock(Frame::class.java)

        filter.timestampOfPreviousApprovedFrame = 1L
        val timestamp = 80000000000
        Mockito.`when`(frame.timestamp).thenReturn(timestamp)

        assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            filter.check(frame)
        )
    }

    @Test
    fun testShouldForceAcceptFalse() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMFrameSequenceFilter(context)
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

        assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE),
            filter.check(frame)
        )
    }

    @Test
    fun testFrameCheck() {
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val filter = FMFrameSequenceFilter(instrumentationContext)

        val fmBlurFilterRule = FMBlurFilterRule(instrumentationContext)
        val spyFMBlurFilterRule = Mockito.spy(fmBlurFilterRule)

        filter.rules = listOf(FMMovementFilterRule(),FMCameraPitchFilterRule(),spyFMBlurFilterRule)

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

        Mockito.doReturn(300.0).`when`(spyFMBlurFilterRule).calculateVariance(frame)

        assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            filter.check(frame)
        )
    }
}