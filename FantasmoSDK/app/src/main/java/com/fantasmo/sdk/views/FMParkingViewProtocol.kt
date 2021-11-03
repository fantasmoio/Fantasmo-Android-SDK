package com.fantasmo.sdk.views

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.models.ErrorResponse

interface FMParkingViewProtocol {

    /**
     * Called when QR scanning has started and the registered `FMQRScanningViewProtocol` is presented.
     */
    fun fmParkingViewDidStartQRScanning(){}

    /**
     * Called when QR scanning has stopped, just before the registered `QRScanningViewProtocol` is dismissed.
     */
    fun fmParkingViewDidStopQRScanning(){}

    /**
     * Called when a QR code is scanned. This method can be used to perform optional validation of the QR code before localization starts.
     *
     * If you implement this method, you *must* eventually call the `onValidQRCode` with a boolean value. A value of `true` indicates
     * the code is valid and that localization should start. Passing `false` to this block indicates the code is invalid and instructs
     * the parking view to scan for more QR codes. This block may be called synchronously or asynchronously but must be done so on the
     * main queue. The default implementation of this method does nothing and will start localizing after any QR code is detected.
     * @param qrCode the scanned QR code value as String
     * @param onValidQRCode a block to be called with a boolean value indicating whether or not to continue to localization.
     */
    fun fmParkingView(qrCode: String, onValidQRCode: (Boolean) -> Unit){}

    /**
     * Called when localization has started and the registered `FMLocalizingViewProtocol` is presented.
     */
    fun fmParkingViewDidStartLocalizing(){}

    /**
     * Called with a corrective behavior request that is intended to be displayed to the user.
     *
     * This method is called to inform the user what they should be doing with their device in order to localize properly. For example
     * if the users device is aimed at the ground, you may receive the `FMBehaviorRequest.TILTUP` request. You should use this method
     * to display any and all behavior requests to the user. For English, the string value in `behavior.description` should be used.
     * @param behavior the requested user behavior
     */
    fun fmParkingView(behavior: FMBehaviorRequest){}

    /**
     * Called any time a localization result is received. Localization is not stopped.
     *
     * This method may be called multiple times with multiple results during localization. It is up to you to decide whether or not a result
     * is acceptable. As one option, you could check the `result.confidence` value. When you're satisfied with the result you should dismiss
     * the `fmParkingView` instance to stop localizing.
     * @param result the localization result containing the parking `coordinate` and a `confidence` value.
     */
    fun fmParkingView(result: FMLocationResult){}

    /**
     * Called any time a localization error is received. Localization is not stopped.
     *
     * This method may be called multiple times with multiple errors during localization. The localization process is not stopped however and
     * it is still possible to receive a successful localization result. You should determine an acceptable threshold for errors and dismiss
     * the `fmParkingView` when that threshold is reached and resort to fallback options.
     * @param error the localization error
     * @param metadata optional additional information about the error
     */
    fun fmParkingView(error: ErrorResponse, metadata: Any?){}
}