package com.fantasmo.sdk.views

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.models.ErrorResponse

/**
 * Establishes a protocol between host app and SDK.
 *
 * When a host app wants to implement its' own `localizingView`,
 * these are the methods that it should follow in order to get the correct values.
 */
interface FMLocalizingViewProtocol {
    /**
     * Called when localization has started and the registered `FMLocalizingViewProtocol` is presented.
     */
    fun didStartLocalizing(){}

    /**
     * Called with a corrective behavior request that is intended to be displayed to the user.
     *
     * This method is called to inform the user what they should be doing with their device in order to localize properly. For example
     * if the users device is aimed at the ground, you may receive the `FMBehaviorRequest.TILTUP` request. You should use this method
     * to display any and all behavior requests to the user. For English, the string value in `behavior.description` should be used.
     * @param behavior the requested user behavior
     */
    fun didRequestLocalizationBehavior(behavior: FMBehaviorRequest){}

    /**
     * Called any time a localization result is received. Localization is not stopped.
     *
     * This method may be called multiple times with multiple results during localization. It is up to you to decide whether or not a result
     * is acceptable. As one option, you could check the `result.confidence` value. When you're satisfied with the result you should dismiss
     * the `fmParkingView` or your custom `LocalizingView` instance to stop localizing.
     * @param result the localization result containing the parking `coordinate` and a `confidence` value.
     */
    fun didReceiveLocalizationResult(result: FMLocationResult){}

    /**
     * Called any time a localization error is received. Localization is not stopped.
     *
     * This method may be called multiple times with multiple errors during localization. The localization process is not stopped however and
     * it is still possible to receive a successful localization result. You should determine an acceptable threshold for errors and dismiss
     * the `fmParkingView` or your custom `LocalizingView` when that threshold is reached and resort to fallback options.
     * @param error the localization error
     * @param errorMetadata optional additional information about the error
     */
    fun didReceiveLocalizationError(error: ErrorResponse, errorMetadata: Any?){}
}