package com.fantasmo.sdk.analytics

import com.fantasmo.sdk.models.analytics.TrackingStateFrameStatistics
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class TrackingStateFrameStatsTest {

    private val trackingStateFrameStatistics = TrackingStateFrameStatistics()

    @Test
    fun testUpdate(){
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.EXCESSIVE_MOTION)
        trackingStateFrameStatistics.update(frame)
        assertEquals(1,trackingStateFrameStatistics.excessiveMotionEventCount)

        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.BAD_STATE)
        trackingStateFrameStatistics.update(frame)
        assertEquals(1,trackingStateFrameStatistics.lossOfTrackingEventCount)

        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.INSUFFICIENT_FEATURES)
        trackingStateFrameStatistics.update(frame)
        assertEquals(2,trackingStateFrameStatistics.lossOfTrackingEventCount)

        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.INSUFFICIENT_LIGHT)
        trackingStateFrameStatistics.update(frame)
        assertEquals(3,trackingStateFrameStatistics.lossOfTrackingEventCount)

        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.CAMERA_UNAVAILABLE)
        trackingStateFrameStatistics.update(frame)
        assertEquals(4,trackingStateFrameStatistics.lossOfTrackingEventCount)

        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)
        trackingStateFrameStatistics.update(frame)
        assertEquals(1,trackingStateFrameStatistics.normalEventCount)

        assertEquals(6,trackingStateFrameStatistics.totalNumberOfFrames)
    }
}