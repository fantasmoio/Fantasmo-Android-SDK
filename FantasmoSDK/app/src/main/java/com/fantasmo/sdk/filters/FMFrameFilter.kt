package com.fantasmo.sdk.filters

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.models.FMFrameRejectionReason

sealed class FMFrameFilterResult {
    object Accepted : FMFrameFilterResult()
    class Rejected(val reason: FMFrameRejectionReason): FMFrameFilterResult()

    fun getRejectedReason(): FMFrameRejectionReason? {
        return when (this){
            is Rejected -> reason
            else -> null
        }
    }
}

/**
 * Prime filters are original blocks for a compound frame filter or can be used alone as a standalone filter.
 */
interface FMFrameFilter {
    val TAG: String
    fun accepts(fmFrame: FMFrame): FMFrameFilterResult
}