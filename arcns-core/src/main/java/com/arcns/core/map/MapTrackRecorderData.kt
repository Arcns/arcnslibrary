package com.arcns.core.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * 通用地图轨迹记录器的数据
 */
class MapTrackRecorderData {
    var Enabled: Boolean = true
    var millisecondTimer: Long = 1000
    val trackData = MapPositionGroup()
}