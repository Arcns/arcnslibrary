package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amap.api.maps.model.MarkerOptions
import com.arcns.core.map.MapPositionGroup
import com.arcns.core.map.MapViewManagerData
import com.arcns.core.util.*
import com.example.arcns.data.network.NetworkDataSource


class ViewModelMap : ViewModel() {


    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    // 通用地图管理器的数据
    var mapViewManagerData = MapViewManagerData()

    // 测量
    var calculateLineValue = MutableLiveData<String>()
    var calculateLineMapPositionGroup = MapPositionGroup().apply {
        applyCustomOptions = { group, options ->
            when (options) {
                is MarkerOptions -> options.draggable(true)
            }
        }
    }
    var calculateAreaValue = MutableLiveData<String>()
    var calculateAreaMapPositionGroup = MapPositionGroup().apply {
        applyCustomOptions = { group, options ->
            when (options) {
                is MarkerOptions -> options.draggable(true)
            }
        }
    }

    // 行政区域坐标组列表
    var districtMapPositions = ArrayList<MapPositionGroup>()
}
