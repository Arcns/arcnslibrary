package com.arcns.core.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arcns.core.map.MapPosition
import com.arcns.core.util.Event

/**
 * 通用地图管理器的数据，请在ViewModel中创建
 */
class MapViewManagerData {
    // 是否首次加载（用于兼容Navigation）
    private var _isfirstLoad = MutableLiveData<Boolean>()
    val isfirstLoad: Boolean get() = _isfirstLoad.value ?: true
    fun onFirstLoadComplete() {
        _isfirstLoad.value = false
    }

    /**
     * 自定义的更新回调
     */
    private var _eventOnUpdate = MutableLiveData<Event<Any>>()
    var eventOnUpdate: LiveData<Event<Any>> = _eventOnUpdate
    fun onUpdate(data: Any? = null) {
        _eventOnUpdate.value = Event(data ?: Any())
    }

    // 暂停时的地图场景位置
    private var _cameraPositionTarget = MutableLiveData<MapPosition>()
    val cameraPositionTarget get() = _cameraPositionTarget.value

    // 暂停时的地图场景缩放级别
    private var _cameraPositionZoom = MutableLiveData<Float>()
    val cameraPositionZoom get() = _cameraPositionZoom.value

    // 暂停时的地图场景俯仰角0°~45°（垂直与地图时为0）
    private var _cameraPositionTilt = MutableLiveData<Float>()
    val cameraPositionTilt get() = _cameraPositionTilt.value

    // 暂停时的地图场景偏航角 0~360° (正北方为0)
    private var _cameraPositionBearing = MutableLiveData<Float>()
    val cameraPositionBearing get() = _cameraPositionBearing.value

    /**
     * 保存暂停时的地图场景相关数据
     */
    fun savePauseCameraPosition(
        mapPosition: MapPosition,
        zoom: Float,
        tilt: Float,
        bearing: Float
    ) {
        _cameraPositionTarget.value = mapPosition
        _cameraPositionZoom.value = zoom
        _cameraPositionTilt.value = tilt
        _cameraPositionBearing.value = bearing
    }
}