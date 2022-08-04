package multi.translator.onscreenocr.translator

import android.content.Context
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.pref.AppPref
import multi.translator.onscreenocr.utils.Constants
import multi.translator.onscreenocr.utils.Utils
import multi.translator.onscreenocr.utils.firstPart

interface Translator {
    companion object {
        suspend fun getTranslator(
            type: TranslationProviderType = TranslationProviderType.fromKey(
                AppPref.selectedTranslationProvider
            )
        ): Translator =
            when (type) {

                TranslationProviderType.GoogleMLKit -> GoogleMLKitTranslator
                TranslationProviderType.GoogleTranslateApp -> GoogleTranslateAppTranslator
                TranslationProviderType.OCROnly -> OCROnlyTranslator
            }
    }

    val type: TranslationProviderType
    val context: Context
        get() = Utils.context
    val translationHint: String?
        get() = null

    suspend fun checkEnvironment(coroutineScope: CoroutineScope): Boolean = true

    suspend fun isLangSupport(): Boolean =
        supportedLanguages().any { it.code.firstPart() == AppPref.selectedOCRLang.firstPart() }

    suspend fun supportedLanguages(): List<TranslationLanguage> = emptyList()
    suspend fun translate(text: String, sourceLangCode: String): TranslationResult
    suspend fun selectedLangCode(supportedLangList: Array<String>): String {
        val selectedLangCode = AppPref.selectedTranslationLang

        return if (supportedLangList.any { it == selectedLangCode }) selectedLangCode
        else {
            AppPref.selectedTranslationLang = Constants.DEFAULT_TRANSLATION_LANG
            Constants.DEFAULT_TRANSLATION_LANG
        }
    }
}

enum class TranslationProviderType(
    val index: Int,
    val key: String,
    @StringRes val nameRes: Int,
    val nonTranslation: Boolean = false
) {
    //MicrosoftAzure(0, "microsoft_azure", R.string.translation_provider_microsoft_azure),
    GoogleMLKit(1, "google_ml_kit", R.string.translation_provider_google_ml_kit),
    GoogleTranslateApp(
        2, "google_translate_app", R.string.translation_provider_google_translate_app,
        nonTranslation = true
    ),
    OCROnly(3, "ocr_only", R.string.translation_provider_none, nonTranslation = true);

    companion object {
        fun fromKey(key: String): TranslationProviderType =
            values().firstOrNull { it.key == key } ?: Constants.DEFAULT_TRANSLATION_PROVIDER
    }
}

data class TranslationProvider(
    val key: String,
    val displayName: String,
    val nonTranslation: Boolean,
    val type: TranslationProviderType,
    val selected: Boolean,
) {
    companion object {
        fun fromType(
            context: Context, type: TranslationProviderType, selected: Boolean = false
        ): TranslationProvider =
            TranslationProvider(
                key = type.key,
                displayName = context.getString(type.nameRes),
                nonTranslation = type.nonTranslation,
                type = type,
                selected = selected,
            )
    }
}

data class TranslationLanguage(
    val code: String, /*val langCode: String,*/
    val displayName: String,
    val selected: Boolean
)

sealed class TranslationResult {
    data class TranslatedResult(val result: String, val type: TranslationProviderType) :
        TranslationResult()

    data class TranslationFailed(val error: Throwable) : TranslationResult()

    data class SourceLangNotSupport(val type: TranslationProviderType) : TranslationResult()
    object OCROnlyResult : TranslationResult()
    object OuterTranslatorLaunched : TranslationResult()
}
