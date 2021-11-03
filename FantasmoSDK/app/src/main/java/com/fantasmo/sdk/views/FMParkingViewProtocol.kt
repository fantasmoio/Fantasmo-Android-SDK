package com.fantasmo.sdk.views

import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.FMLocationResult
import com.fantasmo.sdk.models.ErrorResponse

interface FMParkingViewProtocol {
    fun fmParkingViewDidStartQRScanning(){}
    fun fmParkingViewDidStopQRScanning(){}
    fun fmParkingView(qrCode: String, onValidQRCode: (Boolean) -> Unit){}
    fun fmParkingViewDidStartLocalizing(){}
    fun fmParkingView(behavior: FMBehaviorRequest){}
    fun fmParkingView(result: FMLocationResult){}
    fun fmParkingView(error: ErrorResponse, metadata: Any?){}
}