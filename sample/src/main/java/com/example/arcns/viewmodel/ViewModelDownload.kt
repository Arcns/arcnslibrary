package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arcns.core.file.cacheDirPath
import com.arcns.core.file.getCurrentTimeMillisFileName
import com.arcns.core.network.DownloadManagerData
import com.arcns.core.network.DownloadTask
import com.arcns.core.util.Event
import com.arcns.core.util.InjectSuperViewModel
import com.arcns.core.util.eventValue
import com.example.arcns.data.network.NetworkDataSource


class ViewModelDownload : ViewModel() {


    @InjectSuperViewModel
    lateinit var superViewModel:ViewModelActivityMain

    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    // 下载任务管理器数据
    val downloadManagerData = DownloadManagerData()

    // 任务点击
    private var _eventTaskClick = MutableLiveData<Event<DownloadTask>>()
    var eventTaskClick: LiveData<Event<DownloadTask>> = _eventTaskClick
    fun onEventTaskClick(task: DownloadTask) {
        _eventTaskClick.eventValue = task
    }

    // 添加任务
    fun addDownloadTask() {
        downloadManagerData.download(
            DownloadTask(
//                url = "https://codeload.github.com/afollestad/material-dialogs/zip/3.3.0",
//                url = "https://codeload.github.com/Arcns/arcnslibrary/tar.gz/0.1.20-3",
                url = "http://rtu.earth123.net:10008/uploads/apk/202007061123414530346.apk",
//                url = "https://dldir1.qq.com/weixin/android/weixin7016android1700_arm64.apk",
//            url = "https://6c0fee503ddb187fc6bd1ce48124b314.dd.cdntips.com/imtt.dd.qq.com/16891/apk/B63F493587B17E6AD41B8E6844E6CE99.apk?mkey=5f069da3b7ed4490&f=1806&cip=183.237.98.101&proto=https",
                saveDirPath = cacheDirPath,
                saveFileName = getCurrentTimeMillisFileName(),
                isBreakpointResume = false
            )
        )
    }

    fun clear(){
        downloadManagerData.cancelAllTask(isContainsStop = true)
    }


}