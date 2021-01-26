package com.arcns.core.network

import android.app.NotificationChannel
import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.arcns.core.R
import com.arcns.core.app.*
import com.arcns.core.file.getCurrentTimeMillisFileName
import com.arcns.core.file.tryClose
import com.arcns.core.util.LOG
import com.arcns.core.util.string
import com.arcns.xfile.FileUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * 下载任务类
 */
open class DownloadTask(
    url: String, // 下载文件的地址
    var saveDirPath: String, //下载文件保存的目录
    var saveFileName: String? = null,//下载文件保存的文件名（若为null，则根据下载地址进行获取，若获取不到，则自动生成一个文件名）
    var saveFileSuffix: String? = null,//下载文件保存的后缀名（若为null，则根据下载地址进行获取，若获取不到，则文件保存后不包含后缀）
    var showName: String? = null,// 在通知栏中显示的名称（若为null，则显示saveFullShowName，若saveFullShowName也为空，则显示showNameWhenEmpty）
    var showNameWhenEmpty: String? = R.string.text_download_progress_notification_default_show_name_when_empty.string,// 当showName、saveFullShowName为null时在通知栏中显示的名称
    var isBreakpointResume: Boolean = true, //是否开启断点续传（若服务器无该功能，则会自动重新开始下载）
    var onCustomRequest: ((DownloadTask, Request.Builder) -> Unit)? = null, // 自定义请求（Request）回调，能够使用该回调对请求进行操作
    notificationOptions: NotificationOptions? = null, // 通知栏配置（建议使用DownloadNotificationOptions，若不需要通知栏可使用NotificationOptions.DISABLE）
    okHttpClient: OkHttpClient? = null,// 使用自定义的OkHttpClient（若为null，则使用管理器的OkHttpClient）
    progressUpdateInterval: Long? = null,// 进度更新间隔（若为null，则使用管理器的progressUpdateInterval）
    var onDownloadProgressUpdate: OnDownloadProgressUpdate? = null,// 进度更新回调
    onTaskFailure: OnTaskFailure<DownloadTask>? = null,// 任务失败回调（包含取消、暂停、失败）
    onTaskSuccess: OnTaskSuccess<DownloadTask>? = null,// 任务成功回调
    extraData: Any? = null // 任务中可携带的自定义数据
) : NetworkTask<DownloadTask>(
    url,
    notificationOptions,
    okHttpClient,
    progressUpdateInterval,
    onTaskFailure,
    onTaskSuccess,
    extraData
) {
    // 完整保存的文件名称（若任务需要根据下载地址进行获取时，那在任务未运行时，该值为空）
    var saveFullFileName: String? = null
        private set

    // 完整的显示名称（优先级依次为showName、saveFullFileName、showNameWhenEmpty）
    val saveFullShowName: String get() = showName ?: saveFullFileName ?: showNameWhenEmpty ?: ""

    init {
        // 自动补全saveFileSuffix
        if (!saveFileSuffix.isNullOrBlank() && saveFileSuffix != "." && saveFileSuffix?.startsWith(".") == false) {
            saveFileSuffix = ".$saveFileSuffix"
        }
        // 自动拼接saveFullFileName
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

    // 保存的文件流
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
    fun updateProgress(
        contentLength: Long,
        progress: Long
    ): NetworkTaskProgress =
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

    /**
     * 保存的文件路径
     */
    val saveFilePath: String?
        get() = if (saveFullFileName == null) null else FileUtil.splicing(
            saveDirPath,
            saveFullFileName
        )

    /**
     * 保存的文件
     */
    val saveFile: File? get() = if (saveFilePath.isNullOrBlank()) null else File(saveFilePath)

    /**
     * 断点
     */
    val breakpoint: Long
        get() {
            LOG("DownLoadTask断点长度：" + saveFile?.length())
            return if (saveFile?.exists() == true) saveFile?.length() ?: 0
            else 0
        }

    /**
     * 取消任务栏
     */
    fun cancelNotification() = notificationID.cancelNotification()

    /**
     * 更新通知栏
     */
    fun updateNotification(backupNotificationOptions: NotificationOptions? = null) {
        val notificationOptions =
            notificationOptions ?: backupNotificationOptions ?: return
        if (notificationOptions.cancelIfDisable(notificationID)) return
        LOG("updateNotification show $notificationID")
        // 判断是否允许自动格式化内容
        if (notificationOptions is DownloadNotificationOptions && notificationOptions.isFormatContent) {
            if (notificationOptions.autoCancelOnState?.contains(state) == true) {
                notificationID.cancelNotification()
                return
            }
            notificationOptions.contentTitle =
                formatTaskNotificationPlaceholderContent(
                    notificationOptions.notificationTitle
                ).getAbbreviatedText(
                    20
                )
            if (isRunning) {
                notificationOptions.contentText =
                    formatTaskNotificationPlaceholderContent(
                        notificationOptions.progressContentText
                    )
                notificationOptions.progress = notificationProgress
                notificationOptions.isOngoing =
                    notificationOptions.defaultIsOngoing ?: true
                notificationOptions.isAutoCancel =
                    notificationOptions.defaultIsAutoCancel ?: false
            } else {
                when (state) {
                    TaskState.Wait -> {
                        notificationOptions.contentText =
                            notificationOptions.waitContentText
                        notificationOptions.progress =
                            NotificationProgressOptions.NONE
                    }
                    TaskState.Success -> {
                        notificationOptions.contentText =
                            notificationOptions.successContentText
                        notificationOptions.progress =
                            NotificationProgressOptions.COMPLETED
                    }
                    TaskState.Failure -> {
                        notificationOptions.contentText =
                            notificationOptions.failureContentText
                        if (currentProgress?.indeterminate != false)
                            notificationOptions.progress =
                                NotificationProgressOptions.NONE
                    }
                    TaskState.Pause -> {
                        notificationOptions.contentText =
                            notificationOptions.pauseContentText
                        if (currentProgress?.indeterminate != false)
                            notificationOptions.progress =
                                NotificationProgressOptions.NONE
                    }
                    TaskState.Cancel -> {
                        notificationOptions.contentText =
                            notificationOptions.cancelContentText
                        if (currentProgress?.indeterminate != false)
                            notificationOptions.progress =
                                NotificationProgressOptions.NONE
                    }
                    else -> return
                }
                notificationOptions.isOngoing =
                    notificationOptions.defaultIsOngoing ?: false
                notificationOptions.isAutoCancel =
                    notificationOptions.defaultIsAutoCancel ?: true
            }
        }
        notificationOptions.show(notificationID)
    }

    // 填充占位符
    fun formatTaskNotificationPlaceholderContent(
        content: String
    ): String =
        content.replace(
            TASK_NOTIFICATION_PLACEHOLDER_FILE_NAME, saveFullFileName ?: ""
        ).replace(
            TASK_NOTIFICATION_PLACEHOLDER_SHOW_NAME, saveFullShowName
        ).replace(
            TASK_NOTIFICATION_PLACEHOLDER_LENGTH, currentProgress?.getLengthToString() ?: ""
        ).replace(
            TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE, currentProgress?.permissionToString ?: ""
        )
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
    var autoCancelOnState: List<TaskState>? = null,// 需要下载任务自动取消通知的状态
    var waitContentText: String = R.string.text_download_progress_notification_default_task_wait.string,
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