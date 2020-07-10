package com.arcns.core.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.cancelNotification
import com.arcns.core.util.Event
import com.arcns.core.util.fastValue
import com.arcns.core.util.reverseForEach
import java.util.*
import kotlin.collections.ArrayList

class DownloadManagerData {
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
    }

    fun onEventTaskUpdateByProgress(task: DownloadTask) {
        _tasks.fastValue = tasks
        _eventTaskUpdate.fastValue = Event(task)
    }

    // 添加任务
    private var _eventNotifyManagerDownload = MutableLiveData<Event<DownloadTask>>()
    var eventNotifyManagerDownload: LiveData<Event<DownloadTask>> = _eventNotifyManagerDownload
    fun download(task: DownloadTask): Boolean {
        if (!addTask(task)) return false
        _eventNotifyManagerDownload.fastValue = Event(task)
        onEventTaskUpdateByState(task)
        return true
    }

    private fun addTask(task:DownloadTask):Boolean{
        tasks.forEachIndexed { index, it ->
            // 任务已存在
            if (it == task || it.id == task.id) {
                if (it.isStop) {
                    tasks.removeAt(index)
                    tasks.add(index, task)
                    return true
                }
                return false
            }
            // 任务保存路径正在被使用
            if (!it.isStop && it.saveFilePath == task.saveFilePath) {
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
        isClearNotification: Boolean = true
    ): Boolean {
        if (task != null && tasks.contains(task)) {
            if (isClearNotification) {
                if (task.isStop) task.notificationID.cancelNotification()
                else task.notificationOptions = NotificationOptions.DISABLE
            }
            tasks.remove(task)
            if (isCancelTask && task.isRunning) task.cancel()
//            onEventTaskUpdateByState(task)
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
    fun findTaskByUrl(url: String): DownloadTask? = tasks.firstOrNull { it.url == url }

    /**
     * 根据id查找任务
     */
    fun findTaskByID(id: String): DownloadTask? = tasks.firstOrNull { it.id == id }

    /**
     * 是否包含指定任务
     */
    fun containsTask(task:DownloadTask):Boolean = tasks.contains(task)
}