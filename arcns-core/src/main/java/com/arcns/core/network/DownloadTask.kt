package com.arcns.core.network

import android.app.NotificationChannel
import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.arcns.core.R
import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.NotificationProgressOptions
import com.arcns.core.app.randomNotificationID
import com.arcns.core.file.FileUtil
import com.arcns.core.file.getCurrentTimeMillisFileName
import com.arcns.core.file.tryClose
import com.arcns.core.util.LOG
import com.arcns.core.util.string
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * 下载任务类
 */
open class DownloadTask(
    url: String,
    var saveDirPath: String,
    var saveFileName: String? = null,
    var saveFileSuffix: String? = null,
    var showName: String? = null,
    var isBreakpointResume: Boolean = true, //是否开启断点续传
    var onCustomRequest: ((DownloadTask, Request.Builder) -> Unit)? = null, // 自定义Request回调
    notificationOptions: NotificationOptions? = null, // 建议使用DownloadNotificationOptions
    okHttpClient: OkHttpClient? = null,
    progressUpdateInterval: Long? = null,
    var onDownloadProgressUpdate: OnDownloadProgressUpdate? = null,
    onTaskFailure: OnTaskFailure<DownloadTask>? = null,
    onTaskSuccess: OnTaskSuccess<DownloadTask>? = null,
    extraData: Any? = null
) : NetworkTask<DownloadTask>(
    url,
    notificationOptions,
    okHttpClient,
    progressUpdateInterval,
    onTaskFailure,
    onTaskSuccess,
    extraData
) {

    var saveFullFileName: String? = null
        private set

    init {
        // 补全后缀
        if (!saveFileSuffix.isNullOrBlank() && saveFileSuffix != "." && saveFileSuffix?.startsWith(".") == false) {
            saveFileSuffix = ".$saveFileSuffix"
        }
        if (saveFileName != null && saveFileSuffix != null)
            saveFullFileName = saveFileName + saveFileSuffix
        else saveFullFileName = saveFileName
    }

    // 唯一标识
    var itemId: Long? = null
        get() {
            if (field == null) {
                field = System.currentTimeMillis()
            }
            return field
        }

    // 当前进度
    var currentProgress: NetworkTaskProgress? = null
        private set

    // 文件流
    private var outputStream: OutputStream? = null

    // 上传通知
    private var _notificationID: Int? = null
    val notificationID: Int
        get() {
            if (_notificationID == null) {
                _notificationID = notificationOptions?.notificationID
                if (_notificationID == null) {
                    _notificationID = randomNotificationID
                }
            }
            return _notificationID!!
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
     * 设置保存文件名
     */
    fun completeSaveFullFileName(responseFileName: String?) {
        var newFullName = responseFileName
        var newName = FileUtil.getFileNameNotSuffix(newFullName)
        var newSuffix = FileUtil.getFileSuffixAndVerify(newFullName)
        if (!saveFullFileName.isNullOrBlank()) {
            if (saveFileSuffix == null && !newSuffix.isNullOrBlank()) {
                // 后缀名根据responseFileName补全
                saveFullFileName = saveFileName + newSuffix
            }
            return
        }
        if (newFullName.isNullOrBlank()) {
            // 没有默认名，也没有responseFileName，则自动生成名称
            saveFullFileName = getCurrentTimeMillisFileName(saveFileSuffix)
            return
        }
        if (saveFileSuffix != null && !saveFileSuffix.equals(newSuffix, true)) {
            // 优先使用自定义的后缀
            newSuffix = saveFileSuffix
        }
        saveFullFileName = newName + newSuffix
        var rename = 1
        while (saveFile?.exists() == true) {
            // 文件名被占用时重命名
            saveFullFileName = "$newName($rename)$newSuffix"
            rename++
        }
    }

    /**
     * 设置当前进度
     */
    fun updateProgress(contentLength: Long, progress: Long): NetworkTaskProgress =
        NetworkTaskProgress(contentLength, progress).apply {
            currentProgress = this
            onDownloadProgressUpdate?.invoke(this@DownloadTask, this)
        }

    /**
     * 创建流
     */
    fun getOutputStream(): OutputStream? {
        outputStream?.run { return this }
        FileUtil.mkdirIfNotExists(saveDirPath)
        outputStream = FileOutputStream(saveFile, true)
        return outputStream
    }

    /**
     * 关闭源
     */
    fun closeOutputStream() {
        outputStream?.tryClose()
        outputStream = null
    }

    val saveFilePath: String?
        get() = if (saveFullFileName == null) null else FileUtil.splicing(
            saveDirPath,
            saveFullFileName
        )

    val saveFile: File? get() = if (saveFilePath.isNullOrBlank()) null else File(saveFilePath)

    val breakpoint: Long
        get() {
            LOG("DownLoadTask断点长度：" + saveFile?.length())
            return if (saveFile?.exists() == true) saveFile?.length() ?: 0
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
    defaults: Int? = NotificationCompat.FLAG_ONLY_ALERT_ONCE, //默认通知选项
    priority: Int? = null, // 通知优先级
    progress: NotificationProgressOptions? = null,//进度
    var defaultIsOngoing: Boolean? = null,// 是否禁用滑动删除
    var defaultIsAutoCancel: Boolean? = null,//是否点击时自动取消
    isOnlyAlertOnce: Boolean? = true,//是否只提示一次声音
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
    defaultIsOngoing,
    defaultIsAutoCancel,
    isOnlyAlertOnce,
    onCreateNotificationChannel,
    onSettingNotificationCompatBuilder
)