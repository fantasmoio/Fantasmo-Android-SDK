package com.fantasmo.sdk.filters

import com.fantasmo.sdk.frameSequenceFilter.FMCameraPitchFilterRule
import com.fantasmo.sdk.frameSequenceFilter.FMFrameFilterFailure
import com.fantasmo.sdk.frameSequenceFilter.FMFrameFilterResult
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class FMCameraPitchTest {

    @Test
    fun testPitchFilterAccepts() {
        val filter = FMCameraPitchFilterRule()
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

        assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            filter.check(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsHIGH() {
        val filter = FMCameraPitchFilterRule()
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

        assertEquals(
            Pair(
                FMFrameFilterResult.REJECTED,
                FMFrameFilterFailure.PITCHTOOHIGH
            ), filter.check(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsLOW() {
        val filter = FMCameraPitchFilterRule()
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

        assertEquals(
            Pair(
                FMFrameFilterResult.REJECTED,
                FMFrameFilterFailure.PITCHTOOLOW
            ), filter.check(frame)
        )
    }
}