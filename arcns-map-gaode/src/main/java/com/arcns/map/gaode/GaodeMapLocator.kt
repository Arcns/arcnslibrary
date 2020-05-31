package com.arcns.map.gaode

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.arcns.core.map.MapPosition
import com.arcns.core.map.MapPositionType
import com.arcns.core.map.MapLocator

/**
 * 高德地图定位器
 */
class GaodeMapLocator(
    context: Context,
    applyCustomLocationClientOption: ((AMapLocationClientOption) -> Void)? = null
) : MapLocator(context) {

    val locationClient = AMapLocationClient(context).apply {
        // 定位配置
        setLocationOption(AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            interval = 1000
            isNeedAddress = true
            applyCustomLocationClientOption?.invoke(this)
        })
    }
    private var locationListener: AMapLocationListener? = null

    init {
        // 定位监听
        locationClient.setLocationListener(object : AMapLocationListener {
            init {
                locationListener = this
            }

            override fun onLocationChanged(location: AMapLocation?) {
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
        locationClient.stopLocation()
    }

    /**
     * 开始
     */
    override fun start() {
        if (!locationClient.isStarted)
            locationClient.startLocation()
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
        locationClient.onDestroy()
    }
}