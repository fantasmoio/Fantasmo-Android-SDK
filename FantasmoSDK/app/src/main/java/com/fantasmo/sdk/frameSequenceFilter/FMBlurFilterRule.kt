package com.fantasmo.sdk.frameSequenceFilter

import com.google.ar.core.Frame

class FMBlurFilterRule : FMFrameSequenceFilterRule {

    /**
     * Check frame acceptance
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with MovingTooFast failure
     */
    override fun check(arFrame: Frame): Pair<FMFrameFilterResult,FMFrameFilterFailure> {
        //TODO: Implement blur filter
        return Pair(FMFrameFilterResult.ACCEPTED,FMFrameFilterFailure.ACCEPTED)
    }
}