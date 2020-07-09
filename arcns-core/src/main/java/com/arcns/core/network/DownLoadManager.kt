package com.arcns.core.network

import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.NotificationProgressOptions
import com.arcns.core.app.show
import com.arcns.core.file.tryClose
import com.arcns.core.util.LOG
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


// 下载任务进度事件
typealias OnDownloadProgressUpdate = (DownLoadTask, NetworkTaskProgress) -> Unit


/**
 * 下载管理器
 */
class DownLoadManager {

    // OkHttpClient
    var httpClient: OkHttpClient
        private set

    // 下载任务列表
    val tasks = ArrayList<DownLoadTask>()

    // 每次下载的字节数
    var perByteCount = 2048

    // 下载任务成功回调
    var onTaskSuccess: OnTaskSuccess<DownLoadTask>? = null

    //下载任务失败回调
    var onTaskFailure: OnTaskFailure<DownLoadTask>? = null

    // 下载任务的文件进度回调
    var onProgressUpdate: OnDownloadProgressUpdate? = null

    // 下载时间间隔
    var progressUpdateInterval: Long = 1000

    // 上传通知
    var notificationOptions: NotificationOptions? = null

    // 自定义
    var onCustomRequest: ((DownLoadTask, Request.Builder) -> Unit)? = null


    constructor(httpClient: OkHttpClient? = null) {
        this.httpClient =
            httpClient ?: OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
    }

    @Synchronized
    fun downLoad(
        task: DownLoadTask
    ): Boolean = downLoad(task, false)

