package com.fantasmo.sdk.utilities

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.util.Log
import com.fantasmo.sdk.models.FMFrame
import com.fantasmo.sdk.views.FMParkingViewProtocol
import com.fantasmo.sdk.views.FMQRScanningViewProtocol
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * QRCodeReader - class responsible for getting a frame form ARCore and check
 * if there's a QRCode.
 */
internal class QRCodeScanner(
    var fmParkingViewController: FMParkingViewProtocol,
    private var fmQrScanningViewController: FMQRScanningViewProtocol,
    private var qrCodeScannerListener: QRCodeScannerListener
) {

    private val TAG = QRCodeScanner::class.java.simpleName

    // This prevents the qrCodeReader to be overflowed with frames to analyze
    private enum class State {
        QRCODEDETECTED,
        QRSCANNING,
        IDLE
    }

    private var qrCodeReaderEnabled: Boolean = false
    private var state = State.IDLE
    private var qrFound = false

    /**
     * Gets a frame from ARCore and converts it to bitmap and proceeds with
     * the mlKit barcode scanner analysis
     */
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    fun processImage(
        fmFrame: FMFrame
    ) {
        if (!qrCodeReaderEnabled && state == State.QRCODEDETECTED) {
            return
        }
        // Only read frame if the qrCodeReader is enabled and only if qrCodeReader is in reading mode
        if (canScanFrame()) {
            state = State.QRSCANNING
            // Note that if you know which format of barcode your app is dealing with, detection will be
            // faster to specify the supported barcode formats one by one, e.g.
            // BarcodeScannerOptions.Builder()
            //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            //     .build();
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE
                )
                .build()
            val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(options)

            val yuvImage = fmFrame.yuvImage
            GlobalScope.launch(Dispatchers.Default) {
                if (yuvImage == null) {
                    state = State.IDLE
                } else {
                    val inputImage =
                        InputImage.fromByteArray(yuvImage.yuvData, yuvImage.width, yuvImage.height, 0, ImageFormat.NV21)

                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            barcodes.forEach {
                                displayQRScanBarcodeResult(it)
                            }
                        }
                        .addOnFailureListener {
                            Log.e(TAG, it.message!!)
                        }.addOnCompleteListener {
                            // When the image is from CameraX analysis use case, must call image.close() on received
                            // images when finished using them. Otherwise, new images may not be received or the camera
                            // may stall.
                            state = if (qrFound) {
                                qrCodeReaderEnabled = false
                                State.QRCODEDETECTED
                            } else {
                                qrCodeReaderEnabled = true
                                State.IDLE
                            }
                        }
                }
            }
        }
    }

    private fun displayQRScanBarcodeResult(qrCode: Barcode){
        qrCodeScannerListener.qrCodeScanned()
        qrFound = true
        val stringScan = "QRCodeDetected with value: ${qrCode.rawValue}"
        fmQrScanningViewController.didScanQRCode(stringScan)
        fmParkingViewController.fmParkingView(qrCode) {
            if (it) {
                Log.d(TAG, "QR CODE ACCEPTED")
                qrCodeScannerListener.deployLocalizing()
            } else {
                Log.d(TAG, "QR CODE REFUSED")
                qrFound = false
                qrCodeScannerListener.deployQRScanning()
            }
        }
    }

    fun startQRScanner() {
        qrCodeReaderEnabled = true
        state = State.IDLE
    }

    private fun canScanFrame(): Boolean {
        return (qrCodeReaderEnabled && state == State.IDLE)
    }
}

internal interface QRCodeScannerListener {
    fun deployQRScanning()
    fun deployLocalizing()
    fun qrCodeScanned()
}
