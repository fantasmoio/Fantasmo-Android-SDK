package com.fantasmo.sdk.views

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.models.ErrorResponse

interface FMLocalizingViewProtocol {
    fun didStartLocalizing(){}
    fun didRequestLocalizationBehavior(behavior: FMBehaviorRequest){}
    fun didReceiveLocalizationResult(result: FMLocationResult){}
    fun didReceiveLocalizationError(error: ErrorResponse, errorMetadata: Any?){}
}