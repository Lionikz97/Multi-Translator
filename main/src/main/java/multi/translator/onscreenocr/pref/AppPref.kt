package multi.translator.onscreenocr.pref

import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.gsonpref.gson
import com.chibatching.kotpref.gsonpref.gsonNullablePref
import com.chibatching.kotpref.gsonpref.gsonPref
import com.google.gson.Gson
import multi.translator.onscreenocr.recognition.TextRecognitionProviderType
import multi.translator.onscreenocr.utils.Constants

object AppPref : KotprefModel() {
    init {
        Kotpref.gson = Gson()
    }

    var selectedOCRProviderKey by stringPref(
        default = Constants.DEFAULT_OCR_PROVIDER.key
    )
    var selectedOCRProvider: TextRecognitionProviderType
        get() = TextRecognitionProviderType.values()
            .firstOrNull { it.key == selectedOCRProviderKey } ?: Constants.DEFAULT_OCR_PROVIDER
        set(value) {
            selectedOCRProviderKey = value.key
        }
    var selectedOCRLang by stringPref(default = Constants.DEFAULT_OCR_LANG)

    var selectedTranslationProvider by stringPref(
        default = Constants.DEFAULT_TRANSLATION_PROVIDER.key
    )
    var selectedTranslationLang by stringPref(default = Constants.DEFAULT_TRANSLATION_LANG)

    var lastSelectionArea: Rect? by gsonNullablePref()

    var lastVersionHistoryShownVersion: String? by nullableStringPref(default = null)
    var lastReadmeShownVersion: String? by nullableStringPref(default = null)

    var firebaseRemoteConfigFetchInterval: Long by longPref(default = 43200)

    var lastMainBarPosition: Point by gsonPref(default = Point(0, 0))

    var imageReaderFormat: Int by intPref(default = PixelFormat.RGBA_8888)
}
