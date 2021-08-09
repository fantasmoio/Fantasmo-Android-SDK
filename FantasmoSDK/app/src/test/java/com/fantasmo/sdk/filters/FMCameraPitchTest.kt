package com.fantasmo.sdk.filters

import android.content.Context
import android.view.Display
import android.view.Surface
import com.fantasmo.sdk.filters.primeFilters.FMCameraPitchFilter
import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterFailure
import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterResult
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class FMCameraPitchTest {

    @Test
    fun testPitchFilterAccepts() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(context)
        val frame = Mockito.mock(Frame::class.java)
        val pose = Pose(
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
        Mockito.`when`(frame.androidSensorPose).thenReturn(pose2)
        Mockito.`when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsHIGH() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(context)
        val frame = Mockito.mock(Frame::class.java)
        val pose = Pose(
            floatArrayOf(
                (-0.01).toFloat(),
                (-0.01).toFloat(),
                0.01F
            ),
            floatArrayOf(
                0.25F, 0.01F, 0.01F,
                (-0.01).toFloat()
            )
        )
        val pose2 = Mockito.mock(Pose::class.java)
        Mockito.`when`(frame.androidSensorPose).thenReturn(pose2)
        Mockito.`when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        assertEquals(
            Pair(
                FMFrameFilterResult.REJECTED,
                FMFrameFilterFailure.PITCHTOOHIGH
            ), filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsLOW() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(context)
        val frame = Mockito.mock(Frame::class.java)
        val pose = Pose(
            floatArrayOf(
                (-0.01).toFloat(),
                (-0.01).toFloat(),
                0.01F
            ),
            floatArrayOf(
                -0.25F, 0.01F, 0.01F,
                (-0.01).toFloat()
            )
        )
        val pose2 = Mockito.mock(Pose::class.java)
        Mockito.`when`(frame.androidSensorPose).thenReturn(pose2)
        Mockito.`when`(frame.androidSensorPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_0)

        assertEquals(
            Pair(
                FMFrameFilterResult.REJECTED,
                FMFrameFilterFailure.PITCHTOOLOW
            ), filter.accepts(frame)
        )
    }
}