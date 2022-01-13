package com.fantasmo.sdk.views

import android.content.Context
import android.graphics.Paint
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.fantasmosdk.R

/**
 * Default View for the QR Code Scanning session.
 */
class FMQRScanningView(
    private val context: Context,
    arLayout: CoordinatorLayout,
    private val fmParkingView: FMParkingView
) {

    private var fmQRScanningView: ConstraintLayout = arLayout.findViewById(R.id.fmQRView)
    private var qrCodeResultView: TextView = arLayout.findViewById(R.id.qrCodeResultTextView)
    private var closeButton: ImageButton = fmQRScanningView.findViewById(R.id.fmExitButton)
    private var enterQRButton: TextView = fmQRScanningView.findViewById(R.id.fmEnterQRButton)


    init {
        closeButton.setOnClickListener {
            fmParkingView.dismiss()
        }
        enterQRButton.paintFlags = enterQRButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        enterQRButton.setOnClickListener {
            handleManualEntryButton()
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

    private fun handleManualEntryButton() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        // Get the layout inflater
        val inflater = LayoutInflater.from(context).inflate(R.layout.fmenterqr_dialog, null)
        val editTextQR: EditText = inflater.findViewById(R.id.fmEditTextQRCode)

        // Inflate and set the layout for the dialog
        builder.setView(inflater!!)
            // Add action buttons
            .setCancelable(false)
            .setPositiveButton(
                "Submit"
            ) { _, _ ->
                Log.d("Dialog", editTextQR.text.toString())
                fmParkingView.enterQRCode(editTextQR.text.toString())
            }
            .setNegativeButton(
                "Cancel"
            ) { dialog, _ ->
                dialog.cancel()
            }
        val alert: AlertDialog = builder.create()
        alert.show()
        alert.withCenteredButtons()
    }

    private fun AlertDialog.withCenteredButtons() {
        val positive = getButton(AlertDialog.BUTTON_POSITIVE)
        val negative = getButton(AlertDialog.BUTTON_NEGATIVE)

        //Disable the material spacer view in case there is one
        val parent = positive.parent as? LinearLayout
        parent?.gravity = Gravity.CENTER_HORIZONTAL
        val leftSpacer = parent?.getChildAt(1)
        leftSpacer?.visibility = View.GONE

        //Force the default buttons to center
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layoutParams.weight = 1f
        layoutParams.gravity = Gravity.CENTER

        positive.layoutParams = layoutParams
        negative.layoutParams = layoutParams
    }
}