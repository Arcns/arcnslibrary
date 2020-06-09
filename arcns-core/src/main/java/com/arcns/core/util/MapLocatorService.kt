package com.arcns.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.arcns.core.map.MapLocator

class MapLocatorService : Service() {
    var mapLocator: MapLocator? = null

    // CPU唤醒标志
    private val foregroundServiceWakeLockTag = "app:foregroundServiceWakeLockTag"
    private var foregroundServiceWakeLock: PowerManager.WakeLock? = null

    // 通讯
    private var foregroundServiceBinder = MapLocatorServiceBinder(this)
    override fun onBind(intent: Intent?): IBinder? = foregroundServiceBinder

    //
    override fun onCreate() {
        super.onCreate()
        // 通知栏配置
        getDisposableShareData<NotificationOptions>()?.run {
            startForeground(this)
        }
    }


    /**
     * 保持CPU唤醒状态
     * 需要添加<uses-permission android:name="android.permission.WAKE_LOCK"/>权限
     */
    fun acquireWakeLock() {
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
     * 初始化通知
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
                .apply {
                    options.setNotificationCompatBuilder?.invoke(this)
                }.build()
        )
    }

    override fun onDestroy() {
        releaseWakeLock()
        mapLocator?.onDestroy()
        super.onDestroy()
    }


    companion object {
        fun startService(
            context: Context?,
            serviceConnection: ServiceConnection? = null,
            notificationOptions: NotificationOptions? = null
        ) {
            if (notificationOptions != null) {
                addShareData(notificationOptions)
            }
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

class MapLocatorServiceBinder(var service: MapLocatorService) : Binder() {
    val mapLocator: MapLocator? get() = service.mapLocator
}

data class NotificationOptions(
    val isEnable: Boolean = true,
    val createNotificationChannel: (() -> NotificationChannel)? = null,
    val setNotificationCompatBuilder: ((binder: MapLocatorService.MapLocatorServiceBinder) -> Unit)? = null
)