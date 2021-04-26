package com.fantasmo.sdk.frameSequenceFilter

import com.google.ar.core.Frame

/**
 * Class responsible for filtering frames due to blur on images.
 * Prevents from sending blurred images.
 */
class FMBlurFilterRule : FMFrameSequenceFilterRule {

    /**
     * Check frame acceptance.
     * @param arFrame: Frame to be evaluated
     * @return Accepts frame or Rejects frame with MovingTooFast failure
     */
    override fun check(arFrame: Frame): Pair<FMFrameFilterResult,FMFrameFilterFailure> {
        //TODO: Implement blur filter
        return Pair(FMFrameFilterResult.ACCEPTED,FMFrameFilterFailure.ACCEPTED)
    }
}