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
import com.arcns.core.file.FileUtil
import com.arcns.core.file.tryClose
import com.arcns.core.util.string
import okhttp3.OkHttpClient
import okio.Source
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


/**
 * 下载任务类
 */
open class DownLoadTask(
    url: String,
    var saveDirPath: String,
    var saveFileName: String,
    var showName: String = saveFileName,
    var isBreakpointResume: Boolean = true, //是否开启断点续传
    notificationOptions: NotificationOptions? = null, // 建议使用DownloadNotificationOptions
    okHttpClient: OkHttpClient? = null,
    progressUpdateInterval: Long? = null,
    var onDownloadProgressUpdate: OnDownloadProgressUpdate? = null,
    onTaskFailure: OnTaskFailure<DownLoadTask>? = null,
    onTaskSuccess: OnTaskSuccess<DownLoadTask>? = null,
    extraData: Any? = null
) : NetworkTask<DownLoadTask>(
    url,
    notificationOptions,
    okHttpClient,
    progressUpdateInterval,
    onTaskFailure,
    onTaskSuccess,
    extraData
) {
    var currentProgress: NetworkTaskProgress? = null
        private set

    // 文件流
    private var outputStream: OutputStream? = null

    // 上传通知
    private var _notificationID: Int? = null
    val notificationID: Int
        get() {
            var value = notificationOptions?.notificationID
            if (value == null) {
                if (_notificationID == null) {
                    _notificationID = randomNotificationID
                }
                value = _notificationID!!
            }
            return value
        }
    val notificationProgress: NotificationProgressOptions
        get() {
            return NotificationProgressOptions(
                100,
                currentProgress?.percentage ?: 0,
                (currentProgress?.total ?: 0L) <= 0L
            )
        }

    /**
     * 设置当前进度
     */
    fun updateProgress(contentLength: Long, progress: Long): NetworkTaskProgress =
        NetworkTaskProgress(contentLength, progress).apply {
            currentProgress = this
            onDownloadProgressUpdate?.invoke(this@DownLoadTask, this)
        }

    /**
     * 创建流
     */
    fun getOutputStream(): OutputStream? {
        outputStream?.run { return this }
        FileUtil.mkdirIfNotExists(saveDirPath)
        outputStream = FileOutputStream(saveFile)
        return outputStream
    }

    /**
     * 关闭源
     */
    fun closeOutputStream() {
        outputStream?.tryClose()
        outputStream = null
    }

    val saveFilePath: String get() = FileUtil.splicing(saveDirPath, saveFileName)

    val saveFile: File get() = File(saveFilePath)

    val breakpoint: Long
        get() {
            return if (saveFile.exists()) saveFile.length()
            else 0
        }
}


/**
 * 下载通知配置
 */
open class DownloadNotificationOptions(
    channelId: String = R.string.text_download_progress_notification_default_channel_name.string,
    channelName: String = R.string.text_download_progress_notification_default_channel_name.string,
    channelImportance: Int? = null,
    notificationID: Int = randomNotificationID,
    var notificationTitle: String = "$TASK_NOTIFICATION_PLACEHOLDER_SHOW_NAME",
    var progressContentText: String = "$TASK_NOTIFICATION_PLACEHOLDER_LENGTH | $TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE",//{length} | {percentage}: 1M/2M | 50%
    var successContentText: String = R.string.text_download_progress_notification_default_task_success.string,
    var failureContentText: String = R.string.text_download_progress_notification_default_task_failure.string,
    var pauseContentText: String = R.string.text_download_progress_notification_default_task_pause.string,
    var cancelContentText: String = R.string.text_download_progress_notification_default_task_cancel.string,
    var isFormatContent: Boolean = true,
    contentIntent: PendingIntent? = null,
    smallIcon: Int,
    largeIcon: Bitmap? = null,
    defaults: Int? = Notification.DEFAULT_ALL, //默认通知选项
    priority: Int? = NotificationCompat.PRIORITY_MAX, // 通知优先级
    progress: NotificationProgressOptions? = null,//进度
    isOngoing: Boolean? = true,// 是否禁用滑动删除
    isAutoCancel: Boolean? = false,//是否点击时自动取消
    // 创建自定义NotificationChannel代替默认
    onCreateNotificationChannel: (() -> NotificationChannel)? = null,
    // 设置NotificationCompatBuilder
    onSettingNotificationCompatBuilder: ((NotificationCompat.Builder) -> Unit)? = null
) : NotificationOptions(
    channelId,
    channelName,
    channelImportance,
    notificationID,
    notificationTitle,
    progressContentText,
    contentIntent,
    smallIcon,
    largeIcon,
    defaults,
    priority,
    progress,
    isOngoing,
    isAutoCancel,
    onCreateNotificationChannel,
    onSettingNotificationCompatBuilder
)