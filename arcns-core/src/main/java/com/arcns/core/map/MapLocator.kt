package com.arcns.core.map

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

/**
 * 通用地图定位器
 */
abstract class MapLocator(
    context: Context
) {
    // 定时器总计时
    private var totalTimer: Long = 0

    // 轨迹记录器
    val trackRecorders = ArrayList<MapTrackRecorder>()

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
                    this@MapLocator.onDestroy()
                }
            })
        }


    /**
     * 添加轨迹记录器
     */
    open fun addTrackRecorder(recorder: MapTrackRecorder) {
        trackRecorders.add(recorder)
    }

    /**
     * 删除轨迹记录器
     */
    open fun removeTrackRecorder(recorder: MapTrackRecorder) {
        trackRecorders.remove(recorder)
    }

    /**
     * 每秒回调器
     */
    open fun onPerSecondCallback(position: MapPosition) {
        onLocationChanged?.invoke(position)
        trackRecorders.forEach {
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