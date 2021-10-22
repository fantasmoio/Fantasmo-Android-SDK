package com.fantasmo.sdk.views

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class FMQRScanningView(var fmQRScanningView: ConstraintLayout, var qrCodeResultView: TextView) {

    fun hide() {
        if (fmQRScanningView.visibility == View.VISIBLE) {
            fmQRScanningView.visibility = View.GONE
        }
    }

    fun display() {
        if (fmQRScanningView.visibility == View.GONE) {
            fmQRScanningView.visibility = View.VISIBLE
        }
    }

    fun displayQRCodeResult(result: String) {
        qrCodeResultView.text = result
    }
}