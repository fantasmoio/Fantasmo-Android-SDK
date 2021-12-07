package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.util.Log
import com.fantasmo.sdk.config.RemoteConfig
import com.google.ar.core.Frame

/**
 * Class responsible for filtering frames according the implemented filters
 */
class FMFrameFilterChain(context: Context) {

    private val TAG = FMFrameFilterChain::class.java.simpleName

    // the last time a frame was accepted
    private var lastAcceptTime: Long = System.nanoTime()

    // number of seconds after which we force acceptance
    private var acceptanceThreshold: Float

    /**
     * List of filter rules to apply on frame received.
     */
    var filters : MutableList<FMFrameFilter> = mutableListOf()

    val rc: RemoteConfig.Config = RemoteConfig.remoteConfig

    init {
        acceptanceThreshold = rc.frameAcceptanceThresholdTimeout
        if (rc.isTrackingStateFilterEnabled) {
            filters.add(FMTrackingStateFilter())
        }
        if (rc.isCameraPitchFilterEnabled) {
            val cameraPitchFilter = FMCameraPitchFilter(
                rc.cameraPitchFilterMaxDownwardTilt,
                rc.cameraPitchFilterMaxUpwardTilt,
                context
            )
            filters.add(cameraPitchFilter)
        }
        if (rc.isMovementFilterEnabled) {
            val movementFilter = FMMovementFilter(
                rc.movementFilterThreshold
            )
            filters.add(movementFilter)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (rc.isBlurFilterEnabled) {
                val blurFilter = FMBlurFilter(
                    rc.blurFilterVarianceThreshold,
                    rc.blurFilterSuddenDropThreshold,
                    rc.blurFilterAverageThroughputThreshold,
                    context
                )
                filters.add(blurFilter)
            }
            if (rc.isImageQualityFilterEnabled) {
                val imageQualityFilter = FMImageQualityFilter(
                    rc,
                    context
                )
                filters.add(imageQualityFilter)
            }
        }
    }

    /**
     * Init a new Sequence of frames
     */
    fun restart() {
        lastAcceptTime = System.nanoTime()
    }

    /**
     * Check if frame is valid to determine localize result.
     * @param arFrame Frame for approval.
     * @return result FMFrameFilterResult
     */
    fun accepts(arFrame: Frame): FMFrameFilterResult {
        if (shouldForceAccept()) {
            lastAcceptTime = System.nanoTime()
            return FMFrameFilterResult.Accepted
        } else {
            for (filter in filters) {
                val result = filter.accepts(arFrame)
                if (result != FMFrameFilterResult.Accepted) {
                    return result
                }
            }
        }
        lastAcceptTime = System.nanoTime()
        return FMFrameFilterResult.Accepted
    }


    /**
     * Method to Force Approve frame if a certain time has passed in case
     * of every frame in that period was refused
     * @return result: Boolean
     */
    private fun shouldForceAccept(): Boolean {
        //convert to seconds (timestamp is in nanoseconds)
        val elapsed = (System.nanoTime() - lastAcceptTime) / 1_000_000_000
        return elapsed > acceptanceThreshold
    }

    fun evaluateAsync(arFrame: Frame, completion: (FMFrameFilterResult) -> Unit) {
        completion(accepts(arFrame))
    }
}