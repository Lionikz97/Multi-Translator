package multi.translator.onscreenocr.floatings

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.floatings.manager.FloatingStateManager
import multi.translator.onscreenocr.pages.launch.LaunchActivity
import multi.translator.onscreenocr.pages.setting.SettingManager
import multi.translator.onscreenocr.remoteconfig.RemoteConfigManager
import multi.translator.onscreenocr.screenshot.ScreenExtractor
import multi.translator.onscreenocr.utils.Logger

class ViewHolderService : Service() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "floating_view_notification_channel_v1"
        private const val REQUEST_CODE = 1
        private const val ONGOING_NOTIFICATION_ID = 2021

        private const val ACTION_SHOW_VIEWS = "ACTION_SHOW_VIEWS"
        private const val ACTION_HIDE_VIEWS = "ACTION_HIDE_VIEWS"
        private const val ACTION_EXIT = "ACTION_EXIT"

        private val logger: Logger = Logger(ViewHolderService::class)

        fun showViews(context: Context) {
            startAction(context, ACTION_SHOW_VIEWS)
        }

        fun hideViews(context: Context) {
            startAction(context, ACTION_HIDE_VIEWS)
        }

        fun exit(context: Context) {
            startAction(context, ACTION_EXIT)
        }

        private fun startAction(context: Context, action: String) {
            context.startService(Intent(context, ViewHolderService::class.java).apply {
                this.action = action
            })
        }
    }

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var floatingStateListenerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        floatingStateListenerJob = CoroutineScope(Dispatchers.Main).launch {
            FloatingStateManager.showingStateChangedFlow.collect { startForeground() }
        }
        RemoteConfigManager.tryFetchNew()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        logger.debug("Received action: ${intent?.action}")

        return when (intent?.action) {
            ACTION_SHOW_VIEWS -> {
                showViews()
                START_STICKY
            }

            ACTION_HIDE_VIEWS -> {
                hideViews()
                START_STICKY
            }

            ACTION_EXIT -> {
                exit()
                stopForeground()
                START_NOT_STICKY
            }

            else -> {
                logger.debug("Got a unhandled action: ${intent?.action}")
                START_NOT_STICKY
            }
        }
    }

    private fun showViews() {
        if (ScreenExtractor.isGranted) {
            FloatingStateManager.showMainBar()
        } else {
            startActivity(LaunchActivity.getLaunchIntent(this))
        }
    }

    private fun hideViews() {
        FloatingStateManager.detachAllViews()
    }

    private fun exit() {
        hideViews()
        ScreenExtractor.release()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()

        floatingStateListenerJob?.cancel()
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ONGOING_NOTIFICATION_ID,
                createNotification(!FloatingStateManager.isMainBarAttached),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(
                ONGOING_NOTIFICATION_ID,
                createNotification(!FloatingStateManager.isMainBarAttached),
            )
        }
    }

    private fun stopForeground() {
        stopForeground(true)
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = notificationManager.notificationChannels

            channels
                .filter {
                    it.id != NOTIFICATION_CHANNEL_ID && it.id != NotificationChannel.DEFAULT_CHANNEL_ID
                }.forEach {
                    logger.debug("Remove unknown channel: $it")
                    notificationManager.deleteNotificationChannel(it.id)
                }

            if (!channels.any { it.id == NOTIFICATION_CHANNEL_ID }) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.channel_default),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                }

                logger.debug("Create notification channel: $channel")
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun createNotification(clickToShow: Boolean): Notification {
        initNotificationChannel()

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setColor(ContextCompat.getColor(this, R.color.appIconColor))
            .setSmallIcon(R.drawable.ic_for_notify)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_shadow))
            .setTicker(getString(R.string.app_name))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                if (clickToShow) getString(R.string.msg_click_to_show_the_floating_bar)
                else getString(R.string.msg_click_to_hide_the_floating_bar)
            )
            .setAutoCancel(false)

        //TODO use notification actions instead of click event
        val intent = Intent(this, ViewHolderService::class.java).apply {
            action = if (clickToShow) ACTION_SHOW_VIEWS else ACTION_HIDE_VIEWS
        }
        val pendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.getService(
                    this, REQUEST_CODE, intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            else
                PendingIntent.getService(
                    this, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT
                )
        builder.setContentIntent(pendingIntent)

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
