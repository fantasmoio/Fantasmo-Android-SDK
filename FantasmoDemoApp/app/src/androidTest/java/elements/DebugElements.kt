package elements

import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matcher

// TODO: cannot import R from another app (the SDK) so we need a different matching mechanism here
// may need to add unique test IDs to each SDK debug text field, see:
// https://testyour.app/blog/blog-matching-view-with-text/
// for some text based ViewMatchers approach

//enum class DebugTextField(val text: String) {
//    SDKVERSION(R.id.sdkVersion),
//    LOCALIZATIONSTATUS(R.id.statusText),
//    FRAMESCURRENTWINDOW(R.id.currentWindowText),
//    FRAMESEVALUATED(R.id.framesEvaluatedText),
//    FRAMESREJECTED(R.id.framesRejectedText),
//    BESTSCORE(R.id.bestScoreText),
//    LIVESCORE(R.id.liveScoreText),
//    LASTERROR(R.id.lastErrorText),
//    MODELVERSION(R.id.modelVersionText),
//    LASTRESULT(R.id.lastResultText),
//    ERRORCOUNT(R.id.errorCountText),
//    DEVICELOCATION(R.id.deviceLocationText),
//    TRANSLATION(R.id.translationText),
//    TOTALTRANSLATION(R.id.totalTranslationText),
//    REMOTECONFIGID(R.id.remoteConfigText),
//    EULERANGLES(R.id.eulerAnglesText),
//    EULERANGLESPREADS(R.id.eulerAngleSpreadText),
//    TOTALFRAMEREJECTIONS(R.id.totalFrameRejectionsText)
//}
//
//object DebugElements {
//    fun textField(field: DebugTextField): Matcher<View> {
//        return withId(field.id)
//    }
//}
