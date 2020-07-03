package com.arcns.map.gaode

import com.amap.api.maps.AMapUtils
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.animation.TranslateAnimation
import com.arcns.core.APP
import com.arcns.core.map.MapViewManager
import com.arcns.core.map.MapPositionType
import com.arcns.core.map.MapPosition
import com.arcns.core.map.MapPositionGroup
import com.arcns.core.util.bitmap
import com.arcns.core.util.dp
import com.arcns.core.util.keepDecimalPlaces
import com.arcns.core.util.toArrayList
import java.util.ArrayList


/**
 * 为高德地图中的点添加跳动动画
 */
fun Marker.startBeatingAnimation(mapView: MapView) {
    //根据屏幕距离计算需要移动的目标点
    val point = mapView.map.projection.toScreenLocation(position)
    point.y -= 125.dp
    val target = mapView.map.projection.fromScreenLocation(point)
    //使用TranslateAnimation,填写一个需要移动的目标点
    val animation = TranslateAnimation(target)
    animation.setInterpolator { input -> // 模拟重加速度的interpolator
        if (input <= 0.5) {
            (0.5f - 2 * (0.5 - input) * (0.5 - input)).toFloat()
        } else {
            (0.5f - kotlin.math.sqrt((input - 0.5f) * (1.5f - input).toDouble())).toFloat()
        }
    }
    //整个移动所需要的时间
    animation.setDuration(600)
    //设置动画
    setAnimation(animation)
    //开始动画
    startAnimation()
}

/**
 * 把通用地图管理器转为高德地图管理器，通常用于使用高低地图管理器特有的功能
 */
val MapViewManager<*, *, *, *, *, *, *, *>.asGaode: GaodeMapViewManager? get() = this as? GaodeMapViewManager

/**
 * 把高德坐标转换为通用坐标
 */
val LatLng.toMapPosition: MapPosition
    get() = MapPosition(
        latitude = latitude,
        longitude = longitude,
        type = MapPositionType.GCJ02
    )

/**
 * 把通用坐标转换位高德坐标
 */
val MapPosition.toGaoDe: LatLng
    get() = toGCJ02.let {
        LatLng(
            it.latitude,
            it.longitude
        )
    }

/**
 * 把通用坐标列表转换为高德坐标列表
 */
val ArrayList<MapPosition>.toGaoDeLatLngs: ArrayList<LatLng> get() = map { it.toGaoDe }.toArrayList()

/**
 * 返回高德坐标列表
 */
val MapPositionGroup.mapPositionGaoDeLatLngs: ArrayList<LatLng>
    get() = mapPositions.toGaoDeLatLngs

/**
 * 创建一个高德地图自定义图标
 */
fun Int.newGaodeIcon(width: Int? = null, height: Int? = null) =
    BitmapDescriptorFactory.fromBitmap(
        bitmap(
            context = APP.INSTANCE,
            newWidth = width,
            newHeight = height
        )
    )

/**
 * 计算高德长度
 */
fun calculateGaodeLineDistance(mapPositionGroup: MapPositionGroup): Double {
    var lastPosition: MapPosition? = null
    var lineDistance = 0.0
    mapPositionGroup.mapPositions.forEach {
        if (lastPosition != null) {
            lineDistance += AMapUtils.calculateLineDistance(
                lastPosition?.toGaoDe,
                it.toGaoDe
            );
        }
        lastPosition = it
    }
    return lineDistance
}

/**
 * 计算高德面积
 */
fun calculateGaodeArea(mapPositionGroup: MapPositionGroup): Double =
    AMapUtils.calculateArea(mapPositionGroup.mapPositions.map {
        it.toGaoDe
    }).toDouble()


/**
 * 比较坐标是否一致
 */
fun equaltGaodeLatLng(
    latLng1: LatLng,
    latLng2: LatLng,
    decimalPlaces: Int? = null,
    isRounding: Boolean = true
): Boolean {
    return if (decimalPlaces == null)
        latLng1.latitude == latLng2.latitude && latLng1.longitude == latLng2.longitude
    else
        latLng1.latitude.keepDecimalPlaces(
            decimalPlaces, isRounding
        ) == latLng2.latitude.keepDecimalPlaces(
            decimalPlaces, isRounding
        ) && latLng1.longitude.keepDecimalPlaces(
            decimalPlaces, isRounding
        ) == latLng2.longitude.keepDecimalPlaces(
            decimalPlaces, isRounding
        )
}