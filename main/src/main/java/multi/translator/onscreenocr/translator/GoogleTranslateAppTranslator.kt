package multi.translator.onscreenocr.translator

import kotlinx.coroutines.CoroutineScope
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.utils.GoogleTranslateUtils
import multi.translator.onscreenocr.utils.Logger

object GoogleTranslateAppTranslator : Translator {
    private val logger: Logger by lazy { Logger(GoogleTranslateAppTranslator::class) }

    override val type: TranslationProviderType
        get() = TranslationProviderType.GoogleTranslateApp

    override val translationHint: String
        get() = context.getString(R.string.msg_select_lang_in_google_translate)

    override suspend fun checkEnvironment(
        coroutineScope: CoroutineScope
    ): Boolean = GoogleTranslateUtils.checkGoogleTranslateInstalled()

    override suspend fun translate(text: String, sourceLangCode: String): TranslationResult {
        GoogleTranslateUtils.launchGoogleTranslateApp(text)

        return TranslationResult.OuterTranslatorLaunched
    }
}
