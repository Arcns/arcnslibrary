package com.arcns.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.arcns.core.APP
import com.arcns.core.R
import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.NotificationProgressOptions
import com.arcns.core.app.randomNotificationID
import com.arcns.core.file.FileUtil
import com.arcns.core.file.mimeType
import com.arcns.core.util.string
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okio.Okio
import okio.Source
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.*


/**
 * 上传任务类
 */
data class UploadTask(
    var url: String? = null,
    var parameters: List<UploadTaskBaseParameter>,
    var notificationOptions: NotificationOptions? = null, // 建议使用UploadNotificationOptions
    var okHttpClient: OkHttpClient? = null,
    var onUploadFileProgressUpdate: OnUploadFileProgressUpdate? = null,
    var onUploadFileFailure: OnUploadFileFailure? = null,
    var onUploadFileSuccess: OnUploadFileSuccess? = null,
    var onTaskFailure: OnTaskFailure<UploadTask>? = null,
    var onTaskSuccess: OnTaskSuccess<UploadTask>? = null
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
open class UploadFileParameter : UploadTaskBaseParameter {
    private var source: Source? = null
    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var file: File? = null
    private var filePath: String? = null
    var fileName: String? = null
    var fileMimeType: String  // mimeType，例如 application/octet-stream
    var contentLength: Long = 0
    var breakpoint: Long = 0
    var currentProgress: TaskProgress? = null
        private set

    // 文件源
    private var standardSource: Source? = null
    private var breakpointResumeSource: RandomAccessFile? = null

    // 上传通知
    var notificationOptions: NotificationOptions? = null
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

    constructor(
        name: String,
        fileName: String,
        source: Source,
        contentLength: Long,
        fileMimeType: String,
        notificationOptions: NotificationOptions? = null
    ) : super(name) {
        this.fileName = fileName
        this.source = source
        this.contentLength = contentLength
        this.fileMimeType = fileMimeType
        this.notificationOptions = notificationOptions
    }

    constructor(
        name: String,
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        fileMimeType: String,
        notificationOptions: NotificationOptions? = null
    ) : super(name) {
        this.fileName = fileName
        this.inputStream = inputStream
        this.contentLength = contentLength
        this.fileMimeType = fileMimeType
        this.notificationOptions = notificationOptions
    }

    constructor(
        name: String,
        fileName: String,
        uri: Uri,
        contentLength: Long? = null,
        fileMimeType: String? = null,
        notificationOptions: NotificationOptions? = null
    ) : super(name) {
        this.fileName = fileName
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
    }

    constructor(
        name: String,
        fileName: String,
        file: File,
        breakpoint: Long = 0,
        contentLength: Long? = null,
        fileMimeType: String? = null,
        notificationOptions: NotificationOptions? = null
    ) : super(name) {
        this.fileName = fileName
        this.file = file
        this.breakpoint = breakpoint
        this.contentLength = contentLength ?: file.length()
        this.fileMimeType =
            fileMimeType ?: FileUtil.getFileSuffix(file.absolutePath).mimeType
        this.notificationOptions = notificationOptions
    }

    constructor(
        name: String,
        fileName: String,
        filePath: String,
        breakpoint: Long = 0,
        contentLength: Long? = null,
        fileMimeType: String? = null,
        notificationOptions: NotificationOptions? = null
    ) : super(name) {
        this.fileName = fileName
        this.filePath = filePath
        this.breakpoint = breakpoint
        this.contentLength = contentLength ?: File(filePath).length()
        this.fileMimeType =
            fileMimeType ?: FileUtil.getFileSuffix(filePath).mimeType
        this.notificationOptions = notificationOptions
    }

    /**
     * 设置当前进度
     */
    fun updateProgress(progress: Long): TaskProgress =
        TaskProgress(contentLength, progress).apply {
            currentProgress = this
        }

    /**
     * 创建标准源（不支持断点续传）
     */
    fun createStandardSource(): Source? {
        standardSource?.run { return this }
        if (source != null) {
            standardSource = source
        } else if (inputStream != null) {
            standardSource = Okio.source(inputStream)
        } else if (uri != null) {
            standardSource =
                APP.INSTANCE.contentResolver.openInputStream(uri!!)?.let { Okio.source(it) }
        } else if (file != null) {
            standardSource = if (!file!!.exists()) null else Okio.source(file)
        } else if (filePath != null) {
            standardSource = File(filePath).let { if (!it.exists()) null else Okio.source(it) }
        }
        return standardSource
    }

    /**
     * 创建断电续传源
     */
    fun createBreakpointResumeSource(): RandomAccessFile? {
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
    val isSupportBreakpointResume get() = breakpoint > 0 && createBreakpointResumeSource() != null


    /**
     * 上传文件类型
     */
    val fileMediaType: MediaType?
        get() = MediaType.parse(fileMimeType)

    /**
     * 关闭源
     */
    fun closeSource() {
        if (standardSource != null) {
            try {
                standardSource?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            standardSource = null
        }
        if (breakpointResumeSource != null) {
            try {
                breakpointResumeSource?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            breakpointResumeSource = null
        }
    }

}



/**
 * 上传通知配置
 */
open class UploadNotificationOptions(
    channelId: String = UUID.randomUUID().toString(),
    channelName: String = R.string.text_download_progress_notification_default_channel_name.string,
    channelImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    notificationID: Int = randomNotificationID,
    contentTitle: String = "{fileName}",
    var progressContentText: String = "{length} | {percentage}",//{length} | {percentage}: 1M/2M | 50%
    var successContentText: String = R.string.text_upload_progress_notification_default_task_success.string,
    var failureContentText: String = R.string.text_upload_progress_notification_default_task_failure.string,
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
    channelImportance,
    notificationID,
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