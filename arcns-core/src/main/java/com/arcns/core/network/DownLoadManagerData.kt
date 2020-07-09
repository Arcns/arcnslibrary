package com.arcns.core.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.map.MapPosition
import com.arcns.core.util.Event
import com.arcns.core.util.fastValue
import java.util.*

class DownLoadManagerData {
    val uniqueID: String = UUID.randomUUID().toString()

    private var _tasks =
        MutableLiveData<ArrayList<DownLoadTask>>().apply { fastValue = ArrayList() }
    val tasksLiveData: LiveData<ArrayList<DownLoadTask>> = _tasks
    val tasks: ArrayList<DownLoadTask> get() = _tasks.value ?: arrayListOf()

    private var _eventNotifyAddTask = MutableLiveData<Event<DownLoadTask>>()
    var eventNotifyAddTask:LiveData<Event<DownLoadTask>> = _eventNotifyAddTask
    fun addTask(task: DownLoadTask): DownLoadTask? {
        if (tasks.contains(task)) return null
        tasks.add(task)
        _tasks.value = tasks
        _eventNotifyAddTask.value = Event(task)
        return task
    }
}