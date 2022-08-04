package multi.translator.onscreenocr.remoteconfig

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.github.firemaples.utils.JsonUtil
import io.github.firemaples.utils.TypeReference
import multi.translator.onscreenocr.BuildConfig
import multi.translator.onscreenocr.CoreApplication
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.pref.AppPref
import multi.translator.onscreenocr.remoteconfig.data.TrainedDataFileNames
import multi.translator.onscreenocr.remoteconfig.data.TrainedDataSite
import multi.translator.onscreenocr.utils.Logger

internal const val KEY_VERSION = "version"
internal const val KEY_FETCH_INTERVAL = "fetch_interval"
internal const val KEY_MICROSOFT_KEY = "microsoft_key"
internal const val KEY_MICROSOFT_KEY_GROUP_ID = "microsoft_key_group_id"
internal const val KEY_TRAINED_DATA_URL = "trained_data_url_data_v1"
internal const val KEY_TRAINED_DATA_FILES = "trained_data_files"
internal const val KEY_PRIVACY_POLICY_URL = "privacy_policy_url"
internal const val KEY_ABOUT_URL = "about_url"

object RemoteConfigManager {
    private val logger: Logger by lazy { Logger(RemoteConfigManager::class) }

    private val context: Context by lazy { CoreApplication.instance }

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        val fetchInterval = if (BuildConfig.DEBUG) 0 else AppPref.firebaseRemoteConfigFetchInterval
        val settings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(fetchInterval)
                .build()

        FirebaseRemoteConfig.getInstance().apply {
            setDefaultsAsync(R.xml.remote_config_defaults)
            setConfigSettingsAsync(settings)
        }
    }

    fun tryFetchNew() {
        logger.info("fetchTimeMillis: ${remoteConfig.info.fetchTimeMillis}")
        logger.info("lastFetchStatus: ${getLastFetchState()}")

        logger.info("Version before fetch: $versionString, keyGroup: $microsoftTranslationKeyGroupId")
        remoteConfig.fetchAndActivate().addOnSuccessListener {
            logger.info("Version after fetch: $versionString, keyGroup: $microsoftTranslationKeyGroupId")

            AppPref.firebaseRemoteConfigFetchInterval = remoteConfig.getLong(KEY_FETCH_INTERVAL)
        }.addOnFailureListener {
            logger.error("Remote config fetch failed", it)
        }
    }

    private fun getLastFetchState(): String =
        when (remoteConfig.info.lastFetchStatus) {
            FirebaseRemoteConfig.LAST_FETCH_STATUS_NO_FETCH_YET -> "NotFetchYet"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS -> "Success"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_FAILURE -> "Failure"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_THROTTLED -> "Throttled"
            else -> "Unknown"
        }

    private fun getString(key: String): String =
        remoteConfig.getString(key)

    private fun getStringsByPrefix(prefix: String): Set<String> =
        remoteConfig.getKeysByPrefix(prefix)

    private val versionString: String
        get() = getString(KEY_VERSION)

    val microsoftTranslationKeyGroupId: String
        get() = getString(KEY_MICROSOFT_KEY_GROUP_ID)

    val microsoftTranslationKey: String
        get() = context.getString(R.string.microsoftSubscriptionKey).let {
            if (it.isBlank()) getString(KEY_MICROSOFT_KEY) else it
        }

    val trainedDataSites: List<TrainedDataSite>
        get() = JsonUtil<List<TrainedDataSite>>()
            .parseJson(getString(KEY_TRAINED_DATA_URL),
                object : TypeReference<List<TrainedDataSite>>() {}) ?: listOf()

    fun trainedDataFileSubs(ocrLang: String): Array<String> {
        val fileSubNameString = getString(KEY_TRAINED_DATA_FILES)
        val fileSubNames = JsonUtil<TrainedDataFileNames>()
            .parseJson(fileSubNameString, object : TypeReference<TrainedDataFileNames>() {})

        val subNames = mutableListOf<String>()
        subNames.addAll(fileSubNames.default)
        fileSubNames.others[ocrLang]?.also {
            subNames.addAll(it)
        }

        return subNames.toTypedArray()
    }

    val privacyPolicyUrl: String
        get() = getString(KEY_PRIVACY_POLICY_URL)

    val aboutUrl: String
        get() = getString(KEY_ABOUT_URL)
}
