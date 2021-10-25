package com.fantasmo.sdk.views

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.fantasmo.sdk.FMBehaviorRequest
import android.os.CountDownTimer

class FMLocalizingView(var fmLocalizingView: ConstraintLayout, var filterResultView: TextView) {

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