package com.arcns.core.network

import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.NotificationProgressOptions
import com.arcns.core.app.show
import okhttp3.*
import okio.Buffer
import okio.BufferedSink
import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


//上传任务各个文件事件
typealias OnUploadFileSuccess = (UploadTask, UploadFileParameter) -> Unit
typealias OnUploadFileFailure = (UploadTask, UploadFileParameter, Exception?) -> Unit
typealias OnUploadFileProgressUpdate = (UploadTask, UploadFileParameter, TaskProgress) -> Unit

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
    var onUploadFileProgressUpdate: OnUploadFileProgressUpdate? = null


    constructor(httpClient: OkHttpClient? = null) {
        this.httpClient =
            httpClient ?: OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
    }

    /**
     * 上传文件
     */
    fun upLoad(task: UploadTask) {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)//设置类型
        //追加参数
        task.parameters.forEach {
            when (it) {
                is UploadTaskParameter -> builder.addFormDataPart(it.name, it.value)
                is UploadFileParameter -> builder.addFormDataPart(
                    it.name,
                    it.fileName,
                    createUploadFileRequestBody(task, it, perByteCount)
                )
            }
        }
        (task.okHttpClient ?: httpClient).newCall(
            Request.Builder().url(task.url).post(builder.build()).build()
        )
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onTaskFailure?.invoke(task, e, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        onTaskSuccess?.invoke(task, response)
                    } else {
                        onTaskFailure?.invoke(task, null, response)
                    }
                }
            })
    }

    /**
     * 创建上传文件的请求体RequestBody
     */
    fun createUploadFileRequestBody(
        task: UploadTask,
        parameter: UploadFileParameter,
        uploadPerByteCount: Int
    ) =
        object : RequestBody() {
            override fun contentType(): MediaType? = parameter.fileMediaType

            override fun contentLength(): Long = parameter.contentLength

            override fun writeTo(sink: BufferedSink) {
                var current: Long = 0
                try {
                    if (parameter.isSupportBreakpointResume) {
                        // 断点续传
                        current = parameter.breakpoint
                        val source = parameter.createBreakpointResumeSource() ?: return
                        val bytes = ByteArray(uploadPerByteCount)
                        var len: Int
                        while (source.read(bytes).also { len = it } != -1) {
                            sink.write(bytes, 0, len)
                            current += len
                            // 更新进度
                            updateProgress(current)
                        }
                        updateProgress(parameter.contentLength)
                    } else {
                        // 标准上传
                        var source = parameter.createStandardSource() ?: return
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
                        updateProgress(parameter.contentLength)
                    }
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
                val notificationOptions = getNotificationOptions() ?: return
                if (notificationOptions is UploadNotificationOptions && notificationOptions.isFormatContent) {
                    notificationOptions.contentText = notificationOptions.successContentText
                    notificationOptions.progress = NotificationProgressOptions.COMPLETED
                }
                notificationOptions.show(parameter.notificationID)
            }

            //上传任务的文件失败回调
            private fun uploadFileFailure(e: Exception) {
                task.onUploadFileFailure?.invoke(task, parameter, e)
                onUploadFileFailure?.invoke(task, parameter, e)
                // 更新到通知栏
                val notificationOptions = getNotificationOptions() ?: return
                if (notificationOptions is UploadNotificationOptions && notificationOptions.isFormatContent) {
                    notificationOptions.contentText = notificationOptions.failureContentText
                    if (parameter.currentProgress?.indeterminate == true)
                        notificationOptions.progress = NotificationProgressOptions.FAILURE
                }
                notificationOptions.show(parameter.notificationID)
            }


            /**
             * 上传任务的文件进度回调
             */
            private fun updateProgress(current: Long) {
                // 避免重复回调
                if (parameter.currentProgress?.current == current) return
                // 更新到任务中，然后进行回调
                parameter.updateProgress(current).run {
                    task.onUploadFileProgressUpdate?.invoke(task, parameter, this)
                    onUploadFileProgressUpdate?.invoke(task, parameter, this)
                }
                // 更新到通知栏
                val notificationOptions = getNotificationOptions() ?: return
                if (notificationOptions is UploadNotificationOptions && notificationOptions.isFormatContent) {
                    notificationOptions.contentTitle =
                        formatTaskNotificationPlaceholderContent(
                            notificationOptions.contentTitle,
                            parameter
                        )
                    notificationOptions.contentText =
                        formatTaskNotificationPlaceholderContent(
                            notificationOptions.contentText,
                            parameter
                        )
                    notificationOptions.progress = parameter.notificationProgress
                }
                notificationOptions.show(parameter.notificationID)
            }

            private fun getNotificationOptions(): NotificationOptions? {
                if (task.notificationOptions?.isEnable == false || parameter.notificationOptions?.isEnable == false) return null
                return parameter.notificationOptions ?: task.notificationOptions

            }

            // 填充占位符
            private fun formatTaskNotificationPlaceholderContent(
                content: String,
                parameter: UploadFileParameter
            ): String = content.replace(TASK_NOTIFICATION_PLACEHOLDER_FILE_NAME, parameter.name)
                .replace(
                    TASK_NOTIFICATION_PLACEHOLDER_LENGTH,
                    parameter.currentProgress?.getLengthToString() ?: ""
                ).replace(
                    TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE,
                    parameter.currentProgress?.permissionToString ?: ""
                )
        }

}


