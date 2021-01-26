package com.arcns.core.network

import android.app.NotificationChannel
import android.app.PendingIntent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.arcns.core.APP
import com.arcns.core.R
import com.arcns.core.app.*
import com.arcns.core.file.mimeType
import com.arcns.core.file.tryClose
import com.arcns.core.util.string
import com.arcns.xfile.FileUtil
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Source
import okio.source
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile


/**
 * 上传任务类
 */
open class UploadTask(
    url: String,
    var parameters: List<UploadTaskBaseParameter>,
    var onCustomRequest: ((UploadTask, Request.Builder) -> Unit)? = null, // 自定义Request回调
    notificationOptions: NotificationOptions? = null, // 建议使用UploadNotificationOptions
    okHttpClient: OkHttpClient? = null,
    progressUpdateInterval: Long? = null,
    var onUploadFileProgressUpdate: OnUploadFileProgressUpdate? = null,
    var onUploadFileFailure: OnUploadFileFailure? = null,
    var onUploadFileSuccess: OnUploadFileSuccess? = null,
    onTaskFailure: OnTaskFailure<UploadTask>? = null,
    onTaskSuccess: OnTaskSuccess<UploadTask>? = null,
    extraData: Any? = null
) : NetworkTask<UploadTask>(
    url,
    notificationOptions,
    okHttpClient,
    progressUpdateInterval,
    onTaskFailure,
    onTaskSuccess,
    extraData
) {

    // 唯一标识
    var itemId: Long? = null
        get() {
            if (field == null) {
                field = System.currentTimeMillis()
            }
            return field
        }

    // 获取任务文件列表
    val fileParameters: List<UploadTaskFileParameter>
        get() = parameters.filter {
            it is UploadTaskFileParameter
        }.map { it as UploadTaskFileParameter }


    // 根据状态获取任务文件数量
    fun getFileParametersNumberOnState(state: TaskState): Int = getFileParametersOnState(state).count()

    // 根据状态获取任务文件
    fun getFileParametersOnState(state: TaskState): List<UploadTaskFileParameter> = fileParameters.filter {
        it.state == state
    }

    /**
     * 取消通知栏
     */
    fun cancelNotification() {
        parameters.forEach {
            if (it is UploadTaskFileParameter) it.notificationID.cancelNotification()
        }
    }

    /**
     * 更新通知栏
     */
    fun updateNotification(
        backupNotificationOptions: NotificationOptions? = null
    ) {
        parameters.forEach {
            if (it is UploadTaskFileParameter) it.updateNotification(
                notificationOptions,
                backupNotificationOptions
            )
        }
    }

}

/**
 * 上传任务类更新
 */
open class UploadTaskFileParameterUpdate(
    var task: UploadTask,
    var parameter: UploadTaskFileParameter
)

/**
 * 上传参数基类
 */
abstract class UploadTaskBaseParameter(
    var name: String
)

/**
 * 上传参数（string格式）
 */
open class UploadTaskParameter(
    name: String,
    var value: String
) : UploadTaskBaseParameter(
    name = name
)

/**
 * 上传参数（文件格式）
 */
open class UploadTaskFileParameter : UploadTaskBaseParameter {
    private var source: Source? = null
    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var file: File? = null
    private var filePath: String? = null
    lateinit var fileName: String
    lateinit var showName: String
    var fileMimeType: String  // mimeType，例如 application/octet-stream
    var contentLength: Long = 0
    var breakpoint: Long = 0
    var currentProgress: NetworkTaskProgress? = null
        private set

    // 文件源
    private var standardSource: Source? = null
    private var breakpointResumeSource: RandomAccessFile? = null

    // 当前状态
    var state = TaskState.None


    // 上传通知
    var notificationOptions: NotificationOptions? = null
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

    // 附带数据
    var extraData: Any? = null

    constructor(
        name: String,
        fileName: String,
        showName: String = fileName,
        source: Source,
        contentLength: Long,
        fileMimeType: String,
        notificationOptions: NotificationOptions? = null,
        extraData: Any? = null
    ) : super(name) {
        this.fileName = fileName
        this.showName = showName
        this.source = source
        this.contentLength = contentLength
        this.fileMimeType = fileMimeType
        this.notificationOptions = notificationOptions
        this.extraData = extraData
    }

    constructor(
        name: String,
        fileName: String,
        showName: String = fileName,
        inputStream: InputStream,
        contentLength: Long,
        fileMimeType: String,
        notificationOptions: NotificationOptions? = null,
        extraData: Any? = null
    ) : super(name) {
        this.fileName = fileName
        this.showName = showName
        this.inputStream = inputStream
        this.contentLength = contentLength
        this.fileMimeType = fileMimeType
        this.notificationOptions = notificationOptions
        this.extraData = extraData
    }

    constructor(
        name: String,
        fileName: String,
        showName: String = fileName,
        uri: Uri,
        contentLength: Long? = null,
        fileMimeType: String? = null,
        notificationOptions: NotificationOptions? = null,
        extraData: Any? = null
    ) : super(name) {
        this.fileName = fileName
        this.showName = showName
        this.uri = uri
        this.contentLength = contentLength ?: FileUtil.getFileLengthWithUri(
            APP.INSTANCE,
            uri
        )
        this.fileMimeType =
            fileMimeType ?: FileUtil.getFileSuffixWithUri(
                APP.INSTANCE,
                uri
            ).mimeType
        this.notificationOptions = notificationOptions
        this.extraData = extraData
    }

