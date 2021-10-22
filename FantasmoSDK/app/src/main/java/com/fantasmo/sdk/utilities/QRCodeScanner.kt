package com.fantasmo.sdk.utilities

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.fantasmo.sdk.views.FMParkingViewProtocol
import com.fantasmo.sdk.views.FMQRScanningViewProtocol
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * QRCodeReader - class responsible for getting a frame form ARCore and check
 * if there's a QRCode.
 */
class QRCodeScanner(
    var fmParkingViewController: FMParkingViewProtocol,
    private var fmQrScanningViewController: FMQRScanningViewProtocol,
    private var qrCodeScannerListener: QRCodeScannerListener
) {
    // This prevents the qrCodeReader to be overflowed with frames to analyze
    enum class State{
        QRCODEDETECTED,
        QRSCANNING,
        IDLE
    }

    var qrCodeReaderEnabled: Boolean = false
    var state = State.IDLE
    private val TAG = QRCodeScanner::class.java.simpleName
    private var imageWidth = 0
    private var imageHeight = 0
    private var qrFound = false

    /**
     * Gets a frame from ARCore and converts it to bitmap and proceeds with
     * the mlKit barcode scanner analysis
     */
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    fun processImage(
        arFrame: Frame
    ) {
        if(!qrCodeReaderEnabled && state == State.QRCODEDETECTED){
            return
        }
        state = State.QRSCANNING
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        // BarcodeScannerOptions.Builder()
        //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        //     .build();
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE)
            .build()
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(options)

        val byteBuffers = acquireFrameImage(arFrame)
        GlobalScope.launch(Dispatchers.Default){
            if(byteBuffers == null){
                state = State.IDLE
            }else{
                val baOutputStream = createByteArrayOutputStream(byteBuffers[0],byteBuffers[1],byteBuffers[2])
                val imageBytes: ByteArray = baOutputStream.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val inputImage =
                    InputImage.fromBitmap(bitmap!!, 0)

                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        barcodes.forEach {
                            val value = it.rawValue!!
                            Log.d(TAG, value)
                            displayQRScanResult(value)
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, it.message!!)
                    }.addOnCompleteListener {
                        // When the image is from CameraX analysis use case, must call image.close() on received
                        // images when finished using them. Otherwise, new images may not be received or the camera
                        // may stall.
                        state = if(qrFound){
                            qrCodeReaderEnabled = false
                            Log.d(TAG, "QR Code Reader Disabled")
                            State.QRCODEDETECTED
                        }else{
                            qrCodeReaderEnabled = true
                            State.IDLE
                        }
                    }
            }
        }
    }

    private fun displayQRScanResult(value: String){
        qrFound = true
        val stringScan = "QRCodeDetected with value: $value"
        fmQrScanningViewController.didScanQRCode(stringScan)
        fmParkingViewController.fmParkingView(value){
            if(it){
                qrCodeScannerListener.deployLocalizing()
            }
            else{
                Log.d(TAG,"REFUSED")
                qrFound = false
                qrCodeScannerListener.deployQRScanning()
            }
        }
    }

    private fun createByteArrayOutputStream(
        cameraPlaneY: ByteBuffer,
        cameraPlaneU: ByteBuffer,
        cameraPlaneV: ByteBuffer
    ): ByteArrayOutputStream {

        val compositeByteArray =
            ByteArray(cameraPlaneY.capacity() + cameraPlaneU.capacity() + cameraPlaneV.capacity())

        cameraPlaneY.get(compositeByteArray, 0, cameraPlaneY.capacity())
        cameraPlaneU.get(compositeByteArray, cameraPlaneY.capacity(), cameraPlaneU.capacity())
        cameraPlaneV.get(
            compositeByteArray,
            cameraPlaneY.capacity() + cameraPlaneU.capacity(),
            cameraPlaneV.capacity()
        )

        val baOutputStream = ByteArrayOutputStream()
        val yuvImage = YuvImage(
            compositeByteArray,
            ImageFormat.NV21,
            imageWidth,
            imageHeight,
            null
        )
        yuvImage.compressToJpeg(
            Rect(0, 0, imageWidth, imageHeight),
            100,
            baOutputStream
        )
        return baOutputStream
    }

    /**
     * Acquires the image from the ARCore frame catching all
     * exceptions that could happen during localizing session
     * @param arFrame: Frame
     * @return ByteArrayOutputStream or null in case of exception
     */
    private fun acquireFrameImage(arFrame: Frame): Array<ByteBuffer>? {
        try {
            val cameraImage = arFrame.acquireCameraImage()
            arFrame.acquireCameraImage().close()
            //The camera image received is in YUV YCbCr Format. Get buffers for each of the planes and use
            // them to create a new byte array defined by the size of all three buffers combined
            val cameraPlaneY = cameraImage.planes[0].buffer
            val cameraPlaneU = cameraImage.planes[1].buffer
            val cameraPlaneV = cameraImage.planes[2].buffer
            imageWidth = cameraImage.width
            imageHeight = cameraImage.height
            // Release the image
            cameraImage.close()
            return arrayOf(cameraPlaneY,cameraPlaneU,cameraPlaneV)
        } catch (e: NotYetAvailableException) {
            Log.d(TAG, "FrameNotYetAvailable")
        } catch (e: DeadlineExceededException) {
            Log.d(TAG, "DeadlineExceededException")
        }
        return null
    }
}

interface QRCodeScannerListener {
    fun deployQRScanning()
    fun deployLocalizing()
}
