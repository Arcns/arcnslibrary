package com.arcns.core.network

import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.NotificationProgressOptions
import com.arcns.core.app.show
import com.arcns.core.util.LOG
import okhttp3.*
import okio.Buffer
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


//上传任务各个文件事件
typealias OnUploadFileSuccess = (UploadTask, UploadFileParameter) -> Unit
typealias OnUploadFileFailure = (UploadTask, UploadFileParameter, Exception?) -> Unit
typealias OnUploadFileProgressUpdate = (UploadTask, UploadFileParameter, NetworkTaskProgress) -> Unit

/**
 * 上传管理器
 */
class UploadManager {

    // OkHttpClient
    var httpClient: OkHttpClient
        private set

    //上传任务列表
    var tasks = ArrayList<UploadTask>()

    // 每次上传的字节数
    var perByteCount = 2048

    // 上传任务成功回调
    var onTaskSuccess: OnTaskSuccess<UploadTask>? = null

    //上传任务失败回调
    var onTaskFailure: OnTaskFailure<UploadTask>? = null

    // 上传任务的文件成功回调
    var onUploadFileSuccess: OnUploadFileSuccess? = null

    // 上传任务的文件失败回调
    var onUploadFileFailure: OnUploadFileFailure? = null

    // 上传任务的文件进度回调
    var onProgressUpdate: OnUploadFileProgressUpdate? = null

    // 上传时间间隔
    var progressUpdateInterval: Long = 1000

    // 自定义
    var onCustomMultipartBody: ((UploadTask, MultipartBody.Builder) -> Unit)? = null
    var onCustomRequest: ((UploadTask, Request.Builder) -> Unit)? = null


    constructor(httpClient: OkHttpClient? = null) {
        this.httpClient =
            httpClient ?: OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
    }

