package com.fantasmo.sdk.filters

import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterFailure
import com.fantasmo.sdk.filters.primeFilters.FMFrameFilterResult
import com.fantasmo.sdk.filters.primeFilters.FMTrackingStateFilter
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class FMTrackingStateFilterTest {

    @Test
    fun testPitchFilterAccepts() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingState).thenReturn(TrackingState.TRACKING)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.ACCEPTED, FMFrameFilterFailure.ACCEPTED),
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsBadState() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.BAD_STATE)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOFAST),
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsExcessiveMotion() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.EXCESSIVE_MOTION)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOFAST),
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsCameraUnavailable() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.CAMERA_UNAVAILABLE)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE),
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsInsufficientLight() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.INSUFFICIENT_LIGHT)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.INSUFFICIENTFEATURES),
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsInsufficientFeatures() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.INSUFFICIENT_FEATURES)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.INSUFFICIENTFEATURES),
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsInitializing() {
        val filter = FMTrackingStateFilter()
        val frame = Mockito.mock(Frame::class.java)
        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.trackingState).thenReturn(TrackingState.PAUSED)
        Mockito.`when`(frame.camera.trackingFailureReason).thenReturn(TrackingFailureReason.NONE)

        Assert.assertEquals(
            Pair(FMFrameFilterResult.REJECTED, FMFrameFilterFailure.MOVINGTOOLITTLE),
            filter.accepts(frame)
        )
    }
}