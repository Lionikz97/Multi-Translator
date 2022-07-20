package multi.translator.onscreenocr

import android.app.Application
import multi.translator.onscreenocr.log.FirebaseEvent
import multi.translator.onscreenocr.log.UserInfoUtils
import multi.translator.onscreenocr.remoteconfig.RemoteConfigManager

class CoreApplication : Application() {
    companion object {
        lateinit var instance: Application
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        FirebaseEvent.validateSignature()
        UserInfoUtils.setClientInfo()
        RemoteConfigManager.tryFetchNew()

    }
}