    constructor(
        name: String,
        fileName: String,
        showName: String = fileName,
        file: File,
        breakpoint: Long = 0,
        contentLength: Long? = null,
        fileMimeType: String? = null,
        notificationOptions: NotificationOptions? = null,
        extraData: Any? = null
    ) : super(name) {
        this.fileName = fileName
        this.showName = showName
        this.file = file
        this.breakpoint = breakpoint
        this.contentLength = contentLength ?: file.length()
        this.fileMimeType =
            fileMimeType ?: FileUtil.getFileSuffix(file.absolutePath).mimeType
        this.notificationOptions = notificationOptions
        this.extraData = extraData
    }

    constructor(
        name: String,
        fileName: String,
        showName: String = fileName,
        filePath: String,
        breakpoint: Long = 0,
        contentLength: Long? = null,
        fileMimeType: String? = null,
        notificationName: String = fileName,
        notificationOptions: NotificationOptions? = null,
        extraData: Any? = null
    ) : super(name) {
        this.fileName = fileName
        this.showName = showName
        this.filePath = filePath
        this.breakpoint = breakpoint
        this.contentLength = contentLength ?: File(filePath).length()
        this.fileMimeType =
            fileMimeType ?: FileUtil.getFileSuffix(filePath).mimeType
        this.notificationOptions = notificationOptions
        this.extraData = extraData
    }

    /**
     * 设置当前进度
     */
    fun updateProgress(progress: Long): NetworkTaskProgress =
        NetworkTaskProgress(contentLength, progress).apply {
            currentProgress = this
        }

    /**
     * 创建标准源（不支持断点续传）
     */
    fun getStandardSource(): Source? {
        standardSource?.run { return this }
        if (source != null) {
            standardSource = source
        } else if (inputStream != null) {
            standardSource = inputStream?.source()
        } else if (uri != null) {
            standardSource =
                APP.INSTANCE.contentResolver.openInputStream(uri!!)?.let { it.source() }
        } else if (file != null) {
            standardSource = if (!file!!.exists()) null else file?.source()
        } else if (filePath != null) {
            standardSource = File(filePath).let { if (!it.exists()) null else it.source() }
        }
        return standardSource
    }

    /**
     * 创建断电续传源
     */
    fun getBreakpointResumeSource(): RandomAccessFile? {
        breakpointResumeSource?.run { return this }
        val sourceFile = file ?: filePath?.let { File(it) }
        if (sourceFile == null || !sourceFile.exists()) return null
        return RandomAccessFile(sourceFile, "rw").apply {
            breakpointResumeSource = this
            if (breakpoint > 0) seek(breakpoint)
        }
    }

    /**
     * 当前任务是否支持断点续传
     */
    val isSupportBreakpointResume
        get() = breakpoint > 0 &&
                (breakpointResumeSource != null
                        || (file ?: filePath?.let { File(it) })?.exists() == true)

    /**
     * 上传文件的目录
     */
    val uploadFilePath: String? get() = file?.absolutePath ?: filePath


    /**
     * 上传文件类型
     */
    val fileMediaType: MediaType?
        get() = fileMimeType.toMediaTypeOrNull()

    /**
     * 关闭源
     */
    fun closeSource() {
        standardSource?.tryClose()
        breakpointResumeSource?.tryClose()
        standardSource = null
        breakpointResumeSource = null
    }

    /**
     * 更新通知栏
     */
    fun updateNotification(
//        progressState: TaskState = TaskState.Running,
        taskNotificationOptions: NotificationOptions? = null,
        backupNotificationOptions: NotificationOptions? = null
    ) {
        val notificationOptions =
            notificationOptions ?: taskNotificationOptions ?: backupNotificationOptions ?: return
        if (notificationOptions.cancelIfDisable(notificationID)) return
        // 判断是否允许自动格式化内容
        if (notificationOptions is UploadNotificationOptions && notificationOptions.isFormatContent) {
            if (notificationOptions.autoCancelOnState?.contains(state) == true) {
                notificationID.cancelNotification()
                return
            }
            notificationOptions.contentTitle =
                formatTaskNotificationPlaceholderContent(
                    notificationOptions.notificationTitle
                )
            if (state == TaskState.Running) {
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
    ): String = content.replace(TASK_NOTIFICATION_PLACEHOLDER_FILE_NAME, fileName)
        .replace(TASK_NOTIFICATION_PLACEHOLDER_SHOW_NAME, showName)
        .replace(
            TASK_NOTIFICATION_PLACEHOLDER_LENGTH,
            currentProgress?.getLengthToString() ?: ""
        ).replace(
            TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE,
            currentProgress?.permissionToString ?: ""
        )

}

/**
 * 上传通知配置
 */
open class UploadNotificationOptions(
    channelId: String = R.string.text_download_progress_notification_default_channel_name.string,
    channelName: String = R.string.text_download_progress_notification_default_channel_name.string,
    channelImportance: Int? = null,
    notificationID: Int = randomNotificationID,
    var notificationTitle: String = "$TASK_NOTIFICATION_PLACEHOLDER_SHOW_NAME",
    var progressContentText: String = "$TASK_NOTIFICATION_PLACEHOLDER_LENGTH | $TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE",//{length} | {percentage}: 1M/2M | 50%
    var autoCancelOnState: List<TaskState>? = null,// 需要上传任务自动取消通知的状态
    var successContentText: String = R.string.text_upload_progress_notification_default_task_success.string,
    var failureContentText: String = R.string.text_upload_progress_notification_default_task_failure.string,
    var pauseContentText: String = R.string.text_upload_progress_notification_default_task_pause.string,
    var cancelContentText: String = R.string.text_upload_progress_notification_default_task_cancel.string,
    var isFormatContent: Boolean = true,
    contentIntent: PendingIntent? = null,
    smallIcon: Int,
    largeIcon: Bitmap? = null,
    defaults: Int? = null, //默认通知选项
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