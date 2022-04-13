package com.fantasmo.sdk.filters

import android.content.Context
import android.view.Display
import android.view.Surface
import com.fantasmo.sdk.config.RemoteConfigTest
import com.fantasmo.sdk.models.FMFrame
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class FMCameraPitchTest {

    @Test
    fun testPitchFilterAccepts() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
        val pose = Pose(
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
        val pose2 = Mockito.mock(Pose::class.java)
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
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
            FMFrameFilterResult.Accepted,
            filter.accepts(frame)
        )
    }

    @Test
    fun testPitchFilterRejectsPortraitHIGH() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
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
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
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
            FMFrameFilterRejectionReason.PITCH_TOO_HIGH,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsPortraitLOW() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
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
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
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
            FMFrameFilterRejectionReason.PITCH_TOO_LOW,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsReversePortraitHIGH() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
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
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_180)

        assertEquals(
            FMFrameFilterRejectionReason.PITCH_TOO_HIGH,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsReversePortraitLOW() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
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
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_180)

        assertEquals(
            FMFrameFilterRejectionReason.PITCH_TOO_LOW,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsLandscapeHIGH() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
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
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_90)

        assertEquals(
            FMFrameFilterRejectionReason.PITCH_TOO_HIGH,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsLandscapeLOW() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
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
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_90)

         assertEquals(
             FMFrameFilterRejectionReason.PITCH_TOO_LOW,
             filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsReverseLandscapeHIGH() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
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
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_270)

        assertEquals(
            FMFrameFilterRejectionReason.PITCH_TOO_HIGH,
            filter.accepts(frame).getRejectedReason()
        )
    }

    @Test
    fun testPitchFilterRejectsReverseLandscapeLOW() {
        val context = Mockito.mock(Context::class.java)
        val filter = FMCameraPitchFilter(
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxDownwardTilt,
            RemoteConfigTest.remoteConfig.cameraPitchFilterMaxUpwardTilt,
            context)
        val frame = Mockito.mock(FMFrame::class.java)
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
        Mockito.`when`(frame.cameraPose).thenReturn(pose2)
        Mockito.`when`(frame.cameraPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(frame.camera).thenReturn(camera)
        Mockito.`when`(frame.camera.displayOrientedPose).thenReturn(pose2)
        Mockito.`when`(frame.camera.displayOrientedPose.rotationQuaternion)
            .thenReturn(pose.rotationQuaternion)

        val display = Mockito.mock(Display::class.java)
        Mockito.`when`(context.display).thenReturn(display)
        Mockito.`when`(context.display?.rotation!!).thenReturn(Surface.ROTATION_270)

        assertEquals(
            FMFrameFilterRejectionReason.PITCH_TOO_LOW,
            filter.accepts(frame).getRejectedReason()
        )
    }
}