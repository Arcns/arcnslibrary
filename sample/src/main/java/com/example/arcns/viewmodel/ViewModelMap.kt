package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amap.api.maps.model.MarkerOptions
import com.arcns.core.util.*
import com.example.arcns.data.network.NetworkDataSource


class ViewModelMap : MapViewManagerViewModel() {


    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    // 测量
    var calculateLineValue = MutableLiveData<String>()
    var calculateLineMapPositionGroup = MapPositionGroup().apply {
        applyCustomOptions = {
            when (it) {
                is MarkerOptions -> it.draggable(true)
            }
        }
    }
    var calculateAreaValue = MutableLiveData<String>()
    var calculateAreaMapPositionGroup = MapPositionGroup().apply {
        applyCustomOptions = {
            when (it) {
                is MarkerOptions -> it.draggable(true)
            }
        }
    }
}
