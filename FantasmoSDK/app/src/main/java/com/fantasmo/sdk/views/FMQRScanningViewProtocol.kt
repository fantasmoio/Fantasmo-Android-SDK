package com.fantasmo.sdk.views

interface FMQRScanningViewProtocol{
    fun didStartQRScanning(){}
    fun didStopQRScanning(){}
    fun didScanQRCode(result: String){}
}