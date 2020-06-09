package com.arcns.map.baidu

import android.os.Bundle
import com.arcns.core.APP
import com.arcns.core.map.MapViewManager
import com.arcns.core.map.MapPositionType
import com.arcns.core.map.MapPosition
import com.arcns.core.map.MapPositionGroup
import com.arcns.core.util.bitmap
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.AreaUtil
import com.baidu.mapapi.utils.DistanceUtil
import java.util.*


/**
 * 把通用地图管理器转为百度地图管理器，通常用于使用百度地图管理器特有的功能
 */
val MapViewManager<*,*,*,*,*,*,*>.asGaoDe: BaiduMapViewManager? get() = this as? BaiduMapViewManager

/**
 * 把百度坐标转换为通用坐标
 */
val LatLng.toMapPosition: MapPosition
    get() = MapPosition(
        latitude = latitude,
        longitude = longitude,
        type = MapPositionType.BD09LL
    )

/**
 * 把通用坐标转换位百度坐标
 */
val MapPosition.toBaidu: LatLng
    get() = toBD09LL.let {
        LatLng(
            it.latitude,
            it.longitude
        )
    }



// 字符串id在扩展信息中的key
const val BAIDU_OVERLAY_ID_KEY = "BAIDU_OVERLAY_ID_KEY"

/**
 * 为百度Overlay对象创建一个id，保存在扩展信息中
 */
fun OverlayOptions.addNewID(id: String? = null): String {
    val newID = id ?: UUID.randomUUID().toString() // 生成一个随机的字符串id
    when (this) {
        is MarkerOptions -> extraInfo((extraInfo ?: Bundle()).apply {
            putString(BAIDU_OVERLAY_ID_KEY, newID)
        })
        is PolylineOptions -> extraInfo((extraInfo ?: Bundle()).apply {
            putString(BAIDU_OVERLAY_ID_KEY, newID)
        })
        is PolygonOptions -> extraInfo((extraInfo ?: Bundle()).apply {
            putString(BAIDU_OVERLAY_ID_KEY, newID)
        })
    }
    return newID
}

/**
 * 获取扩展信息中的id
 */
val Overlay.id: String? get() = extraInfo?.getString(BAIDU_OVERLAY_ID_KEY, null)

/**
 * 创建一个百度地图自定义图标
 */
fun Int.newBaiduIcon(width: Int? = null, height: Int? = null) =
    BitmapDescriptorFactory.fromBitmap(
        bitmap(
            context = APP.INSTANCE,
            newWidth = width,
            newHeight = height
        )
    )

/**
 * 计算百度长度
 */
fun calculateBaiduLineDistance(mapPositionGroup: MapPositionGroup): Double {
    var lastPosition: MapPosition? = null
    var lineDistance = 0.0
    mapPositionGroup.mapPositions.forEach {
        if (lastPosition != null) {
            lineDistance += DistanceUtil.getDistance(
                lastPosition?.toBaidu,
                it.toBaidu
            )
        }
        lastPosition = it
    }
    return lineDistance
}

/**
 * 计算百度面积
 */
fun calculateBaiduArea(mapPositionGroup: MapPositionGroup): Double =
    AreaUtil.calculateArea(mapPositionGroup.mapPositions.map {
        it.toBaidu
    })