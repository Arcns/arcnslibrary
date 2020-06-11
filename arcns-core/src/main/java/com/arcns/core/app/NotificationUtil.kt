package com.arcns.core.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arcns.core.R
import com.arcns.core.util.string
import java.util.*


/**
 * 通知配置
 */
data class NotificationOptions(
    var isEnable: Boolean = true,
    var channelId: String = UUID.randomUUID().toString(),
    var channelName: String = R.string.text_notification_default_channel_name.string,
    var notificationID: Int = ((Int.MAX_VALUE / 2)..Int.MAX_VALUE).random(),
    var contentTitle: String = R.string.text_notification_default_content_title.string,
    var contentText: String = R.string.text_notification_default_content_text.string,
    var smallIcon: Int? = null,
    // 创建自定义NotificationChannel代替默认
    var onCreateNotificationChannel: (() -> NotificationChannel)? = null,
    // 设置NotificationCompatBuilder
    var onSettingNotificationCompatBuilder: ((NotificationCompat.Builder) -> Unit)? = null
)

/**
 * 创建通知
 */
fun Context.createNotification(options: NotificationOptions): Notification? {
    if (!options.isEnable) return null
    var channelId = options.channelId
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //在Android O之上需要发起通知时需要先创建渠道
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            (options.onCreateNotificationChannel?.invoke() ?: NotificationChannel(
                options.channelId,
                options.channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )).apply {
                channelId = id
            }
        )
    }
    return NotificationCompat.Builder(this, channelId)
        .setContentTitle(options.contentTitle)
        .setContentText(options.contentText)
        .apply {
            options.smallIcon?.run {
                setSmallIcon(this)
            }
            setContentIntent(
                PendingIntent.getBroadcast(
                    this@createNotification,
                    0,
                    WakeAppReceiver.newIntent(this@createNotification),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            options.onSettingNotificationCompatBuilder?.invoke(this)
        }.build()
}