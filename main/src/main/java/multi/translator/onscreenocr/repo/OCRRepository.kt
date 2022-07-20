package multi.translator.onscreenocr.repo

import android.content.Context
import androidx.lifecycle.asFlow
import com.chibatching.kotpref.livedata.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import multi.translator.onscreenocr.api.ApiHub
import multi.translator.onscreenocr.pref.AppPref
import multi.translator.onscreenocr.recognition.RecognitionLanguage
import multi.translator.onscreenocr.recognition.TesseractTextRecognizer
import multi.translator.onscreenocr.recognition.TextRecognitionProviderType
import multi.translator.onscreenocr.recognition.TextRecognizer
import multi.translator.onscreenocr.utils.Logger
import multi.translator.onscreenocr.utils.Utils
import java.io.File
import java.io.IOException

class OCRRepository {
    private val logger: Logger by lazy { Logger(OCRRepository::class) }
    private val context: Context by lazy { Utils.context }

    private var downloadingTessDataCall: Call<ResponseBody>? = null

    val selectedOCRLangFlow: Flow<String>
        get() = AppPref.asLiveData(AppPref::selectedOCRLang).asFlow()
            .flowOn(Dispatchers.Default)

    fun getAllOCRLanguages(): Flow<List<RecognitionLanguage>> = flow {
        val supportedLangList = TextRecognizer.allSupportedLanguages(
            AppPref.selectedOCRLang,
            AppPref.selectedOCRProvider
        )

        emit(supportedLangList)
    }.flowOn(Dispatchers.Default)

    suspend fun setSelectedOCRLanguage(
        langCode: String,
        ocrProviderType: TextRecognitionProviderType,
    ) {
        withContext(Dispatchers.Default) {
            AppPref.selectedOCRLang = langCode
            AppPref.selectedOCRProvider = ocrProviderType
        }
    }

    @Throws(Exception::class)
    suspend fun downloadTessData(
        langCode: String,
        destFile: File = TesseractTextRecognizer.getTessDataFile(langCode),
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val call = ApiHub.tessDataDownloader.downloadFromGithub(langCode)
                downloadingTessDataCall = call
                val response = call.execute()
                if (response.isSuccessful) {
                    return@withContext response.body()?.saveToFile(destFile) == true
                } else {
                    val status = response.code()
                    val error = response.errorBody()?.string()
                    val msg = "Download Tesseract data failed, status: $status, error: $error"
                    logger.warn(msg)

                    throw Exception(msg)
                }
            } catch (e: Exception) {
                logger.warn("Download Tesseract data failed", e)

                throw e
            }
        }

    fun cancelDownloadingTessData() {
        downloadingTessDataCall?.cancel()
    }

    @Throws(IOException::class)
    suspend fun ResponseBody.saveToFile(destFile: File): Boolean =
        withContext(Dispatchers.IO) {
            val temp = ApiHub.tessDataTempFile
            if (temp.exists() && !temp.delete()) {
                logger.error("Deleting temporary tesseract data failed, path: $temp")
                return@withContext false
            }

            if (destFile.exists() && !destFile.delete()) {
                logger.error("Deleting dest tesseract data failed, path: $destFile")
                return@withContext false
            }

            byteStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext true
        }
}
