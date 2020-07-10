package com.arcns.core.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.util.Event

/**
 * 通用地图管理器的数据，请在ViewModel中创建
 */
class MapViewManagerData() {
    // 是否首次加载（用于兼容Navigation）
    var isFirstLoad = true
        private set

    fun onFirstLoadComplete() {
        isFirstLoad = false
    }

    /**
     * 自定义的更新回调
     */
    private var _eventOnUpdate = MutableLiveData<Event<Any>>()
    var eventOnUpdate: LiveData<Event<Any>> = _eventOnUpdate
    fun onUpdate(data: Any? = null) {
        _eventOnUpdate.value = Event(data ?: Any())
    }

    // 页面关闭时的地图场景位置
    var destroyCameraTarget: MapPosition? = null
        private set

    // 页面关闭时的地图场景缩放级别
    var destroyCameraZoom: Float? = null
        private set

    // 页面关闭时的地图场景俯仰角0°~45°（垂直与地图时为0）
    var destroyCameraTilt: Float? = null
        private set

    // 页面关闭时的地图场景偏航角 0~360° (正北方为0)
    var destroyCameraBearing: Float? = null
        private set

    // 是否拥有未消费的页面关闭时保存的数据
    var hasUnconsumedDestroyCamera: Boolean = false
        private set

    /**
     * 保存页面关闭时的地图场景相关数据
     */
    fun saveDestroyCamera(
        mapPosition: MapPosition,
        zoom: Float,
        tilt: Float,
        bearing: Float
    ) {
        destroyCameraTarget = mapPosition
        destroyCameraZoom = zoom
        destroyCameraTilt = tilt
        destroyCameraBearing = bearing
        hasUnconsumedDestroyCamera = true
    }

    /**
     * 已消费页面关闭时保存的地图场景相关数据
     */
    fun onConsumedDestroyCamera() {
        hasUnconsumedDestroyCamera = false
    }
}