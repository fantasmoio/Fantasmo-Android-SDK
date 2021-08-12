package com.fantasmo.sdk.models.analytics

import com.fantasmo.sdk.FMUtility.Companion.distance
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason

/**
 * Class responsible for tracking total translation movement during the session
 * Calculates the trajectory length of a device based on the device translation provided by the sequence of ARCore frames
 * To reduce error introduced by noise, we use decimation (downsampling without passing through low-pass filter) by an integer factor as the most
 * simple approach which should yield sufficient accuracy.
 * More info about downsampling and decimation can be read at https://en.wikipedia.org/wiki/Downsampling_(signal_processing)
 */
class TotalDeviceTranslationAccumulator(private val decimationFactor: Int) {

    // Current value of total translation in meters, which is updated as more frames are passed via `update(`
    var totalTranslation = 0f

    private var frameCounter: Int = 0
    private var nextFrameToTake = 0
    private var previousTranslation = floatArrayOf()

    /**
     * Every n-th frame is taken, where n = `decimationFactor`.
     * Frames with limited tracking state are omitted.
     * @param arFrame: Frame
     */
    fun update(arFrame: Frame) {
        if (previousTranslation.isEmpty()) {
            previousTranslation = arFrame.camera.pose.translation
        }
        if (frameCounter >= nextFrameToTake) {
            if (arFrame.camera.trackingFailureReason == TrackingFailureReason.NONE) {
                val translation = arFrame.camera.pose.translation
                totalTranslation += distance(translation!!, previousTranslation)
                previousTranslation = translation
                nextFrameToTake += decimationFactor
            } else {
                nextFrameToTake += 1
            }
        }
        frameCounter += 1
    }

    /**
     * Resets Counters
     */
    fun reset() {
        frameCounter = 0
        nextFrameToTake = 0
        previousTranslation = floatArrayOf()
        totalTranslation = 0f
    }
}
