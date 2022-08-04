package multi.translator.onscreenocr.floatings.translationSelectPanel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.floatings.base.FloatingViewModel
import multi.translator.onscreenocr.floatings.dialog.DialogView
import multi.translator.onscreenocr.floatings.dialog.showDialog
import multi.translator.onscreenocr.floatings.dialog.showErrorDialog
import multi.translator.onscreenocr.recognition.RecognitionLanguage
import multi.translator.onscreenocr.recognition.TextRecognitionProviderType
import multi.translator.onscreenocr.recognition.TextRecognizer
import multi.translator.onscreenocr.repo.OCRRepository
import multi.translator.onscreenocr.repo.TranslationRepository
import multi.translator.onscreenocr.translator.TranslationProvider
import multi.translator.onscreenocr.utils.Logger
import multi.translator.onscreenocr.utils.Utils
import multi.translator.onscreenocr.utils.firstPart

class TranslationSelectPanelViewModel(viewScope: CoroutineScope) :
    FloatingViewModel(viewScope) {

    private val _ocrLanguageList = MutableLiveData<List<OCRLangItem>>()
    val ocrLanguageList: LiveData<List<OCRLangItem>> = _ocrLanguageList

    private val _selectedTranslationProviderName = MutableLiveData<String>()
    val selectedTranslationProviderName: LiveData<String> = _selectedTranslationProviderName

    private val _displayTranslationProviders = MutableLiveData<List<TranslationProvider>>()
    val displayTranslateProviders: LiveData<List<TranslationProvider>> =
        _displayTranslationProviders

    private val _translationLangList = MutableLiveData<List<TranslateLangItem>>()
    val translationLangList: LiveData<List<TranslateLangItem>> = _translationLangList

    private val _displayTranslationHint = MutableLiveData<String?>()
    val displayTranslationHint: LiveData<String?> = _displayTranslationHint

    private val logger: Logger by lazy { Logger(TranslationSelectPanelViewModel::class) }
    private val context: Context by lazy { Utils.context }

    private val ocrRepo = OCRRepository()
    private val translationRepo = TranslationRepository()

    fun load() {
        viewScope.launch {
            val ocrLanguages = ocrRepo.getAllOCRLanguages().first()
            _ocrLanguageList.value = ocrLanguages
                .map {
                    OCRLangItem(
                        code = it.code,
                        displayName = it.displayName,
                        selected = it.selected,
                        showDownloadIcon = !it.downloaded,
                        unrecommended = it.unrecommended,
                        ocrLang = it,
                    )
                }

            val selectedTranslationProvider =
                translationRepo.getSelectedProvider().first()
            _selectedTranslationProviderName.value = selectedTranslationProvider.displayName

            loadTranslationLanguageList(selectedTranslationProvider.key)
        }
    }

    private suspend fun loadTranslationLanguageList(providerKey: String) {
        val translationLanguages =
            translationRepo.getTranslationLanguageList(providerKey).first()

        if (translationLanguages.isEmpty()) {
            _displayTranslationHint.value = translationRepo.getTranslationHint(providerKey).first()
            _translationLangList.value = emptyList()
        } else {
            val selectedOCRLang = ocrRepo.selectedOCRLangFlow.first()

            if (translationLanguages.any {
                    it.code.firstPart() == selectedOCRLang.firstPart()
                }) {
                _displayTranslationHint.value = null
                _translationLangList.value = translationLanguages
                    .map { TranslateLangItem(it.code, it.displayName, it.selected) }
            } else {
                _displayTranslationHint.value =
                    context.getString(R.string.msg_translator_provider_does_not_support_the_ocr_lang)
                _translationLangList.value = emptyList()
            }
        }
    }

    fun onOCRLangSelected(langItem: OCRLangItem) {
        viewScope.launch {
            if (langItem.showDownloadIcon) {
                if (!downloadOCRModel(langItem)) {
                    return@launch
                }

                logger.debug("Download lang item success: $langItem")
            }

            ocrRepo.setSelectedOCRLanguage(langItem.code, langItem.recognizer)
            val ocrLangList = _ocrLanguageList.value ?: return@launch
            _ocrLanguageList.value = ocrLangList.map {
                when {
                    it.code == langItem.code && it.recognizer == langItem.recognizer ->
                        it.copy(selected = true, showDownloadIcon = false)
                    it.selected ->
                        it.copy(selected = false)
                    else -> it
                }
            }

            loadTranslationLanguageList(translationRepo.selectedProviderTypeFlow.first().key)
        }
    }

    private suspend fun downloadOCRModel(langItem: OCRLangItem): Boolean {
        val ocrLang = langItem.ocrLang
        val result = context.showDialog(
            title = "Download OCR model",
            message = "Do you want to download ${langItem.displayName} language model for OCR?",
            dialogType = DialogView.DialogType.CONFIRM_CANCEL,
            cancelByClickingOutside = true,
        )

        if (result) {
            when (val recognizer = ocrLang.recognizer) {
                TextRecognitionProviderType.Tesseract -> {
                    var cancelled = false
                    val dialogJob = viewScope.launch {
                        val downloadDialogResult = context.showDialog(
                            title = "OCR model downloading",
                            message = "Downloading OCR model: ${ocrLang.innerCode}[${langItem.displayName}]",
                            dialogType = DialogView.DialogType.CANCEL_ONLY,
                            cancelByClickingOutside = false,
                        )
                        if (!downloadDialogResult) {
                            cancelled = true
                            ocrRepo.cancelDownloadingTessData()
                        }
                    }
                    try {
                        if (ocrRepo.downloadTessData(ocrLang.innerCode)) {
                            dialogJob.cancel()
                            TextRecognizer.invalidSupportLanguages()
                            return true
                        } else {
                            if (!cancelled) {
                                context.showErrorDialog("Download OCR model failed with unknown error")
                            }
                        }
                    } catch (e: Exception) {
                        if (!cancelled) {
                            context.showErrorDialog("Download OCR model failed: ${e.message ?: e.localizedMessage}")
                        }
                    }
                }
                else -> {
                    logger.warn("The recognizer [$recognizer] does not implement the model downloader")
                }
            }
        }

        return false
    }

    fun onTranslationProviderClicked() {
        viewScope.launch {
            _displayTranslationProviders.value = translationRepo.getAllProviders().first()
        }
    }

    fun onTranslationProviderSelected(key: String) {
        viewScope.launch {
            val selectedProvider = translationRepo.setSelectedProvider(key).first()
            _selectedTranslationProviderName.value = selectedProvider.displayName
            loadTranslationLanguageList(selectedProvider.key)
        }
    }

    fun onTranslationLangChecked(langCode: String) {
        viewScope.launch {
            translationRepo.setSelectedTranslationLang(langCode)
            val translationLangList = _translationLangList.value ?: return@launch
            _translationLangList.value = translationLangList.map {
                it.copy(selected = it.code == langCode)
            }
        }
    }
}

sealed class LangItem(
    open val code: String,
    open val displayName: String,
    open val selected: Boolean,
    open val showDownloadIcon: Boolean = false,
    open val unrecommended: Boolean = false,
)

data class OCRLangItem(
    override val code: String,
    override val displayName: String,
    override val selected: Boolean,
    override val showDownloadIcon: Boolean = false,
    override val unrecommended: Boolean = false,
    val ocrLang: RecognitionLanguage,
) : LangItem(code, displayName, selected, showDownloadIcon, unrecommended) {
    val recognizer: TextRecognitionProviderType get() = ocrLang.recognizer
}

data class TranslateLangItem(
    override val code: String,
    override val displayName: String,
    override val selected: Boolean,
) : LangItem(code, displayName, selected)
