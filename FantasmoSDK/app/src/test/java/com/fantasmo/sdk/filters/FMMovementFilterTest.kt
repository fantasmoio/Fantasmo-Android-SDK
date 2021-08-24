package com.fantasmo.sdk.filters

import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class FMMovementFilterTest {

    @Test
    fun testMovementFilterAccepts() {
        val filter = FMMovementFilter()
        val frame = Mockito.mock(Frame::class.java)
        val pose = Pose(
            floatArrayOf(
                (-0.92).toFloat(),
                (-0.92).toFloat(),
                0.63F
            ),
            floatArrayOf(
                0.8F, 0.03F, 0.6F,
                (-0.005).toFloat()
            )
        )
        val camera = Mockito.mock(Camera::class.java)
        val pose2 = Mockito.mock(Pose::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(pose.translation)

        assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            filter.accepts(frame)
        )
    }

    @Test
    fun testMovementFilterRejects() {
        val filter = FMMovementFilter()
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
        val camera = Mockito.mock(Camera::class.java)
        val pose2 = Mockito.mock(Pose::class.java)
        `when`(frame.camera).thenReturn(camera)
        `when`(frame.camera.pose).thenReturn(pose2)
        `when`(frame.camera.pose.translation).thenReturn(pose.translation)

        assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE),
            filter.accepts(frame)
        )
    }
}