package Elements

import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matcher
import android.R

enum class LocalizeElementIds(val id: Int) {
    QRCODE(R.id.scanQRCodeSwitch),
    DEBUGSTATS(R.id.showStatisticsSwitch),
    SIMULATIONMODE(R.id.simulationModeSwitch),
    LOCALIZEBUTTON(R.id.endRideButton),
    LOCALIZERESULTS(5)
}

object LocalizeElements {
    fun qrCodeToggle(): Matcher<View> {
        return withId(LocalizeElementIds.QRCODE.id)
    }

    fun debugStatsToggle(): Matcher<View> {
        return withId(LocalizeElementIds.DEBUGSTATS.id)
    }
    fun simulationModToggle(): Matcher<View> {
        return withId(LocalizeElementIds.SIMULATIONMODE.id)
    }

    fun localizeButton() : Matcher<View> {
        return withId(LocalizeElementIds.LOCALIZEBUTTON.id)
    }

    fun localizeResults() : Matcher<View> {
        return withId(LocalizeElementIds.LOCALIZERESULTS.id)
    }
}