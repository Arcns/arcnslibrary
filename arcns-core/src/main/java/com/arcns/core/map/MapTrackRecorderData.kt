package com.arcns.core.map

/**
 * 通用地图轨迹记录器的数据
 */
class MapTrackRecorderData {
    var enabled: Boolean = true
    var millisecondTimer: Long = 1000
    val trackData = MapPositionGroup()
}