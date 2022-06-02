package com.fantasmo.sdk.views

import android.os.CountDownTimer
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.FMBehaviorRequest
import com.fantasmo.sdk.fantasmosdk.R

/**
 * Default View for the Localization session.
 */
internal class FMLocalizingView(arLayout: CoordinatorLayout, private val fmParkingView: FMParkingView) {

    private var fmLocalizingView: ConstraintLayout = arLayout.findViewById(R.id.fmLocalizeView)
    private var filterResultView: TextView = arLayout.findViewById(R.id.filterRejectionTextView)
    private var closeButton: ImageButton = fmLocalizingView.findViewById(R.id.fmExitButton)

    init {
        closeButton.setOnClickListener {
            fmParkingView.dismiss()
        }
    }
    /**
     * When a localization session ends, this method provides the ability to hide the Localization view.
     */
    fun hide() {
        if (fmLocalizingView.visibility == View.VISIBLE) {
            fmLocalizingView.visibility = View.GONE
        }
    }

    /**
     * When a localization session begins, this method provides the ability to display the Localization view.
     */
    fun display() {
        if (fmLocalizingView.visibility == View.GONE) {
            fmLocalizingView.visibility = View.VISIBLE
        }
    }

    /**
     * Method responsible for displaying to the user the behavior to follow.
     */
    fun displayFilterResult(behavior: FMBehaviorRequest) {
        filterResultView.text = behavior.description
        if (filterResultView.visibility == View.GONE) {
            filterResultView.visibility = View.VISIBLE
            val timer = object : CountDownTimer(2000, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    filterResultView.visibility = View.GONE
                }
            }.start()
        }
    }
}