package com.arcns.core.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arcns.core.APP
import com.arcns.core.R
import com.arcns.core.util.bitmap
import com.arcns.core.util.string
import java.util.*


/**
 * 通知配置
 */
open class NotificationOptions(
    var channelId: String = UUID.randomUUID().toString(),
    var channelName: String,
    var notificationID: Int = ((Int.MAX_VALUE / 2)..Int.MAX_VALUE).random(),
    var contentTitle: String,
    var contentText: String,
    var contentIntent: PendingIntent? = null,
    var smallIcon: Int,
    var largeIcon: Bitmap? = null,
    var defaults: Int? = null, // 默认通知选项
    var priority: Int? = null, // 通知优先级
    var progress: NotificationProgressOptions? = null,//进度
    var isOngoing: Boolean? = null,// 是否禁用滑动删除
    // 创建自定义NotificationChannel代替默认
    var onCreateNotificationChannel: (() -> NotificationChannel)? = null,
    // 设置NotificationCompatBuilder
    var onSettingNotificationCompatBuilder: ((NotificationCompat.Builder) -> Unit)? = null
) {

    var isEnable: Boolean = true
        private set

    companion object {
        val DISABLE: NotificationOptions
            get() = NotificationOptions(
                channelName = "",
                contentTitle = "",
                contentText = "",
                smallIcon = -1
            ).apply { isEnable = false }
    }
}

/**
 * 通知进度配置
 */
data class NotificationProgressOptions(
    var max: Int,
    var progress: Int = 0,
    var indeterminate: Boolean = false
)

/**
 * 通知管理器
 */
val notificationManager: NotificationManager get() = APP.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

/**
 * 创建通知
 */
fun NotificationOptions.createBuilder(): NotificationCompat.Builder? {
    if (!isEnable) return null
    var notificationChannelId = channelId
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //在Android O之上需要发起通知时需要先创建渠道
        notificationManager.createNotificationChannel(
            (onCreateNotificationChannel?.invoke() ?: NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )).apply {
                notificationChannelId = id
            }
        )
    }
    return NotificationCompat.Builder(APP.INSTANCE, notificationChannelId)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .apply {
            smallIcon?.run {
                setSmallIcon(this)
            }
            largeIcon?.run {
                setLargeIcon(this)
            }
            defaults?.run {
                setDefaults(this)
            }
            this@createBuilder.priority?.run {
                setPriority(this)
            }
            progress?.run {
                setProgress(max, progress, indeterminate)
            }
            isOngoing?.run {
                setOngoing(this)
            }
            setContentIntent(
                contentIntent ?: PendingIntent.getBroadcast(
                    APP.INSTANCE,
                    0,
                    WakeAppReceiver.newIntent(APP.INSTANCE), // 创建唤醒广播意图
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            onSettingNotificationCompatBuilder?.invoke(this)
        }
}

/**
 * 创建通知
 */
fun NotificationOptions.create(): Notification? = createBuilder()?.build()

/**
 * 显示通知
 */
fun NotificationOptions.show(): Notification? = create()?.apply {
    notificationManager.notify(notificationID, this)
}

/**
 * 更新NotificationCompat.Builder，注意与create相比，update不会配置channelId和初始化空contentIntent
 */
fun NotificationCompat.Builder.update(options: NotificationOptions): NotificationCompat.Builder {
    setContentTitle(options.contentTitle)
    setContentText(options.contentText)
    options.smallIcon?.run {
        setSmallIcon(this)
    }
    options.largeIcon?.run {
        setLargeIcon(this)
    }
    options.defaults?.run {
        setDefaults(this)
    }
    options.priority?.run {
        setPriority(this)
    }
    options.progress?.run {
        setProgress(max, progress, indeterminate)
    }
    options.isOngoing?.run {
        setOngoing(this)
    }
    options.contentIntent?.run {
        setContentIntent(this)
    }
    options.onSettingNotificationCompatBuilder?.invoke(this)
    return this
}

/**
 * 更新并显示
 */
fun NotificationCompat.Builder.updateAndShow(options: NotificationOptions) =
    update(options).show(options.notificationID)

/**
 * 显示
 */
fun NotificationCompat.Builder.show(notificationID: Int) =
    notificationManager.notify(notificationID, build())


/**
 * 取消显示通知
 */
fun NotificationOptions.cancel() = notificationManager.cancel(notificationID)
fun Int.cancelNotification() = notificationManager.cancel(this)