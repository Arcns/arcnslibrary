package com.arcns.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.arcns.core.R
import com.arcns.core.map.MapLocator

/**
 * 定位器服务
 */
class MapLocatorService : Service() {

    // 定位器
    var mapLocator: MapLocator? = null

    // CPU唤醒标志
    private var foregroundServiceWakeLock: PowerManager.WakeLock? = null

    // 通讯
    private var foregroundServiceBinder = MapLocatorServiceBinder(this)
    override fun onBind(intent: Intent?): IBinder? = foregroundServiceBinder

    //
    override fun onCreate() {
        super.onCreate()
        // 通知栏配置
        getDisposableShareData<NotificationOptions>(KEY_NOTIFICATION_OPTIONS)?.run {
            startForeground(this)
        }
        // 定位器
        getDisposableShareData<CreateMapLocator>(KEY_CREATE_MAP_LOCATOR)?.run {
            mapLocator = this.invoke(this@MapLocatorService)
            mapLocator?.start()
        }
        // 保持CPU唤醒状态
        if (getDisposableShareData<Boolean>(KEY_IS_ACQUIRE_WAKELOCK) == true) {
            acquireWakeLock()
        }
    }


    /**
     * 保持CPU唤醒状态
     * 需要添加<uses-permission android:name="android.permission.WAKE_LOCK"/>权限
     */
    fun acquireWakeLock() {
        val foregroundServiceWakeLockTag = "app:foregroundServiceWakeLockTag"
        foregroundServiceWakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                foregroundServiceWakeLockTag
            )
        foregroundServiceWakeLock?.acquire()
    }

    /**
     * 释放CPU唤醒状态
     */
    fun releaseWakeLock() {
        try {
            foregroundServiceWakeLock?.release()
        } catch (e: Exception) {
        }
    }

    /**
     * 开始前台通知
     */
    private fun startForeground(options: NotificationOptions) {
        var mapLocatorServiceChannelId = "MapLocatorServiceChannelId"
        val mapLocatorServiceChannelName = "MapLocatorServiceChannelName"
        val mapLocatorServiceNotificationID = 99999999
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //在Android O之上需要发起通知时需要先创建渠道
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                (options.createNotificationChannel?.invoke() ?: NotificationChannel(
                    mapLocatorServiceChannelId,
                    mapLocatorServiceChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                )).apply {
                    mapLocatorServiceChannelId = id
                }
            )
        }
        startForeground(
            mapLocatorServiceNotificationID,
            NotificationCompat.Builder(this, mapLocatorServiceChannelId)
                .setContentTitle(R.string.text_map_locator_service_notification_content_title.string)
                .setContentText(R.string.text_map_locator_service_notification_content_text.string)
                .apply {
                    options.notificationSmallIcon?.run {
                        setSmallIcon(this)
                    }
                    options.setNotificationCompatBuilder?.invoke(this)
                }.build()
        )
    }

    // 销毁
    override fun onDestroy() {
        releaseWakeLock()
        mapLocator?.onDestroy()
        super.onDestroy()
    }


    companion object {
        const val KEY_IS_ACQUIRE_WAKELOCK = "KEY_IS_ACQUIRE_WAKELOCK"
        const val KEY_NOTIFICATION_OPTIONS = "KEY_NOTIFICATION_OPTIONS"
        const val KEY_CREATE_MAP_LOCATOR = "KEY_CREATE_MAP_LOCATOR"

        // 默认值
        private var defualtCreateMapLocator: CreateMapLocator? = null
        private var defaultNotificationOptions: NotificationOptions? = null
        private var defaultIsAcquireWakeLock: Boolean = false
        fun setDefaultOptions(
            createMapLocator: CreateMapLocator,
            notificationOptions: NotificationOptions? = null,
            isAcquireWakeLock: Boolean = false
        ) {
            defualtCreateMapLocator = createMapLocator
            defaultNotificationOptions = notificationOptions
            defaultIsAcquireWakeLock = isAcquireWakeLock
        }

        // 开始服务
        fun startService(
            context: Context?,
            serviceConnection: ServiceConnection? = null,
            createMapLocator: CreateMapLocator? = defualtCreateMapLocator,
            notificationOptions: NotificationOptions? = defaultNotificationOptions,
            isAcquireWakeLock: Boolean = defaultIsAcquireWakeLock
        ) {
            if (createMapLocator != null) {
                addShareData(KEY_CREATE_MAP_LOCATOR, createMapLocator)
            }
            if (notificationOptions != null) {
                addShareData(KEY_NOTIFICATION_OPTIONS, notificationOptions)
            }
            addShareData(KEY_IS_ACQUIRE_WAKELOCK, isAcquireWakeLock)
            // 开启服务
            var intent = Intent(context, MapLocatorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(intent)
            } else {
                context?.startService(intent)
            }
            serviceConnection?.run {
                context?.bindService(intent, this, BIND_AUTO_CREATE)
            }
        }
    }
}

/**
 * 新建定时器
 */
typealias CreateMapLocator = (Context) -> MapLocator

/**
 * 定位器服务通讯
 */
class MapLocatorServiceBinder(val service: MapLocatorService) : Binder() {
    val mapLocator: MapLocator? get() = service.mapLocator
}

abstract class MapLocatorServiceConnection : ServiceConnection {
    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        onServiceConnected(service as? MapLocatorServiceBinder ?: return)
    }

    abstract fun onServiceConnected(binder: MapLocatorServiceBinder)
}

/**
 * 通知配置
 */
data class NotificationOptions(
    val isEnable: Boolean = true,
    val notificationSmallIcon: Int? = null,
    val createNotificationChannel: (() -> NotificationChannel)? = null,
    val setNotificationCompatBuilder: ((NotificationCompat.Builder) -> Unit)? = null
)