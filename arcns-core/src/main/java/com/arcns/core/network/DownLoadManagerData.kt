package com.arcns.core.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.arcns.core.util.Event
import com.arcns.core.util.fastValue
import java.util.*

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
    private var _eventNotifyManagerAddTask = MutableLiveData<Event<DownLoadTask>>()
    var eventNotifyManagerAddTask: LiveData<Event<DownLoadTask>> = _eventNotifyManagerAddTask
    fun addTask(task: DownLoadTask): Boolean {
        if (!tasks.chackAddDownloadTask(task)) return false
        tasks.add(task)
        _eventNotifyManagerAddTask.fastValue = Event(task)
        onEventTaskUpdateByState(task)
        return true
    }

    // 删除任务
    fun removeTask(task: DownLoadTask?, isCancelTask: Boolean = true) {
        if (tasks.contains(task)) {
            if (isCancelTask && task?.isRunning == true) task.cancel()
            tasks.remove(task)
            onEventTaskUpdateByState(task ?: return)
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


@Synchronized
fun List<DownLoadTask>.chackAddDownloadTask(addTask: DownLoadTask): Boolean {
    if (contains(addTask)) return false  // 不能重复添加任务
    forEach {
        if (it.id == addTask.id) {
            return false // 任务id已存在
        }
        if (!it.isStop && it.saveFilePath == addTask.saveFilePath) {
            return false // 任务保存路径正在被使用
        }
    }
    return true
}