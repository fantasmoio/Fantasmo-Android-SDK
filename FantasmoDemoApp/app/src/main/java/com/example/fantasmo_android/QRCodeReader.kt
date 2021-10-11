package com.example.fantasmo_android

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import android.widget.TextView
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

/**
 * QRCodeReader - class responsible for getting a frame form ARCore and check
 * if there's a QRCode.
 */
class QRCodeReader(
    private val urlView: TextView
) {
    var qrReading = false
    private val TAG = QRCodeReader::class.java.simpleName

    /**
     * Gets a frame from ARCore and converts it to bitmap and proceeds with
     * the mlKit barcode scanner analysis
     */
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    fun processImage(
        arFrame: Frame
    ) {
        qrReading = true
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

        val baOutputStream = acquireFrameImage(arFrame)
        GlobalScope.launch(Dispatchers.Default){
            if(baOutputStream == null){
                qrReading = false
            }else{
                val imageBytes: ByteArray = baOutputStream.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val inputImage =
                    InputImage.fromBitmap(bitmap!!, 0)

                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        barcodes.forEach {
                            val value = it.rawValue!!
                            Log.d(TAG, value)
                            urlView.text = value
                            qrReading = false
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, it.message!!)
                    }.addOnCompleteListener {
                        // When the image is from CameraX analysis use case, must call image.close() on received
                        // images when finished using them. Otherwise, new images may not be received or the camera
                        // may stall.
                        qrReading = false
                    }
            }
        }
    }

    private fun createByteArrayOutputStream(cameraImage: Image): ByteArrayOutputStream {
        //The camera image received is in YUV YCbCr Format. Get buffers for each of the planes and use
        // them to create a new byte array defined by the size of all three buffers combined
        val cameraPlaneY = cameraImage.planes[0].buffer
        val cameraPlaneU = cameraImage.planes[1].buffer
        val cameraPlaneV = cameraImage.planes[2].buffer

        //Use the buffers to create a new byteArray that
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
            cameraImage.width,
            cameraImage.height,
            null
        )
        yuvImage.compressToJpeg(
            Rect(0, 0, cameraImage.width, cameraImage.height),
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
    private fun acquireFrameImage(arFrame: Frame): ByteArrayOutputStream? {
        try {
            val cameraImage = arFrame.acquireCameraImage()
            arFrame.acquireCameraImage().close()

            val baOutputStream = createByteArrayOutputStream(cameraImage)
            // Release the image
            cameraImage.close()
            return baOutputStream
        } catch (e: NotYetAvailableException) {
            Log.d(TAG, "FrameNotYetAvailable")
        } catch (e: DeadlineExceededException) {
            Log.d(TAG, "DeadlineExceededException")
        }
        return null
    }
}