import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matcher

enum class LocalizeElementIds(val id: Int) {
    QRCODE(1),
    DEBUGSTATS(2),
    SIMULATIONMODE(3),
    LOCALIZEBUTTON(4),
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