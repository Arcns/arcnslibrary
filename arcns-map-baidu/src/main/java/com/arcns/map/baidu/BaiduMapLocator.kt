package com.arcns.map.baidu

import android.content.Context
import com.arcns.core.map.MapPosition
import com.arcns.core.map.MapPositionType
import com.arcns.core.map.MapLocator
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption

/**
 * 高德地图定位器
 */
class BaiduMapLocator(
    context: Context,
    applyCustomLocationClientOption: ((LocationClientOption) -> Void)? = null
) : MapLocator(context) {

    val locationClient = LocationClient(context).apply {
        // 定位配置
        locOption = LocationClientOption().apply {
            openGps = true
            coorType = "bd09ll"
            scanSpan = 1000
            setIsNeedAddress(true)
            applyCustomLocationClientOption?.invoke(this)
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
                    val position = MapPosition(
                        latitude = latitude,
                        longitude = longitude,
                        type = MapPositionType.GCJ02,
                        extraData = this
                    )
                    onPerSecondCallback(position)
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