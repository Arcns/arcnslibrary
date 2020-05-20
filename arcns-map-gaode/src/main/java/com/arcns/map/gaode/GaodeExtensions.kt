package com.arcns.map.gaode

import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.animation.TranslateAnimation
import com.arcns.core.APP
import com.arcns.core.map.MapViewManager
import com.arcns.core.map.MapLatLngType
import com.arcns.core.map.MapPosition
import com.arcns.core.util.bitmap
import com.arcns.core.util.dp


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
val MapViewManager<*,*,*,*,*,*>.asGaode: GaodeMapViewManager? get() = this as? GaodeMapViewManager

/**
 * 把高德坐标转换为通用坐标
 */
val LatLng.toMapPosition: MapPosition
    get() = MapPosition(
        latitude = latitude,
        longitude = longitude,
        type = MapLatLngType.GCJ02
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