    /**
     * 上传文件
     */
    @Synchronized
    fun upLoad(task: UploadTask): Boolean {
        tasks.removeAll {
            it.isStop
        }
        if (tasks.contains(task)) {
            return false
        }
        tasks.forEach {
            if (it.id == task.id) {
                return false
            }
        }
        tasks.add(task)
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)//设置类型
        //追加参数
        task.parameters.forEach {
            when (it) {
                is UploadTaskParameter -> bodyBuilder.addFormDataPart(it.name, it.value)
                is UploadFileParameter -> bodyBuilder.addFormDataPart(
                    it.name,
                    it.fileName,
                    createUploadFileRequestBody(
                        task,
                        it,
                        perByteCount,
                        task.progressUpdateInterval ?: progressUpdateInterval
                    )
                )
            }
        }
        onCustomMultipartBody?.invoke(task, bodyBuilder)
        (task.okHttpClient ?: httpClient).newCall(
            Request.Builder().apply {
                url(task.url)
                post(bodyBuilder.build())
                onCustomRequest?.invoke(task, this)
            }.build()
        ).apply {
            task.onRunning(this)
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LOG("UploadManager task onFailure")
                    task.onFailureIfNotStop()
                    onTaskFailure?.invoke(task, e, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        LOG("UploadManager task onResponse isSuccessful")
                        task.onSuccess()
                        onTaskSuccess?.invoke(task, response)
                    } else {
                        LOG("UploadManager task onResponse not Successful")
                        task.onFailureIfNotStop()
                        onTaskFailure?.invoke(task, null, response)
                    }
                }
            })
        }
        return true
    }

    /**
     * 创建上传文件的请求体RequestBody
     */
    private fun createUploadFileRequestBody(
        task: UploadTask,
        parameter: UploadFileParameter,
        uploadPerByteCount: Int,
        updateInterval: Long
    ) =
        object : RequestBody() {
            private var lastProgressUpdateTime: Long = 0

            override fun contentType(): MediaType? = parameter.fileMediaType

            override fun contentLength(): Long = parameter.contentLength

            override fun writeTo(sink: BufferedSink) {
                var current: Long = 0
                try {
                    if (parameter.isSupportBreakpointResume) {
                        // 断点续传
                        current = parameter.breakpoint
                        val source = parameter.getBreakpointResumeSource()
                            ?: throw Exception("file parameter source not empty")
                        val bytes = ByteArray(uploadPerByteCount)
                        var len: Int
                        while (source.read(bytes).also { len = it } != -1) {
                            if (task.isStop) {
                                throw Exception("task is stop")
                            }
                            sink.write(bytes, 0, len)
                            current += len
                            // 更新进度
                            updateProgress(current)
                        }
                    } else {
                        // 标准上传
                        var source = parameter.getStandardSource()
                            ?: throw Exception("file parameter source not empty")
                        val buf = Buffer()
                        var len: Long
                        while (source.read(buf, uploadPerByteCount.toLong())
                                .also { len = it } != -1L
                        ) {
                            sink.write(buf, len)
                            current += len
                            // 更新进度
                            updateProgress(current)
                        }
                    }
                    updateProgress(parameter.contentLength, true)
                    // 上传任务的文件成功回调
                    uploadFileSuccess()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 上传任务的文件失败回调
                    uploadFileFailure(e)
                } finally {
                    parameter.closeSource()
                }
            }

            //上传任务的文件成功回调
            private fun uploadFileSuccess() {
                task.onUploadFileSuccess?.invoke(task, parameter)
                onUploadFileSuccess?.invoke(task, parameter)
                // 更新到通知栏
                updateNotification()
            }

            //上传任务的文件失败回调
            private fun uploadFileFailure(e: Exception) {
                task.onUploadFileFailure?.invoke(task, parameter, e)
                onUploadFileFailure?.invoke(task, parameter, e)
                // 更新到通知栏
                updateNotification()
            }


            /**
             * 上传任务的文件进度回调
             */
            private fun updateProgress(current: Long, isEnd: Boolean = false) {
                // 避免短时间内多次回调
                if (!isEnd && System.currentTimeMillis() - lastProgressUpdateTime < updateInterval) return
                lastProgressUpdateTime = System.currentTimeMillis()
                // 避免重复回调
                if (parameter.currentProgress?.current == current) return
                // 更新到任务中，然后进行回调
                parameter.updateProgress(current).run {
                    task.onUploadFileProgressUpdate?.invoke(task, parameter, this)
                    onProgressUpdate?.invoke(task, parameter, this)
                }
                // 更新到通知栏
                updateNotification()
            }

            /**
             * 更新通知栏
             */
            private fun updateNotification() {
                val notificationOptions = getNotificationOptions() ?: return
                if (notificationOptions is DownloadNotificationOptions && notificationOptions.isFormatContent) {
                    notificationOptions.contentTitle =
                        formatTaskNotificationPlaceholderContent(
                            notificationOptions.notificationTitle,
                            parameter
                        )
                    if (task.isRunning) {
                        notificationOptions.contentText =
                            formatTaskNotificationPlaceholderContent(
                                notificationOptions.progressContentText,
                                parameter
                            )
                        notificationOptions.progress = parameter.notificationProgress
                        notificationOptions.isOngoing = true
                        notificationOptions.isAutoCancel = false
                    } else {
                        when (task.state) {
                            TaskState.Success -> {
                                notificationOptions.contentText =
                                    notificationOptions.successContentText
                                notificationOptions.progress =
                                    NotificationProgressOptions.COMPLETED
                            }
                            TaskState.Failure -> {
                                notificationOptions.contentText =
                                    notificationOptions.failureContentText
                                if (parameter.currentProgress?.indeterminate == true)
                                    notificationOptions.progress =
                                        NotificationProgressOptions.FAILURE
                            }
                            TaskState.Pause -> {
                                notificationOptions.contentText =
                                    notificationOptions.pauseContentText
                                if (parameter.currentProgress?.indeterminate == true)
                                    notificationOptions.progress =
                                        NotificationProgressOptions.FAILURE
                            }
                            TaskState.Cancel -> {
                                notificationOptions.contentText =
                                    notificationOptions.cancelContentText
                                if (parameter.currentProgress?.indeterminate == true)
                                    notificationOptions.progress =
                                        NotificationProgressOptions.FAILURE
                            }
                            else -> return
                        }
                        notificationOptions.isOngoing = false
                        notificationOptions.isAutoCancel = true
                    }
                }
                notificationOptions.show(parameter.notificationID)
            }

            // 返回当前的通知配置
            private fun getNotificationOptions(): NotificationOptions? {
                if (task.notificationOptions?.isEnable == false || parameter.notificationOptions?.isEnable == false) return null
                return parameter.notificationOptions ?: task.notificationOptions
            }

            // 填充占位符
            private fun formatTaskNotificationPlaceholderContent(
                content: String,
                parameter: UploadFileParameter
            ): String = content.replace(TASK_NOTIFICATION_PLACEHOLDER_FILE_NAME, parameter.fileName)
                .replace(TASK_NOTIFICATION_PLACEHOLDER_SHOW_NAME, parameter.showName)
                .replace(
                    TASK_NOTIFICATION_PLACEHOLDER_LENGTH,
                    parameter.currentProgress?.getLengthToString() ?: ""
                ).replace(
                    TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE,
                    parameter.currentProgress?.permissionToString ?: ""
                )
        }


    fun findTaskByUrl(url: String): UploadTask? = tasks.firstOrNull { it.url == url }
    fun findTaskByID(id: String): UploadTask? = tasks.firstOrNull { it.id == id }

}


