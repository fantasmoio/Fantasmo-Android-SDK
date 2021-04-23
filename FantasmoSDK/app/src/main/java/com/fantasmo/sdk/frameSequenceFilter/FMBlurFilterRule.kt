package com.fantasmo.sdk.frameSequenceFilter

import com.google.ar.core.Frame

class FMBlurFilterRule : FMFrameSequenceFilterRule {

    override fun check(arFrame: Frame): Pair<FMFrameFilterResult,FMFrameFilterFailure> {
        return Pair(FMFrameFilterResult.ACCEPTED,FMFrameFilterFailure.ACCEPTED)
    }
}