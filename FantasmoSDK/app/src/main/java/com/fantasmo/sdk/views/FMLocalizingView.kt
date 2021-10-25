package com.fantasmo.sdk.views

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.fantasmo.sdk.FMBehaviorRequest
import android.os.CountDownTimer
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fantasmo.sdk.fantasmosdk.R

class FMLocalizingView(arLayout: CoordinatorLayout) {

    private var fmLocalizingView: ConstraintLayout = arLayout.findViewById(R.id.fmLocalizeView)
    private var filterResultView: TextView = arLayout.findViewById(R.id.filterRejectionTextView)

    fun hide() {
        if (fmLocalizingView.visibility == View.VISIBLE) {
            fmLocalizingView.visibility = View.GONE
        }
    }

    fun display() {
        if (fmLocalizingView.visibility == View.GONE) {
            fmLocalizingView.visibility = View.VISIBLE
        }
    }

    fun displayFilterResult(behavior: FMBehaviorRequest) {
        filterResultView.text = behavior.displayName
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