package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arcns.core.file.cacheDirPath
import com.arcns.core.file.getCurrentTimeMillisFileName
import com.arcns.core.network.DownLoadManagerData
import com.arcns.core.network.DownLoadTask
import com.arcns.core.util.Event
import com.example.arcns.data.network.NetworkDataSource


class ViewModelDownload : ViewModel() {


    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    // 下载任务管理器数据
    val downloadManagerData = DownLoadManagerData()

    // 任务点击
    private var _eventTaskClick = MutableLiveData<Event<String>>()
    var eventTaskClick: LiveData<Event<String>> = _eventTaskClick

    // 添加任务
    fun addDownloadTask() {
        downloadManagerData.addTask(
            DownLoadTask(
                url = "https://dldir1.qq.com/weixin/android/weixin7016android1700_arm64.apk",
//            url = "https://6c0fee503ddb187fc6bd1ce48124b314.dd.cdntips.com/imtt.dd.qq.com/16891/apk/B63F493587B17E6AD41B8E6844E6CE99.apk?mkey=5f069da3b7ed4490&f=1806&cip=183.237.98.101&proto=https",
                saveDirPath = cacheDirPath,
                saveFileName = getCurrentTimeMillisFileName(".apk"),
                isBreakpointResume = false
            )
        )
    }


}