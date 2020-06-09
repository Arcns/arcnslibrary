package com.arcns.core.map

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.arcns.core.R
import com.arcns.core.util.showDialog
import kotlin.math.abs


// 百度地图
const val MAP_BAIDU_PKG = "com.baidu.BaiduMap"

// 高德地图
const val MAP_GAODE_PKG = "com.autonavi.minimap"

/**
 * 返回已安装的导航应用
 */
fun Context.getExistMaps(): ArrayList<String> {
    var existMaps = ArrayList<String>()
    if (checkMapIsExist(MAP_BAIDU_PKG)) {
        existMaps.add(getString(R.string.text_map_navigation_item_baidu))
    }
    if (checkMapIsExist(MAP_GAODE_PKG)) {
        existMaps.add(getString(R.string.text_map_navigation_item_gaode))
    }
    return existMaps
}

/**
 * 检查导航应用是否安装
 */
fun Context.checkMapIsExist(
    packageName: String?
): Boolean {
    var packageInfo = try {
        packageManager.getPackageInfo(packageName, 0)
    } catch (e: Exception) {
        null
    }
    return packageInfo != null
}

/**
 * 打开地图导航选择器
 */
fun Activity.getMapNavigationSeletor(
    position: MapPosition,
    browserMapNavigationTitle: String? = null,
    browserMapNavigationContent: String? = null,
    seletorTitle: String? = null
): (MaterialDialog.() -> Unit)? {
    var existMaps = getExistMaps()
    if (existMaps == null || existMaps.size == 0) {
        // 手机没有安装地图应用时，直接打开网页版地图导航
        openBrowserMapNavigation(
            position,
            browserMapNavigationTitle,
            browserMapNavigationContent
        )
    } else if (existMaps.size == 1) {
        // 手机只安装一个地图应用时直接打开导航
        openMapNavigation(
            position, existMaps[0]
        )
    } else {
        // 手机只安装多个地图应用时弹出导航地图应用选择列表
        val setupDialog: MaterialDialog.() -> Unit = {
            if (seletorTitle != null) {
                title(text = seletorTitle)
            }
            listItems(items = existMaps) { dialog, index, text ->
                context?.openMapNavigation(
                    position, text.toString()
                )
            }
        }
        return setupDialog
    }
    return null
}


/**
 * 打开地图导航选择器
 */
fun Activity.openMapNavigationSeletor(
    position: MapPosition,
    browserMapNavigationTitle: String? = null,
    browserMapNavigationContent: String? = null,
    seletorTitle: String? = null
) {
    showDialog(
        func = getMapNavigationSeletor(
            position, browserMapNavigationTitle, browserMapNavigationContent, seletorTitle
        ) ?: return
    )
}

/**
 * 根据应用名打开相应导航
 */
fun Context.openMapNavigation(position: MapPosition, mapName: String? = null) = when (mapName) {
    getString(R.string.text_map_navigation_item_baidu) -> openBaiduMapNavigation(position)
    getString(R.string.text_map_navigation_item_gaode) -> openGaodeMapNavigation(position)
    else -> Unit
}

/**
 * 打开高德导航
 */
fun Context.openGaodeMapNavigation(
    position: MapPosition
) {
    var gcLatLng = position.toGCJ02
    val intent = Intent(
        "android.intent.action.VIEW",
        Uri.parse("androidamap://navi?sourceApplication=" + getString(R.string.app_name) + "&lat=${gcLatLng.latitude}&lon=${gcLatLng.longitude}&dev=0")
    )
    intent.setPackage(MAP_GAODE_PKG)
    startActivity(intent)
}

/**
 * 打开百度导航
 */
fun Context.openBaiduMapNavigation(
    position: MapPosition
) {
    val intent = Intent()
    intent.data = Uri.parse(
        "baidumap://map/geocoder?location=${position.latitude},${position.longitude}"
    )
    startActivity(intent)
}

/**
 * 打开浏览器导航
 */
fun Context.openBrowserMapNavigation(position: MapPosition, title: String?, content: String?) {
    var gcLatLng = position.toGCJ02
    var intent = Intent();
    intent.setAction("android.intent.action.VIEW");
    intent.setData(Uri.parse("http://api.map.baidu.com/marker?location=${gcLatLng.latitude},${gcLatLng.longitude}&title=$title&content=$content&output=html"));
    startActivity(intent);
}

/**
 * 转换为自适应单位（输入单位为米）
 */
fun Double.adaptiveMapUnit(units: List<String> = listOf("m", "km"), decimalScale: Int = 2) =
    adaptiveMapUnitValue(decimalScale).toString() + adaptiveMapUnitName(units)

/**
 * 转换为自适应单位后的值（输入单位为米）
 */
fun Double.adaptiveMapUnitValue(decimalScale: Int = 2) =
    if (abs(this) >= 1000) String.format("%." + decimalScale + "f", this / 1000)
        .toDouble() else this

/**
 * 转换为自适应单位后的单位名（输入单位为米）
 */
fun Double.adaptiveMapUnitName(units: List<String> = listOf("m", "km")) =
    if (abs(this) >= 1000) units[1] else units[0]

/**
 * 计算两点之间的距离（单位为米）
 */
fun MapPosition.distanceBetween(mapPosition: MapPosition): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        latitude,
        longitude,
        mapPosition.latitude,
        mapPosition.longitude,
        results
    )
    return results[0]
}