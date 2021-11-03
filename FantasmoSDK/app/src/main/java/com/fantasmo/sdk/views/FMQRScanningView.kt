package com.fantasmo.sdk.views

import android.os.CountDownTimer
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.fantasmosdk.R

class FMQRScanningView(arLayout: CoordinatorLayout, private val fmParkingView: FMParkingView){

    private var fmQRScanningView: ConstraintLayout = arLayout.findViewById(R.id.fmQRView)
    private var qrCodeResultView: TextView = arLayout.findViewById(R.id.qrCodeResultTextView)
    private var closeButton: ImageButton = fmQRScanningView.findViewById(R.id.fmExitButton)

    init {
        closeButton.setOnClickListener {
            fmParkingView.dismiss()
        }
    }
    fun hide() {
        if (fmQRScanningView.visibility == View.VISIBLE) {
            fmQRScanningView.visibility = View.GONE
        }
    }

    fun display() {
        if (fmQRScanningView.visibility == View.GONE) {
            fmQRScanningView.visibility = View.VISIBLE
        }
        if (qrCodeResultView.visibility == View.GONE) {
            qrCodeResultView.visibility = View.VISIBLE
        }
    }

    fun displayQRCodeResult(result: String) {
        qrCodeResultView.text = result
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