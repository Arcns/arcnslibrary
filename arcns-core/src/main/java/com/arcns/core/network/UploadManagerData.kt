package com.arcns.core.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.cancelNotification
import com.arcns.core.util.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.collections.forEachIndexed

class UploadManagerData(
    // OkHttpClient
    val httpClient: OkHttpClient = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
        .build(),
    // 每次上传的字节数
    var perByteCount: Int = 2048,
    // 上传任务成功回调
    var onTaskSuccess: OnTaskSuccess<UploadTask>? = null,
    //上传任务失败回调
    var onTaskFailure: OnTaskFailure<UploadTask>? = null,
    // 上传任务的文件成功回调
    var onUploadFileSuccess: OnUploadFileSuccess? = null,
    // 上传任务的文件失败回调
    var onUploadFileFailure: OnUploadFileFailure? = null,
    // 上传任务的文件进度回调
    var onProgressUpdate: OnUploadFileProgressUpdate? = null,
    // 上传进度更新时间间隔
    var progressUpdateInterval: Long = 1000,
    // 上传通知配置（禁用优先级高于任务配置，但内容优先级低于任务配置）
    var notificationOptions: NotificationOptions? = null,
    // 自定义MultipartBody回调
    var onCustomMultipartBody: ((UploadTask, MultipartBody.Builder) -> Unit)? = null,
    // 自定义请求（Request）回调，能够使用该回调对请求进行操作
    var onCustomRequest: ((UploadTask, Request.Builder) -> Unit)? = null,
    // 上传通道数量，0为无限制，若当前下载数量超过通道数量，则超过的任务会排队等待
    var lanes: Int = 3
) {
    val uniqueID: String = java.util.UUID.randomUUID().toString()

    // 任务列表
    private var _tasks =
        MutableLiveData<ArrayList<UploadTask>>().apply { fastValue = ArrayList() }
    val tasksLiveData: LiveData<ArrayList<UploadTask>> = _tasks
    val tasks: ArrayList<UploadTask> get() = _tasks.value ?: arrayListOf()

    // 任务状态更新事件
    private var _eventTaskStateUpdate = MutableLiveData<Event<UploadTask>>()
    var eventTaskStateUpdate: LiveData<Event<UploadTask>> = _eventTaskStateUpdate

    // 任务进度更新事件
    private var _eventTaskProgressUpdate = MutableLiveData<Event<UploadTaskFileParameterUpdate>>()
    var eventTaskProgressUpdate: LiveData<Event<UploadTaskFileParameterUpdate>> =
        _eventTaskProgressUpdate

    // 下发任务管理器操作指令
    private var _eventUploadManagerNotify = MutableLiveData<Event<UploadManagerNotify>>()
    var eventUploadManagerNotify: LiveData<Event<UploadManagerNotify>> =
        _eventUploadManagerNotify

    // 根据任务状态获取任务数量
    fun getTasksNumberOnState(state: TaskState): Int = getTasksOnState(state).count()

    // 根据任务状态获取任务
    fun getTasksOnState(state: TaskState): List<UploadTask> = tasks.filter {
        it.state == state
    }

    // 任务状态更新通知
    fun onEventTaskUpdateByState(task: UploadTask) {
        _tasks.fastValue = tasks
        _eventTaskStateUpdate.fastValue = Event(task)
        if (task.isStop && task.stopReason != NetworkTaskStopReason.HumanAll) {
            uploadWaitTasks()
        }
    }

    // 任务进度更新通知
    fun onEventTaskUpdateByProgress(update: UploadTaskFileParameterUpdate) {
        _tasks.fastValue = tasks
        _eventTaskProgressUpdate.fastValue = Event(update)
    }

    // 上传排队等待中的任务
    private fun uploadWaitTasks() {
        val quota = if (lanes < 0) null else lanes - tasks.count { it.isRunning }
        if (quota != null && quota <= 0) return
        var quantity = 0
        LOG("task uploadWaitTasks")
        tasks.forEach {
            if (it.isWait) {
                it.onChangeStateToNone()
                _eventUploadManagerNotify.fastEventValue =
                    UploadManagerNotify(UploadManagerNotifyType.Upload, it)
                onEventTaskUpdateByState(it)
                quantity++
            }
            if (quota != null && quantity >= quota) {
                return
            }
        }
    }

    /**
     * 上传文件列表
     */
    @Synchronized
    fun upload(
        tasks: List<UploadTask>
    ): Int = tasks.filter {
        upload(it)
    }.size

    /**
     * 上传文件
     */
    @Synchronized
    fun upload(task: UploadTask): Boolean {
        if (!addTask(task)) return false
        if (lanes <= 0 || tasks.count { it.isRunning } < lanes) {
            // 下发任务管理器上传指令
            _eventUploadManagerNotify.fastEventValue =
                UploadManagerNotify(UploadManagerNotifyType.Upload, task)
        } else {
            // 若当前上传数量超过通道数量，则超过的任务会排队等待
            task.onChangeStateToWait()
            _eventUploadManagerNotify.fastEventValue =
                UploadManagerNotify(UploadManagerNotifyType.UpdateNotification, task)
        }
        onEventTaskUpdateByState(task)
        return true
    }


    /**
     * 添加任务到任务列表
     */
    private fun addTask(task: UploadTask): Boolean {
        val taskUploadFilePaths =
            task.parameters.filter { it is UploadTaskFileParameter && !it.uploadFilePath.isNullOrBlank() }
                .map { (it as UploadTaskFileParameter).uploadFilePath }
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
            if (!it.isStop) {
                it.parameters.forEach {
                    if (it is UploadTaskFileParameter && taskUploadFilePaths.contains(it.uploadFilePath)) {
                        LOG("task 任务保存路径正在被使用 " + it.uploadFilePath)
                        return false // 上传的文件已被占用
                    }
                }
            }
        }
        tasks.add(task)
        return true
    }


    /**
     * 删除任务
     */
    fun removeTask(
        task: UploadTask?,
        isCancelTask: Boolean = true,
        cancelReason: NetworkTaskStopReason = NetworkTaskStopReason.Human,
        isClearNotification: Boolean = true
    ): Boolean {
        if (task != null && tasks.contains(task)) {
            if (isClearNotification) {
                task.notificationOptions = NotificationOptions.DISABLE
                if (task.isStop) task.cancelNotification()
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
                it.notificationOptions = NotificationOptions.DISABLE
                if (isContainsStop && it.isStop) {
                    it.cancelNotification()
                }
            }
            if (isContainsStop || !it.isStop) cancel(it, cancelReason)
        }
    }


    /**
     * 停止任务
     */
    fun stop(
        task: UploadTask,
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
            _eventUploadManagerNotify.fastEventValue =
                UploadManagerNotify(UploadManagerNotifyType.UpdateNotification, task)
            onEventTaskUpdateByState(task)
        }
    }

    /**
     * 更新通知栏
     */
    fun updateNotification(task: UploadTask) =
        task.updateNotification(notificationOptions)

    /**
     * 取消任务
     */
    fun cancel(task: UploadTask, reason: NetworkTaskStopReason = NetworkTaskStopReason.Human) =
        stop(task, TaskState.Cancel, reason)

    /**
     * 暂停任务
     */
    fun pause(task: UploadTask, reason: NetworkTaskStopReason = NetworkTaskStopReason.Human) =
        stop(task, TaskState.Pause, reason)

    /**
     * 手动使任务错误失败
     */
    fun failure(task: UploadTask, reason: NetworkTaskStopReason = NetworkTaskStopReason.Human) =
        stop(task, TaskState.Failure, reason)

    /**
     * 根据url查找任务
     */
    fun findTaskByUrl(url: String): UploadTask? = tasks.firstOrNull { it.url == url }

    /**
     * 根据id查找任务
     */
    fun findTaskByID(id: String): UploadTask? = tasks.firstOrNull { it.id == id }

    /**
     * 是否包含指定任务
     */
    fun containsTask(task: UploadTask): Boolean = tasks.contains(task)
}

// 下发任务管理器的操作指令类型
enum class UploadManagerNotifyType {
    Upload, UpdateNotification
}

// 下发任务管理器的操作指令
data class UploadManagerNotify(
    val type: UploadManagerNotifyType,
    val task: UploadTask
)