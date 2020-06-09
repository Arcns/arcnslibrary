package com.arcns.core.map

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.util.Event
import com.arcns.core.util.LOG

/**
 * 通用地图轨迹记录器（请在ViewModel中创建）
 */
class MapTrackRecorder {
    /**
     * 状态
     */
    var enabled: Boolean = true

    /**
     * 记录间隔（毫秒）
     */
    var recorderInterval: Long = 1000

    /**
     * 轨迹坐标数据
     */
    val trackData = MapPositionGroup()

    /**
     * 相邻时只添加一个相同的坐标
     */
    var isAddOneSameTrackPositionWhenAdjacent = true

    /**
     * 定义为相同的坐标的距离（m）
     */
    var sameTrackPositionDistanceBetween = 5f

    /**
     * 轨迹坐标数据更新回调
     */
    private var _eventTrackDataUpdate = MutableLiveData<Event<Unit>>()
    var eventTrackDataUpdate: LiveData<Event<Unit>> = _eventTrackDataUpdate

    /**
     * 添加轨迹坐标
     */
    fun addTrackPosiition(position: MapPosition) {
        // 只记录一个邻近的相同坐标
        if (isAddOneSameTrackPositionWhenAdjacent
        ) {
            val last = trackData.mapPositions.lastOrNull()
            if (last != null) {
                if (last.latitude == position.latitude && last.longitude == position.longitude) {
                    // 相同的坐标避免重复记录
                    return
                }
                if (sameTrackPositionDistanceBetween > 0f && last.distanceBetween(position) < sameTrackPositionDistanceBetween) {
                    // 避免添加距离过近的点
                    return
                }
                LOG("添加坐标距离：" + last.distanceBetween(position))
            }
        }
        LOG("添加坐标：" + position.latitude + "  " + position.latitude)
        trackData.addMapPosition(position)
        _eventTrackDataUpdate.value = Event(Unit)
    }

    /**
     * 添加轨迹坐标（根据记录间隔）
     */
    fun addTrackPositionOnRecorderInterval(
        position: MapPosition,
        totalTime: Long,
        locatorInterval: Long
    ) {
        if (locatorInterval >= recorderInterval) {
            addTrackPosiition(position)
        } else if (totalTime % recorderInterval == 0L && enabled) {
            addTrackPosiition(position)
        }
    }
}