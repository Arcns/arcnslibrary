package com.arcns.core.map

/**
 * 通用地图轨迹记录器（请在ViewModel中创建）
 */
class MapTrackRecorder {
    var enabled: Boolean = true
    var millisecondTimer: Long = 1000
    val trackData = MapPositionGroup()
}