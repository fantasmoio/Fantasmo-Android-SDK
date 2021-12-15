package com.fantasmo.sdk.views

import android.graphics.Paint
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.fantasmosdk.R

/**
 * Default View for the QR Code Scanning session.
 */
class FMQRScanningView(arLayout: CoordinatorLayout, private val fmParkingView: FMParkingView){

    private var fmQRScanningView: ConstraintLayout = arLayout.findViewById(R.id.fmQRView)
    private var qrCodeResultView: TextView = arLayout.findViewById(R.id.qrCodeResultTextView)
    private var closeButton: ImageButton = fmQRScanningView.findViewById(R.id.fmExitButton)
    private var skipButton: TextView = fmQRScanningView.findViewById(R.id.fmSkipButton)

    init {
        closeButton.setOnClickListener {
            fmParkingView.dismiss()
        }
        skipButton.paintFlags = skipButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        skipButton.setOnClickListener {
            fmParkingView.skipQRScanning()
        }
    }

    /**
     * When a QR Code Scanning session ends, this method provides the ability to hide the QR Code Scanning view.
     */
    fun hide() {
        if (fmQRScanningView.visibility == View.VISIBLE) {
            fmQRScanningView.visibility = View.GONE
        }
    }

    /**
     * When a QR Code Scanning session begins, this method provides the ability to display the QR Code Scanning view.
     */
    fun display() {
        if (fmQRScanningView.visibility == View.GONE) {
            fmQRScanningView.visibility = View.VISIBLE
        }
        if (qrCodeResultView.visibility == View.GONE) {
            qrCodeResultView.visibility = View.VISIBLE
        }
    }

    /**
     * Method responsible for displaying to the user the result of the QR Code Scanning session.
     */
    fun displayQRCodeResult(result: String) {
        qrCodeResultView.text = result
        if (qrCodeResultView.visibility == View.GONE) {
            qrCodeResultView.visibility = View.VISIBLE
        }
        val timer = object : CountDownTimer(2000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                qrCodeResultView.visibility = View.GONE
                val resetString = "Scan QR Code"
                qrCodeResultView.text = resetString
            }
        }.start()
    }
}