    @Synchronized
    private fun downLoad(
        task: DownLoadTask,
        isBreakpointRetry: Boolean
    ): Boolean {
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
            if (!it.isStop && it.saveFilePath == task.saveFilePath) {
                return false
            }
        }
        tasks.add(task)
        var current = 0L
        httpClient.newCall(Request.Builder().apply {
            if (task.isBreakpointResume && !isBreakpointRetry && task.breakpoint > 0) {
                header("Range", "bytes=${task.breakpoint}-") // 设置断点续传
                current = task.breakpoint
            }
            url(task.url)
            onCustomRequest?.invoke(task, this)
        }.build()).apply {
            task.onRunning(this)
            enqueue(object : Callback {
                private var lastProgressUpdateTime: Long = 0
                private val updateInterval = task.progressUpdateInterval ?: progressUpdateInterval

                override fun onFailure(call: Call, e: IOException) {
                    downloadFailure(e, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        var inputStream: InputStream? = null
                        try {
                            val responseBody =
                                response.body() ?: throw Exception("response body not empty")
                            var total = responseBody.contentLength()
                            // 已下载完成
                            if (task.isBreakpointResume && total == task.breakpoint) {
                                downloadSuccess(response)
                                return
                            } else if (task.breakpoint > 0) {
                                // 不启用或不支持断点续传
                                if (!task.isBreakpointResume || isBreakpointRetry || !response.isAcceptRange) {
                                    task.saveFile.deleteOnExit()
                                    current = 0
                                }
                                // 重新计算断点续传的文件长度
                                else {
                                    total += current
                                }
                            }

                            LOG("DownLoadManager:" + total + "  " + task.breakpoint + "  " + current)
                            var outputStream =
                                task.getOutputStream()
                                    ?: throw Exception("download task save file output stream not empty")
                            inputStream = responseBody.byteStream()
                            var len = 0
                            val buf = ByteArray(perByteCount)
                            while (inputStream.read(buf).also { len = it } != -1) {
                                if (task.isStop) {
                                    throw Exception("task is stop")
                                }
                                outputStream.write(buf, 0, len)
                                current += len
                                // 更新进度
                                updateProgress(total, current)
                            }
                            updateProgress(total, total, true)
                            outputStream.flush()

                            LOG("DownLoadManager task onResponse isSuccessful")
                            downloadSuccess(response)
                        } catch (e: Exception) {
                            LOG("DownLoadManager task error :" + e.message)
                            downloadFailure(e, response)
                        } finally {
                            inputStream?.tryClose()
                            task.closeOutputStream()
                        }
                        if (task.state == TaskState.Success) {
                            LOG("DownLoadManager task success file length:" + task.saveFile.length())
                        }

                    } else {
                        LOG("DownLoadManager task onResponse not Successful")
                        if (!isBreakpointRetry && task.isBreakpointResume && task.breakpoint > 0) downLoad(
                            task,
                            true
                        )
                        else downloadFailure(null, response)
                    }
                }

                //下载任务的文件成功回调
                private fun downloadSuccess(response: Response) {
                    task.onSuccess()
                    task.onTaskSuccess?.invoke(task, response)
                    onTaskSuccess?.invoke(task, response)
                    // 更新到通知栏
                    updateNotification()
                    tasks.remove(task)
                }

                //下载任务的文件失败回调
                private fun downloadFailure(e: Exception?, response: Response?) {
                    task.onFailureIfNotStop()
                    task.onTaskFailure?.invoke(task, e, response)
                    onTaskFailure?.invoke(task, e, response)
                    // 更新到通知栏
                    updateNotification()
                    tasks.remove(task)
                }

                /**
                 * 下载任务的文件进度回调
                 */
                private fun updateProgress(total: Long, current: Long, isEnd: Boolean = false) {
                    // 避免短时间内多次回调
                    if (!isEnd && System.currentTimeMillis() - lastProgressUpdateTime < updateInterval) return
                    lastProgressUpdateTime = System.currentTimeMillis()
                    // 避免重复回调
                    if (task.currentProgress?.current == current) return
                    // 更新到任务中，然后进行回调
                    task.updateProgress(total, current)?.run {
                        onProgressUpdate?.invoke(task, this)
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
                            formatTaskNotificationPlaceholderContent(notificationOptions.notificationTitle)
                        if (task.isRunning) {
                            notificationOptions.contentText =
                                formatTaskNotificationPlaceholderContent(notificationOptions.progressContentText)
                            notificationOptions.progress = task.notificationProgress
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
                                    if (task.currentProgress?.indeterminate == true)
                                        notificationOptions.progress =
                                            NotificationProgressOptions.FAILURE
                                }
                                TaskState.Pause -> {
                                    notificationOptions.contentText =
                                        notificationOptions.pauseContentText
                                    if (task.currentProgress?.indeterminate == true)
                                        notificationOptions.progress =
                                            NotificationProgressOptions.FAILURE
                                }
                                TaskState.Cancel -> {
                                    notificationOptions.contentText =
                                        notificationOptions.cancelContentText
                                    if (task.currentProgress?.indeterminate == true)
                                        notificationOptions.progress =
                                            NotificationProgressOptions.FAILURE
                                }
                                else -> return
                            }
                            notificationOptions.isOngoing = false
                            notificationOptions.isAutoCancel = true
                        }
                    }
                    notificationOptions.show(task.notificationID)
                }

                // 返回当前的通知配置
                private fun getNotificationOptions(): NotificationOptions? {
                    if (notificationOptions?.isEnable == false || task.notificationOptions?.isEnable == false) return null
                    return task.notificationOptions ?: notificationOptions
                }

                // 填充占位符
                private fun formatTaskNotificationPlaceholderContent(content: String): String =
                    content.replace(TASK_NOTIFICATION_PLACEHOLDER_FILE_NAME, task.saveFileName)
                        .replace(TASK_NOTIFICATION_PLACEHOLDER_SHOW_NAME, task.showName)
                        .replace(
                            TASK_NOTIFICATION_PLACEHOLDER_LENGTH,
                            task.currentProgress?.getLengthToString() ?: ""
                        ).replace(
                            TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE,
                            task.currentProgress?.permissionToString ?: ""
                        )

            })
        }
        return true
    }

    fun findTaskByUrl(url: String): DownLoadTask? = tasks.firstOrNull { it.url == url }
    fun findTaskByID(id: String): DownLoadTask? = tasks.firstOrNull { it.id == id }
}



