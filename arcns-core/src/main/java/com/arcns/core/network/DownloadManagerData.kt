package com.arcns.core.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.cancelNotification
import com.arcns.core.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.forEachIndexed

class DownloadManagerData(
    // OkHttpClient
    val httpClient: OkHttpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .build(),
    // 每次下载的字节数
    var perByteCount: Int = 2048,
    // 下载任务成功回调
    var onTaskSuccess: OnTaskSuccess<DownloadTask>? = null,
    //下载任务失败回调
    var onTaskFailure: OnTaskFailure<DownloadTask>? = null,
    // 下载任务的文件进度回调
    var onProgressUpdate: OnDownloadProgressUpdate? = null,
    // 下载时间间隔
    var progressUpdateInterval: Long = 1000,
    // 上传通知配置（内容优先级低于任务配置）
    var notificationOptions: NotificationOptions? = null,
    // 自定义Request回调
    var onCustomRequest: ((DownloadTask, Request.Builder) -> Unit)? = null,
    // 下载通道数量，0为无限制
    var lanes: Int = 3
) {
    val uniqueID: String = UUID.randomUUID().toString()

    // 任务列表
    private var _tasks =
        MutableLiveData<ArrayList<DownloadTask>>().apply { fastValue = ArrayList() }
    val tasksLiveData: LiveData<ArrayList<DownloadTask>> = _tasks
    val tasks: ArrayList<DownloadTask> get() = _tasks.value ?: arrayListOf()

    // 任务更新事件
    private var _eventTaskUpdate = MutableLiveData<Event<DownloadTask>>()
    var eventTaskUpdate: LiveData<Event<DownloadTask>> = _eventTaskUpdate
    fun onEventTaskUpdateByState(task: DownloadTask) {
        _tasks.fastValue = tasks
        _eventTaskUpdate.fastValue = Event(task)
        if (task.isStop && task.stopReason != NetworkTaskStopReason.HumanAll) {
            downloadWaitTasks()
        }
    }

    private fun downloadWaitTasks() {
        val quota = if (lanes < 0) null else lanes - tasks.count { it.isRunning }
        if (quota != null && quota <= 0) return
        var quantity = 0
        LOG("task downloadWaitTasks")
        tasks.forEach {
            if (it.isWait) {
                it.onChangeStateToNone()
                _eventDownloadManagerNotify.fastEventValue =
                    DownloadManagerNotify(DownloadManagerNotifyType.Download, it)
                onEventTaskUpdateByState(it)
                quantity++
            }
            if (quota != null && quantity >= quota) {
                return
            }
        }
    }

    fun onEventTaskUpdateByProgress(task: DownloadTask) {
        _tasks.fastValue = tasks
        _eventTaskUpdate.fastValue = Event(task)
    }

    // 添加任务
    private var _eventDownloadManagerNotify = MutableLiveData<Event<DownloadManagerNotify>>()
    var eventDownloadManagerNotify: LiveData<Event<DownloadManagerNotify>> =
        _eventDownloadManagerNotify

    @Synchronized
    fun download(task: DownloadTask): Boolean {
        if (!addTask(task)) return false
        if (lanes <= 0 || tasks.count { it.isRunning } < lanes) {
            _eventDownloadManagerNotify.fastEventValue =
                DownloadManagerNotify(DownloadManagerNotifyType.Download, task)
        } else {
            task.onChangeStateToWait()
        }
        onEventTaskUpdateByState(task)
        return true
    }

    private fun addTask(task: DownloadTask): Boolean {
        tasks.forEachIndexed { index, it ->
            // 任务已存在
            if (it == task || it.id == task.id) {
                if (it.isStop) {
                    tasks.removeAt(index)
                    tasks.add(index, task)
                    return true
                }
                LOG("task 任务已存在 ")
                return false
            }
            // 任务保存路径正在被使用
            if (!it.isStop && (task.saveFilePath != null && it.saveFilePath == task.saveFilePath)) {
                LOG("task 任务保存路径正在被使用 " + it.saveFilePath)
                return false
            }
        }
        tasks.add(task)
        return true
    }

    // 删除任务
    fun removeTask(
        task: DownloadTask?,
        isCancelTask: Boolean = true,
        cancelReason: NetworkTaskStopReason = NetworkTaskStopReason.Human,
        isClearNotification: Boolean = true
    ): Boolean {
        if (task != null && tasks.contains(task)) {
            if (isClearNotification) {
                if (task.isStop) task.notificationID.cancelNotification()
                else task.notificationOptions = NotificationOptions.DISABLE
            }
            tasks.remove(task)
            if (isCancelTask && !task.isStop) cancel(task, cancelReason)
//            onEventTaskUpdateByState(task)
            return true
        }
        return false
    }

    /**
     * 清空任务
     */
    fun clearTask(
        isCancelTask: Boolean = true,
        cancelReason: NetworkTaskStopReason = NetworkTaskStopReason.HumanAll,
        isClearNotification: Boolean = true
    ) {
        tasks.reverseForEach {
            removeTask(it, isCancelTask, cancelReason, isClearNotification)
        }
    }

    /**
     * 取消所有任务
     */
    fun cancelAllTask(
        isClearNotification: Boolean = true,
        isContainsStop: Boolean = false,
        cancelReason: NetworkTaskStopReason = NetworkTaskStopReason.HumanAll
    ) {
        tasks.forEach {
            if (isClearNotification) {
                if (it.isStop) it.notificationID.cancelNotification()
                else it.notificationOptions = NotificationOptions.DISABLE
            }
            if (isContainsStop || !it.isStop) cancel(it, cancelReason)
        }
    }

    /**
     * 停止任务
     */
    fun stop(
        task: DownloadTask,
        state: TaskState,
        reason: NetworkTaskStopReason = NetworkTaskStopReason.Human
    ) {
        if (task.isRunning) {
            LOG("task ${task.id} stop isRunning")
            task.stop(state, reason)
        } else {
            LOG("task ${task.id} stop forceStop")
            task.forceStop(state, reason)
            task.onTaskFailure?.invoke(task, null, null)
            onTaskFailure?.invoke(task, null, null)
            // 更新到通知栏
            _eventDownloadManagerNotify.fastEventValue =
                DownloadManagerNotify(DownloadManagerNotifyType.UpdateNotification, task)
            onEventTaskUpdateByState(task)
        }
    }

    /**
     * 取消任务
     */
    fun cancel(task: DownloadTask, reason: NetworkTaskStopReason = NetworkTaskStopReason.Human) =
        stop(task, TaskState.Cancel, reason)

    /**
     * 暂停任务
     */
    fun pause(task: DownloadTask, reason: NetworkTaskStopReason = NetworkTaskStopReason.Human) =
        stop(task, TaskState.Pause, reason)

    /**
     * 手动使任务错误失败
     */
    fun failure(task: DownloadTask, reason: NetworkTaskStopReason = NetworkTaskStopReason.Human) =
        stop(task, TaskState.Failure, reason)

    /**
     * 根据url查找任务
     */
    fun findTaskByUrl(url: String): DownloadTask? = tasks.firstOrNull { it.url == url }

    /**
     * 根据id查找任务
     */
    fun findTaskByID(id: String): DownloadTask? = tasks.firstOrNull { it.id == id }

    /**
     * 是否包含指定任务
     */
    fun containsTask(task: DownloadTask): Boolean = tasks.contains(task)
}

enum class DownloadManagerNotifyType {
    Download, UpdateNotification
}

data class DownloadManagerNotify(
    val type: DownloadManagerNotifyType,
    val task: DownloadTask
)