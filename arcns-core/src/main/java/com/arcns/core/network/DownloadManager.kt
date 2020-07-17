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


// 下载任务进度更新回调
typealias OnDownloadProgressUpdate = (DownloadTask, NetworkTaskProgress) -> Unit


/**
 * 下载管理器
 */
class DownloadManager {

    // 地图管理器绑定的数据
    var managerData: DownloadManagerData
        private set

    // 无生命周期管理时与managerdata连接的对象
    var eventDownloadManagerNotifyObserver: EventObserver<DownloadManagerNotify>? = null
        private set

    constructor(
        managerData: DownloadManagerData
    ) {
        this.managerData = managerData
        // 无生命周期管理
        eventDownloadManagerNotifyObserver = EventObserver {
            handleDownloadManagerNotify(it)
        }
        managerData.eventDownloadManagerNotify.observeForever(eventDownloadManagerNotifyObserver!!)
    }

    constructor(
        lifecycleOwner: LifecycleOwner,
        managerData: DownloadManagerData
    ) {
        this.managerData = managerData
        // 拥有生命周期管理
        managerData.eventDownloadManagerNotify.observe(lifecycleOwner, EventObserver {
            handleDownloadManagerNotify(it)
        })
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                lifecycleOwner.lifecycle.removeObserver(this)
                releaseManagerData()
            }
        })
    }

    private fun handleDownloadManagerNotify(notify: DownloadManagerNotify) {
        when (notify.type) {
            DownloadManagerNotifyType.Download -> download(notify.task, false)
            DownloadManagerNotifyType.UpdateNotification -> managerData.updateNotification(notify.task)
        }
    }

    /**
     * 下载文件列表
     */
    @Synchronized
    fun download(
        tasks: List<DownloadTask>
    ): Int = managerData.download(tasks)

    /**
     * 下载文件
     */
    @Synchronized
    fun download(
        task: DownloadTask
    ): Boolean = managerData.download(task)


    /**
     * 下载文件
     */
    @Synchronized
    private fun download(
        task: DownloadTask,
        isBreakpointRetry: Boolean
    ): Boolean {
        if (task.isRunning) return false
        // 开始下载
        var current = 0L
        managerData.httpClient.newCall(Request.Builder().apply {
            // 断点续传
            if (task.isBreakpointResume && !isBreakpointRetry && task.breakpoint > 0) {
                header("Range", "bytes=${task.breakpoint}-") // 设置断点续传
                current = task.breakpoint // 设置开始位置
            }
            // 设置下载路径
            url(task.url)
            // 自定义Request回调
            (task?.onCustomRequest ?: managerData.onCustomRequest)?.invoke(task, this)
        }.build()).apply {
            if (task.isRunning) return false
            // 更新任务状态为运行中
            task.onChangeStateToRunning(this)
            // 封装请求回调处理
            enqueue(object : Callback {
                // 上一次更新时间（用于与更新间隔做对比）
                private var lastProgressUpdateTime: Long = 0

                // 更新间隔
                private val updateInterval =
                    task.progressUpdateInterval ?: managerData.progressUpdateInterval

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
                                    task.saveFile?.deleteOnExit()//删除之前的文件
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
                            val buf = ByteArray(managerData.perByteCount)
                            updateProgress(total, current)
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
                            LOG("DownLoadTask 任务失败回调" + e.message)
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
                    if (task.isStop) return
                    // 更新状态
                    task.onChangeStateToSuccess()
                    // 回调
                    task.onTaskSuccess?.invoke(task, response)
                    managerData.onTaskSuccess?.invoke(task, response)
                    // 更新到通知栏
                    managerData.updateNotification(task)
                    managerData.onEventTaskUpdateByState(task)
                }

                //下载任务失败（含取消、暂停）回调
                private fun downloadFailure(e: Exception?, response: Response?) {
                    // 更新状态
                    task.onChangeStateToFailureIfNotStop()
                    // 回调
                    task.onTaskFailure?.invoke(task, e, response)
                    managerData.onTaskFailure?.invoke(task, e, response)
                    // 更新到通知栏
                    managerData.updateNotification(task)
                    managerData.onEventTaskUpdateByState(task)
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
                        managerData.onProgressUpdate?.invoke(task, this)
                    }
                    // 更新到通知栏
                    managerData.updateNotification(task)
                    managerData.onEventTaskUpdateByProgress(task)
                }


            })
        }
        return true
    }


    /**
     * 释放ManagerData
     */
    fun releaseManagerData() {
        eventDownloadManagerNotifyObserver?.run {
            managerData.eventDownloadManagerNotify.removeObserver(this)
            eventDownloadManagerNotifyObserver = null
        }
        managerData.cancelAllTask()
    }
}



