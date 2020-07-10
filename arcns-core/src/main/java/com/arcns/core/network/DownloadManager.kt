package com.arcns.core.network

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.arcns.core.app.*
import com.arcns.core.file.tryClose
import com.arcns.core.util.EventObserver
import com.arcns.core.util.LOG
import okhttp3.*
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.util.concurrent.TimeUnit


// 下载任务进度更新回调
typealias OnDownloadProgressUpdate = (DownloadTask, NetworkTaskProgress) -> Unit


/**
 * 下载管理器
 */
class DownloadManager {

    // OkHttpClient
    var httpClient: OkHttpClient
        private set

    // 每次下载的字节数
    var perByteCount = 2048

    // 下载任务成功回调
    var onTaskSuccess: OnTaskSuccess<DownloadTask>? = null

    //下载任务失败回调
    var onTaskFailure: OnTaskFailure<DownloadTask>? = null

    // 下载任务的文件进度回调
    var onProgressUpdate: OnDownloadProgressUpdate? = null

    // 下载时间间隔
    var progressUpdateInterval: Long = 1000

    // 上传通知配置（内容优先级低于任务配置）
    var notificationOptions: NotificationOptions? = null

    // 自定义Request回调
    var onCustomRequest: ((DownloadTask, Request.Builder) -> Unit)? = null

    // 地图管理器绑定的数据
    var managerData: DownloadManagerData
        private set

    // 无生命周期管理时与managerdata连接的对象
    var eventNotifyManagerDownloadObserver: EventObserver<DownloadTask>? = null
        private set

    constructor(httpClient: OkHttpClient? = null) : this(DownloadManagerData(), httpClient)

    constructor(
        managerData: DownloadManagerData,
        httpClient: OkHttpClient? = null
    ) {
        this.httpClient =
            httpClient ?: OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
        this.managerData = managerData
        // 无生命周期管理
        eventNotifyManagerDownloadObserver =
            EventObserver<DownloadTask> {
                download(it, true)
            }
        managerData.eventNotifyManagerDownload.observeForever(eventNotifyManagerDownloadObserver!!)
    }

