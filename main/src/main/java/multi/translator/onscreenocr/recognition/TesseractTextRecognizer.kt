package multi.translator.onscreenocr.recognition

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.utils.Logger
import multi.translator.onscreenocr.utils.Utils
import java.io.File

class TesseractTextRecognizer : TextRecognizer {
    companion object {
        private const val TRAINED_DATA_FILE_NAME_SUFFIX = ".traineddata"

        private val logger: Logger by lazy { Logger(TesseractTextRecognizer::class) }
        private val context: Context by lazy { Utils.context }

        private val tessBaseDir: File
            get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "tesseract")
        private val tessDataDir: File
            get() = File(tessBaseDir, "tessdata")

        private fun initTessDataFolder(): Boolean =
            (tessDataDir.isDirectory || tessDataDir.mkdirs()).also {
                if (!it) logger.warn("Initializing tesseract data folder failed")
            }

        fun getTessDataFile(langCode: String): File =
            File(tessDataDir, "$langCode$TRAINED_DATA_FILE_NAME_SUFFIX")

        private fun getDownloadedLangCodes(): Set<String> {
            if (!initTessDataFolder()) {
                return emptySet()
            }

            return tessDataDir.list { _, name -> name.endsWith(TRAINED_DATA_FILE_NAME_SUFFIX) }
                ?.map { it.substring(0, it.length - TRAINED_DATA_FILE_NAME_SUFFIX.length) }
                ?.toHashSet() ?: emptySet()
        }

        fun getSupportedLanguageList(context: Context): List<RecognitionLanguage> {
            val res = context.resources
            val innerCodes = res.getStringArray(R.array.lang_ocr_tesseract_code_iso6393)
            val displayCodes = res.getStringArray(R.array.lang_ocr_tesseract_display_code_iso6391)
            val names = res.getStringArray(R.array.lang_ocr_tesseract_name)
            val downloadedCodes = getDownloadedLangCodes()

            return innerCodes.indices
                .mapNotNull { i ->
                    val displayCode = displayCodes[i]
                    val name = names[i]
                    val innerCode = innerCodes[i]

                    RecognitionLanguage(
                        code = displayCode,
                        displayName = name,
                        selected = false, //displayCode == selected,
                        downloaded = downloadedCodes.contains(innerCode),
                        recognizer = TextRecognitionProviderType.Tesseract,
                        innerCode = innerCode,
                    )
                }
                .sortedBy { it.displayName }
        }
    }

    override val type: TextRecognitionProviderType
        get() = TextRecognitionProviderType.Tesseract
    override val name: String
        get() = type.name

    override suspend fun recognize(lang: RecognitionLanguage, bitmap: Bitmap): RecognitionResult {
        val tess = TessBaseAPI()
        tess.init(tessBaseDir.absolutePath, lang.innerCode)
        tess.setImage(bitmap)
        val resultText = tess.utF8Text
        val boxes = tess.regions.boxRects

        return RecognitionResult(lang.code, resultText, boxes)
    }

    override suspend fun parseToDisplayLangCode(langCode: String): String {
        return langCode
    }
}
