package com.arcns.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.arcns.core.R
import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.NotificationProgressOptions
import com.arcns.core.app.randomNotificationID
import com.arcns.core.util.string
import okhttp3.OkHttpClient
import java.util.*


/**
 * 上传任务类
 */
data class DownLoadTask(
    var url: String? = null,
    var parameters: List<UploadTaskBaseParameter>,
    var notificationOptions: NotificationOptions? = null, // 建议使用DownloadNotificationOptions
    var okHttpClient: OkHttpClient? = null,
    var onDownloadProgressUpdate: OnDownloadProgressUpdate? = null,
    var onTaskFailure: OnTaskFailure<UploadTask>? = null,
    var onTaskSuccess: OnTaskSuccess<UploadTask>? = null
)



/**
 * 下载通知配置
 */
open class DownloadNotificationOptions(
    channelId: String = UUID.randomUUID().toString(),
    channelName: String = R.string.text_download_progress_notification_default_channel_name.string,
    channelImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    notificationID: Int = randomNotificationID,
    contentTitle: String = "{fileName}",
    var progressContentText: String = "{length} | {percentage}",//{length} | {percentage}: 1M/2M | 50%
    var successContentText: String = R.string.text_download_progress_notification_default_task_success.string,
    var failureContentText: String = R.string.text_download_progress_notification_default_task_failure.string,
    var isFormatContent: Boolean = true,
    contentIntent: PendingIntent? = null,
    smallIcon: Int,
    largeIcon: Bitmap? = null,
    defaults: Int? = Notification.DEFAULT_ALL, //默认通知选项
    priority: Int? = NotificationCompat.PRIORITY_MAX, // 通知优先级
    progress: NotificationProgressOptions,//进度
    isOngoing: Boolean? = null,// 是否禁用滑动删除
    // 创建自定义NotificationChannel代替默认
    onCreateNotificationChannel: (() -> NotificationChannel)? = null,
    // 设置NotificationCompatBuilder
    onSettingNotificationCompatBuilder: ((NotificationCompat.Builder) -> Unit)? = null
) : NotificationOptions(
    channelId,
    channelName,
    notificationID,
    channelImportance,
    contentTitle,
    progressContentText,
    contentIntent,
    smallIcon,
    largeIcon,
    defaults,
    priority,
    progress,
    isOngoing,
    onCreateNotificationChannel,
    onSettingNotificationCompatBuilder
)