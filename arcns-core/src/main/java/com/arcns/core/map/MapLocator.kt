package com.arcns.core.map

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.arcns.core.APP
import com.arcns.core.R
import com.arcns.core.app.ForegroundService
import com.arcns.core.app.ForegroundServiceBinder
import com.arcns.core.app.ForegroundServiceConnection
import com.arcns.core.app.ForegroundServiceOptions
import com.arcns.core.util.string

/**
 * 通用地图定位器
 */
abstract class MapLocator(
    context: Context,
    var isOnlyForegroundLocator: Boolean // 是否为前台定位器，若是则处于后台时定位器将自动关闭
) {
    // 定位器总计时
    private var locatorTotalTime: Long = 0

    // 暂停时定位器是否开启
    private var isStartedWhenOnPause = false

    // 轨迹记录器
    val trackRecorders = ArrayList<MapTrackRecorder>()

    // 自定义定位回调
    var onLocationChanged: ((MapPosition) -> Unit)? = null

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

                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                fun onResume() {
                    if (isOnlyForegroundLocator && isStartedWhenOnPause) {
                        start()
                    }
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                fun onPause() {
                    if (isOnlyForegroundLocator) {
                        isStartedWhenOnPause = isStarted()
                        stop()
                    }
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    this@MapLocator.onDestroy()
                    lifecycleOwner?.lifecycle?.removeObserver(this)
                }
            })
        }

    /**
     * 返回定时器间隔
     */
    abstract fun getLocatorInterval(): Long

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
     * 地图定时回调器
     */
    open fun onLocatorLocationCallback(position: MapPosition) {
        onLocationChanged?.invoke(position)
        trackRecorders.forEach {
            it.addTrackPositionOnRecorderInterval(position, locatorTotalTime, getLocatorInterval())
        }
        locatorTotalTime += getLocatorInterval()
    }

    /**
     * 定位器是否开启
     */
    abstract fun isStarted(): Boolean

    /**
     * 停止
     */
    open fun stop() {
        locatorTotalTime = 0
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

/**
 * 设置地图定位器前台服务默认设置
 */
fun <T : MapLocator> setMapLocatorServiceDefaultOptions(options: ForegroundServiceOptions<T>) =
    ForegroundService.setDefaultOptions(MapLocator::class, options.apply {
        if (onDestroyServiceContent == null) {
            onDestroyServiceContent = {
                it?.onDestroy()
            }
        }
        if (notificationOptions != null && notificationOptions?.channelName.equals(R.string.text_foreground_service_notification_default_channel_name.string)) {
            notificationOptions?.channelName =
                R.string.text_map_locator_service_notification_channel_name.string
        }
    })

/**
 * 开启地图定位器前台服务
 */
fun startMapLocatorService(
    serviceConnection: ForegroundServiceConnection<MapLocator>? = null,
    options: ForegroundServiceOptions<MapLocator>? = ForegroundService.getDefaultOptions(
        MapLocator::class
    )
) = ForegroundService.startService(serviceConnection, options)
