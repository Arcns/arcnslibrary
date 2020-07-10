package com.arcns.core.network

import android.app.Notification
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.cancelNotification
import com.arcns.core.util.Event
import com.arcns.core.util.fastValue
import com.arcns.core.util.reverseForEach
import java.util.*
import kotlin.collections.ArrayList

class DownLoadManagerData {
    val uniqueID: String = UUID.randomUUID().toString()

    // 任务列表
    private var _tasks =
        MutableLiveData<ArrayList<DownLoadTask>>().apply { fastValue = ArrayList() }
    val tasksLiveData: LiveData<ArrayList<DownLoadTask>> = _tasks
    val tasks: ArrayList<DownLoadTask> get() = _tasks.value ?: arrayListOf()

    // 任务更新事件
    private var _eventTaskUpdate = MutableLiveData<Event<DownLoadTask>>()
    var eventTaskUpdate: LiveData<Event<DownLoadTask>> = _eventTaskUpdate
    fun onEventTaskUpdateByState(task: DownLoadTask) {
        _tasks.fastValue = tasks
        _eventTaskUpdate.fastValue = Event(task)
    }

    fun onEventTaskUpdateByProgress(task: DownLoadTask) {
        _tasks.fastValue = tasks
        _eventTaskUpdate.fastValue = Event(task)
    }

    // 添加任务
    private var _eventNotifyManagerDownload = MutableLiveData<Event<DownLoadTask>>()
    var eventNotifyManagerDownload: LiveData<Event<DownLoadTask>> = _eventNotifyManagerDownload
    fun download(task: DownLoadTask): Boolean {
        if (!tasks.addDownloadTask(task)) return false
        _eventNotifyManagerDownload.fastValue = Event(task)
        onEventTaskUpdateByState(task)
        return true
    }

    // 删除任务
    fun removeTask(
        task: DownLoadTask?,
        isCancelTask: Boolean = true,
        isClearNotification: Boolean = true
    ): Boolean {
        if (task != null && tasks.contains(task)) {
            if (isClearNotification) {
                if (task.isStop) task.notificationID.cancelNotification()
                else task.notificationOptions = NotificationOptions.DISABLE
            }
            if (isCancelTask && task.isRunning) task.cancel()
            tasks.remove(task)
            onEventTaskUpdateByState(task)
            return true
        }
        return false
    }

    /**
     * 清空任务
     */
    fun clearTask(isCancelTask: Boolean = true, isClearNotification: Boolean = true) {
        tasks.reverseForEach {
            removeTask(it, isCancelTask, isClearNotification)
        }
    }

    /**
     * 取消所有任务
     */
    fun cancelAllTask(isClearNotification: Boolean = true) {
        tasks.forEach {
            if (isClearNotification) {
                if (it.isStop) it.notificationID.cancelNotification()
                else it.notificationOptions = NotificationOptions.DISABLE
            }
            if (it.isRunning) it.cancel()
        }
    }

    /**
     * 根据url查找任务
     */
    fun findTaskByUrl(url: String): DownLoadTask? = tasks.firstOrNull { it.url == url }

    /**
     * 根据id查找任务
     */
    fun findTaskByID(id: String): DownLoadTask? = tasks.firstOrNull { it.id == id }
}

/**
 * 查找合适的位置
 */
fun ArrayList<DownLoadTask>.findDownloadTaskAppropriateIndex(addTask: DownLoadTask): Int? {
    forEachIndexed { index, it ->
        // 任务已存在
        if (it == addTask || it.id == addTask.id) {
            if (it.isStop) {
                return index
            }
            return null
        }
        // 任务保存路径正在被使用
        if (!it.isStop && it.saveFilePath == addTask.saveFilePath) {
            return null
        }
    }
    return -1
}

/**
 * 添加任务
 */
@Synchronized
fun ArrayList<DownLoadTask>.addDownloadTask(addTask: DownLoadTask): Boolean {
    findDownloadTaskAppropriateIndex(addTask)?.run {
        if (this == -1) {
            add(addTask)
        } else {
            removeAt(this)
            add(this, addTask)
        }
        return true
    }
    return false
}
