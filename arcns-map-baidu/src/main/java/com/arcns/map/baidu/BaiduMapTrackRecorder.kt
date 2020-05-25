package com.arcns.map.baidu

import android.content.Context
import com.arcns.core.map.MapPosition
import com.arcns.core.map.MapPositionType
import com.arcns.core.map.MapTrackRecorder
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption

/**
 * 百度地图轨迹记录器
 */
class BaiduMapTrackRecorder(context: Context) : MapTrackRecorder(context) {

    val locationClient = LocationClient(context).apply {
        // 定位配置
        locOption = LocationClientOption().apply {
            openGps = true
            coorType = "bd09ll"
            scanSpan = 1000
            setIsNeedAddress(true)
        }
    }
    private var locationListener: BDAbstractLocationListener? = null

    init {
        // 定位监听
        locationClient.registerLocationListener(object : BDAbstractLocationListener() {
            init {
                locationListener = this
            }

            override fun onReceiveLocation(location: BDLocation?) {
                location?.run {
                    onPerSecondCallback(
                        MapPosition(
                            latitude = latitude,
                            longitude = longitude,
                            type = MapPositionType.GCJ02,
                            extraData = this
                        )
                    )
                }
            }
        })
    }

    /**
     * 停止
     */
    override fun stop() {
        super.stop()
        locationClient.stop()
    }

    /**
     * 开始
     */
    override fun start() {
        if (!locationClient.isStarted)
            locationClient.start()
    }


    /**
     * 资源回收
     */
    override fun onDestroy() {
        if (locationListener != null) {
            locationClient.unRegisterLocationListener(locationListener)
            locationListener = null
        }
        stop()
    }
}