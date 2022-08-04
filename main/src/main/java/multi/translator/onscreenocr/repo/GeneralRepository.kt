package multi.translator.onscreenocr.repo

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.floatings.readme.ReadmeView
import multi.translator.onscreenocr.pages.setting.SettingManager
import multi.translator.onscreenocr.pref.AppPref
import multi.translator.onscreenocr.utils.Logger
import multi.translator.onscreenocr.utils.Utils
import java.util.*

class GeneralRepository {
    companion object {
        private const val FORMAT_VERSION_MESSAGE_KEY = "version_%s"
    }

    private val logger: Logger by lazy { Logger(GeneralRepository::class) }
    private val context: Context by lazy { Utils.context }

    fun isRememberLastSelection(): Flow<Boolean> = flow {
        emit(SettingManager.saveLastSelectionArea)
    }.flowOn(Dispatchers.Default)

    fun getLastRememberedSelectionArea(): Flow<Rect?> = flow {
        emit(AppPref.lastSelectionArea)
    }.flowOn(Dispatchers.Default)

    suspend fun setLastRememberedSelectionArea(rect: Rect) {
        withContext(Dispatchers.Default) {
            AppPref.lastSelectionArea = rect
        }
    }

    fun isReadmeAlreadyShown(): Flow<Boolean> = flow {
        val lastVersionName = ReadmeView.VERSION
        val lastShownName = AppPref.lastReadmeShownVersion

        if (lastVersionName != lastShownName) {
            AppPref.lastReadmeShownVersion = lastVersionName
            emit(false)
        } else {
            emit(true)
        }
    }

    fun isVersionHistoryAlreadyShown(): Flow<Boolean> = flow {
        val lastVersionName = context.resources.getStringArray(R.array.versionCodes).firstOrNull()
        val lastShownName = AppPref.lastVersionHistoryShownVersion

        if (lastVersionName != null && lastVersionName != lastShownName) {
            AppPref.lastVersionHistoryShownVersion = lastVersionName
            emit(false)
        } else {
            emit(true)
        }
    }

    fun getVersionHistory(): Flow<List<Record>> = flow {
        val packageName = context.packageName
        val versionCodes = context.resources.getStringArray(R.array.versionCodes)

        val result = versionCodes.mapNotNull { versionCode ->
            val resName = String.format(
                Locale.US,
                FORMAT_VERSION_MESSAGE_KEY, versionCode.replace(".", "_")
            )
            try {
                val resId = context.resources.getIdentifier(resName, "string", packageName)
                val desc = context.getString(resId)

                Record(version = versionCode, desc = desc)
            } catch (e: Exception) {
                logger.debug(t = e)
                null
            }
        }.toList()

        emit(result)
    }.flowOn(Dispatchers.Default)

    suspend fun saveLastMainBarPosition(x: Int, y: Int) {
        AppPref.lastMainBarPosition = Point(x, y)
    }

    fun isAutoCopyOCRResult(): Flow<Boolean> = flow {
        emit(SettingManager.autoCopyOCRResult)
    }.flowOn(Dispatchers.Default)

    fun hideRecognizedTextAfterTranslated(): Flow<Boolean> = flow {
        emit(SettingManager.hideRecognizedResultAfterTranslated)
    }.flowOn(Dispatchers.Default)

    data class Record(val version: String, val desc: String)
}
