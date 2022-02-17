package com.fantasmo.sdk.filters

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.fantasmo.sdk.config.RemoteConfig
import com.fantasmo.sdk.models.FMFrame
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.ResourceExhaustedException
import java.util.*

/**
 * Class responsible for filtering frames according the implemented filters
 */
class FMFrameFilterChain(context: Context) {

    private val TAG = FMFrameFilterChain::class.java.simpleName

    // the last time a frame was accepted
    private var lastAcceptTime: Long = System.nanoTime()

    // number of seconds after which we force acceptance
    private var acceptanceThreshold: Float

    private var currentFilter: FMFrameFilter? = null

    private lateinit var context: Context

    /**
     * Active frame filters, in order of increasing computational cost
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
            if(rc.isImageEnhancerEnabled) {
                val imageEnhancer = FMAutoGammaCorrectionFilter(
                    rc.imageEnhancerTargetBrightness,
                    context
                )
                filters.add(imageEnhancer)
            }
            if (rc.isImageQualityFilterEnabled) {
                val imageQualityFilter = FMImageQualityFilter(
                    rc.imageQualityFilterScoreThreshold,
                    context
                )
                filters.add(imageQualityFilter)
            }
        }
        this.context = context
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
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun accepts(fmFrame: FMFrame): FMFrameFilterResult {
        if (shouldForceAccept()) {
            lastAcceptTime = System.nanoTime()
            return FMFrameFilterResult.Accepted
        } else {
            try{
                for (filter in filters) {
                    currentFilter = filter
                    val before = SystemClock.elapsedRealtimeNanos()
                    val result = filter.accepts(fmFrame)
                    val after = SystemClock.elapsedRealtimeNanos()
                    val interval = ((after - before) / 1000L).toFloat() / 1000f
                    Log.d(TAG, "Frame ${fmFrame.timestamp} took ${interval}ms through ${filter.TAG}")
                    if (result != FMFrameFilterResult.Accepted) {
                        return result
                    }
                }
            }
            catch (e: NotYetAvailableException) {
                Log.e(TAG, "FrameNotYetAvailable")
            } catch (e: DeadlineExceededException) {
                if (currentFilter != null)
                    Log.e(TAG, "DeadlineExceededException: ${currentFilter?.TAG}")
                else
                    Log.e(TAG, "DeadlineExceededException")
            } catch (e: ResourceExhaustedException) {
                Log.e(TAG, "ResourceExhaustedException")
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

    fun evaluateAsync(fmFrame: FMFrame, completion: (FMFrameFilterResult) -> Unit) {
        val result = accepts(fmFrame)
        completion(result)
     }
}