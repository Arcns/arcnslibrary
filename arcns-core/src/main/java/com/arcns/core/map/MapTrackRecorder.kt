package com.arcns.core.map

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

/**
 * 通用地图轨迹记录器
 */
abstract class MapTrackRecorder(
    context: Context
) {
    // 定时器总计时
    private var totalTimer: Long = 0

    // 定时器数据记录器
    val dataList = ArrayList<MapTrackRecorderData>()

    // 自定义定位回调
    val onLocationChanged: ((MapPosition) -> Unit)? = null

    // 生命周期感知
    private var lifecycleObserver: LifecycleObserver? = null
    var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            lifecycleObserver?.run {
                lifecycleOwner?.lifecycle?.removeObserver(this)
            }
            field = value
            field?.lifecycle?.addObserver(object : LifecycleObserver {
                init {
                    lifecycleObserver = this
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    lifecycleOwner?.lifecycle?.removeObserver(this)
                    this@MapTrackRecorder.onDestroy()
                }
            })
        }


    /**
     * 添加记录器
     */
    open fun add(data: MapTrackRecorderData) {
        dataList.add(data)
    }

    /**
     * 删除记录器
     */
    open fun remove(data: MapTrackRecorderData) {
        dataList.remove(data)
    }

    /**
     * 每秒回调器
     */
    open fun onPerSecondCallback(position: MapPosition) {
        dataList.forEach {
            if (totalTimer % it.millisecondTimer == 0L && it.enabled) {
                it.trackData.addMapPosition(position)
            }
        }
        totalTimer++
    }

    /**
     * 停止
     */
    open fun stop() {
        totalTimer = 0
    }

    /**
     * 开始
     */
    abstract fun start()

    /**
     * 资源回收
     */
    abstract fun onDestroy()
}