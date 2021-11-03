package com.fantasmo.sdk.views

/**
 * Establishes a protocol between host app and SDK.
 *
 * When a host app wants to implement its' own `qrCodeScanView`,
 * these are the methods that it should follow in order to get the correct values.
 */
interface FMQRScanningViewProtocol{
    /**
     * Called when QR scanning has started and the registered `FMQRScanningViewProtocol` is presented.
     */
    fun didStartQRScanning(){}

    /**
     * Called when QR scanning has stopped, just before the registered `QRScanningViewProtocol` is dismissed.
     */
    fun didStopQRScanning(){}

    /**
     * Called when a QR code is scanned.
     * This method can't be used to perform optional validation of the QR code before localization starts.
     * @param result the scanned QR code value as String
     */
    fun didScanQRCode(result: String){}
}