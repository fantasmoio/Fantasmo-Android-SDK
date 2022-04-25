package com.fantasmo.sdk.filters

import com.fantasmo.sdk.models.FMFrame
import com.google.ar.core.Camera
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class FMTrackingStateFilterTest {

    @Test
    fun testPitchFilterAccepts() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(FMFrame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        Assert.assertEquals(
            FMFrameFilterResult.Accepted,
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsBadState() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(FMFrame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.BAD_STATE)

        Assert.assertEquals(
            FMFrameFilterRejectionReason.MOVING_TOO_FAST,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsExcessiveMotion() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(FMFrame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.EXCESSIVE_MOTION)

        Assert.assertEquals(
            FMFrameFilterRejectionReason.MOVING_TOO_FAST,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsCameraUnavailable() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(FMFrame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.CAMERA_UNAVAILABLE)

        Assert.assertEquals(
            FMFrameFilterRejectionReason.MOVING_TOO_LITTLE,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsInsufficientLight() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(FMFrame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.INSUFFICIENT_LIGHT)

        Assert.assertEquals(
            FMFrameFilterRejectionReason.INSUFFICIENT_FEATURES,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsInsufficientFeatures() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(FMFrame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.INSUFFICIENT_FEATURES)

        Assert.assertEquals(
            FMFrameFilterRejectionReason.INSUFFICIENT_FEATURES,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsInitializing() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(FMFrame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingState).thenReturn(TrackingState.PAUSED)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)

        Assert.assertEquals(
            FMFrameFilterRejectionReason.MOVING_TOO_LITTLE,
            filter.accepts(frame).getRejectedReason()
        )
    }
}