package com.fantasmo.sdk.filters

import com.fantasmo.sdk.config.RemoteConfigTest
import com.fantasmo.sdk.models.FMFrame
import com.google.ar.core.Pose
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class FMMovementFilterTest {

    @Test
    fun testMovementFilterAccepts() {
        val filter = FMMovementFilter(RemoteConfigTest.remoteConfig.movementFilterThreshold)
        val frame = Mockito.mock(FMFrame::class.java)
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
        `when`(frame.cameraPose).thenReturn(pose)

        assertEquals(
            FMFrameFilterResult.Accepted,
            filter.accepts(frame)
        )
    }

    @Test
    fun testMovementFilterRejects() {
        val filter = FMMovementFilter(RemoteConfigTest.remoteConfig.movementFilterThreshold)
        val frame = Mockito.mock(FMFrame::class.java)
        val pose = Pose(
            floatArrayOf(
                (-0.001).toFloat(),
                (-0.001).toFloat(),
                0.001F
            ),
            floatArrayOf(
                0F, 0F, 0F, 1F
            )
        )
        `when`(frame.cameraPose).thenReturn(pose)

        assertEquals(
            FMFilterRejectionReason.MOVINGTOOLITTLE,
            filter.accepts(frame).getRejectedReason()
        )
    }
}