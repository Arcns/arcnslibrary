package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arcns.core.util.Event
import com.example.arcns.data.network.NetworkDataSource


class ViewModelMap:ViewModel() {


    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()
    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng
    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

}