    constructor(
        lifecycleOwner: LifecycleOwner,
        managerData: DownloadManagerData,
        httpClient: OkHttpClient? = null
    ) {
        this.httpClient =
            httpClient ?: OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
        this.managerData = managerData
        // 拥有生命周期管理
        managerData.eventNotifyManagerDownload.observe(lifecycleOwner, EventObserver {
            download(it, false)
        })
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                lifecycleOwner.lifecycle.removeObserver(this)
                releaseManagerData()
            }
        })
    }


    @Synchronized
    fun download(
        tasks: List<DownloadTask>
    ) = tasks.forEach {
        download(it)
    }

    @Synchronized
    fun download(
        task: DownloadTask
    ): Boolean = managerData.download(task)

    @Synchronized
    private fun download(
        task: DownloadTask,
        isBreakpointRetry: Boolean
    ): Boolean {
        // 开始下载
        var current = 0L
        httpClient.newCall(Request.Builder().apply {
            // 断点续传
            if (task.isBreakpointResume && !isBreakpointRetry && task.breakpoint > 0) {
                header("Range", "bytes=${task.breakpoint}-") // 设置断点续传
                current = task.breakpoint // 设置开始位置
            }
            // 设置下载路径
            url(task.url)
            // 自定义Request回调
            onCustomRequest?.invoke(task, this)
        }.build()).apply {
            // 更新任务状态为运行中
            task.onChangeStateToRunning(this)
            // 封装请求回调处理
            enqueue(object : Callback {
                // 上一次更新时间（用于与更新间隔做对比）
                private var lastProgressUpdateTime: Long = 0

                // 更新间隔
                private val updateInterval = task.progressUpdateInterval ?: progressUpdateInterval

                override fun onFailure(call: Call, e: IOException) {
                    // 任务失败回调
                    downloadFailure(e, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        var inputStream: InputStream? = null
                        try {
                            val responseBody =
                                response.body() ?: throw Exception("response body not empty")
                            // 获取下载任务的长度
                            var total = responseBody.contentLength()
                            // 判断断点继传相关
                            if (isBreakpointRetry && task.isBreakpointResume && total == task.breakpoint) {
                                // 任务长度与已下载的断点长度相同，则表示已下载完成
                                downloadSuccess(response)//任务成功回调
                                return
                            } else if (task.breakpoint > 0) {
                                // 不启用或不支持断点续传，则删除之前的文件
                                if (!task.isBreakpointResume || isBreakpointRetry || !response.isAcceptRange) {
                                    task.saveFile.deleteOnExit()//删除之前的文件
                                    current = 0 // 重置开始位置
                                } else {
                                    // 确认启动断点续传，重新计算断点续传的文件长度（因为使用Range头后，responseBody.contentLength只会返回剩余的大小）
                                    total += current
                                }
                            }
                            task.completeSaveFullFileName(response.fileName)
                            LOG("DownLoadTask fileName: " + response.fileName)
                            LOG("DownLoadTask:$total  $current")
                            // 开始循环接收数据流
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
                                // 更新进度回调
                                updateProgress(total, current)
                            }
                            // 下载完成后，再更新一次进度回调
                            total = current
                            updateProgress(total, current, true)
                            outputStream.flush()
                            // 任务完成回调
                            downloadSuccess(response)
                        } catch (e: Exception) {
                            // 任务失败回调
                            downloadFailure(e, response)
                        } finally {
                            LOG("DownLoadTask 释放连接" + task.breakpoint)
                            // 释放连接
                            inputStream?.tryClose()
                            task.closeOutputStream()
                        }

                    } else {
                        // 若断点继传使用Range头后请求失败，则尝试不使用Range头进行调用（重试）
                        if (!isBreakpointRetry && task.isBreakpointResume && task.breakpoint > 0) download(
                            task,
                            true
                        )
                        // 任务失败回调
                        else downloadFailure(null, response)
                    }
                }

                //下载任务成功回调
                private fun downloadSuccess(response: Response) {
                    // 更新状态
                    task.onChangeStateToSuccess()
                    // 回调
                    task.onTaskSuccess?.invoke(task, response)
                    onTaskSuccess?.invoke(task, response)
                    // 更新到通知栏
                    updateNotification()
                    managerData?.onEventTaskUpdateByState(task)
                }

                //下载任务失败（含取消、暂停）回调
                private fun downloadFailure(e: Exception?, response: Response?) {
                    // 更新状态
                    task.onChangeStateToFailureIfNotStop()
                    // 回调
                    task.onTaskFailure?.invoke(task, e, response)
                    onTaskFailure?.invoke(task, e, response)
                    // 更新到通知栏
                    updateNotification()
                    managerData?.onEventTaskUpdateByState(task)
                }

                /**
                 * 下载任务的文件进度回调
                 */
                private fun updateProgress(total: Long, current: Long, isEnd: Boolean = false) {
                    // 判断回调间隔，避免短时间内多次回调
                    if (!isEnd && System.currentTimeMillis() - lastProgressUpdateTime < updateInterval) return
                    lastProgressUpdateTime = System.currentTimeMillis()
                    // 避免相同进度重复回调
                    if (task.currentProgress?.current == current) return
                    // 更新到任务中，然后进行回调
                    task.updateProgress(total, current)?.run {
                        onProgressUpdate?.invoke(task, this)
                    }
                    // 更新到通知栏
                    updateNotification()
                    managerData?.onEventTaskUpdateByProgress(task)
                }

                /**
                 * 更新通知栏
                 */
                private fun updateNotification() {
                    val notificationOptions = getNotificationOptions() ?: return
                    if (notificationOptions.cancelIfDisable(task.notificationID)) return
                    LOG("updateNotification show " + task.notificationID)
                    // 判断是否允许自动格式化内容
                    if (notificationOptions is DownloadNotificationOptions && notificationOptions.isFormatContent) {
                        notificationOptions.contentTitle =
                            formatTaskNotificationPlaceholderContent(notificationOptions.notificationTitle).getAbbreviatedText(
                                20
                            )
                        if (task.isRunning) {
                            notificationOptions.contentText =
                                formatTaskNotificationPlaceholderContent(notificationOptions.progressContentText)
                            notificationOptions.progress = task.notificationProgress
                            notificationOptions.isOngoing =
                                notificationOptions.defaultIsOngoing ?: true
                            notificationOptions.isAutoCancel =
                                notificationOptions.defaultIsAutoCancel ?: false
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
                            notificationOptions.isOngoing =
                                notificationOptions.defaultIsOngoing ?: false
                            notificationOptions.isAutoCancel =
                                notificationOptions.defaultIsAutoCancel ?: true
                        }
                    }
                    notificationOptions.show(task.notificationID)
                }

                // 返回当前的通知配置
                private fun getNotificationOptions(): NotificationOptions? {
                    return task.notificationOptions ?: notificationOptions
                }

                // 填充占位符
                private fun formatTaskNotificationPlaceholderContent(content: String): String =
                    content.replace(
                        TASK_NOTIFICATION_PLACEHOLDER_FILE_NAME,
                        task.saveFullFileName ?: ""
                    )
                        .replace(
                            TASK_NOTIFICATION_PLACEHOLDER_SHOW_NAME,
                            task.showName ?: task.saveFullFileName ?: ""
                        )
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

    /**
     * 释放ManagerData
     */
    fun releaseManagerData() {
        eventNotifyManagerDownloadObserver?.run {
            managerData.eventNotifyManagerDownload.removeObserver(this)
            eventNotifyManagerDownloadObserver = null
        }
        managerData.cancelAllTask()
    }
}



