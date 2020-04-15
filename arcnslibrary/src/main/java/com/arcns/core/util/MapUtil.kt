package com.arcns.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.arcns.core.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


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
    latLng: MapLatLng,
    browserMapNavigationTitle: String? = null,
    browserMapNavigationContent: String? = null,
    seletorTitle: String? = null
): (MaterialDialog.() -> Unit)? {
    var existMaps = getExistMaps()
    if (existMaps == null || existMaps.size == 0) {
        // 手机没有安装地图应用时，直接打开网页版地图导航
        openBrowserMapNavigation(
            latLng,
            browserMapNavigationTitle,
            browserMapNavigationContent
        )
    } else if (existMaps.size == 1) {
        // 手机只安装一个地图应用时直接打开导航
        openMapNavigation(
            latLng, existMaps[0]
        )
    } else {
        // 手机只安装多个地图应用时弹出导航地图应用选择列表
        val setupDialog: MaterialDialog.() -> Unit = {
            if (seletorTitle != null) {
                title(text = seletorTitle)
            }
            listItems(items = existMaps) { dialog, index, text ->
                context?.openMapNavigation(
                    latLng, text.toString()
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
    latLng: MapLatLng,
    browserMapNavigationTitle: String? = null,
    browserMapNavigationContent: String? = null,
    seletorTitle: String? = null
) {
    showDialog(
        func = getMapNavigationSeletor(
            latLng, browserMapNavigationTitle, browserMapNavigationContent, seletorTitle
        ) ?: return
    )
}

/**
 * 根据应用名打开相应导航
 */
fun Context.openMapNavigation(latLng: MapLatLng, mapName: String? = null) = when (mapName) {
    getString(R.string.text_map_navigation_item_baidu) -> openBaiduMapNavigation(latLng)
    getString(R.string.text_map_navigation_item_gaode) -> openGaoDeMapNavigation(latLng)
    else -> Unit
}

/**
 * 打开高德导航
 */
fun Context.openGaoDeMapNavigation(
    latLng: MapLatLng
) {
    var gcLatLng = latLng.toGCJ02
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
    latLng: MapLatLng
) {
    val intent = Intent()
    intent.data = Uri.parse(
        "baidumap://map/geocoder?location=${latLng.latitude},${latLng.longitude}"
    )
    startActivity(intent)
}

/**
 * 打开浏览器导航
 */
fun Context.openBrowserMapNavigation(latLng: MapLatLng, title: String?, content: String?) {
    var gcLatLng = latLng.toGCJ02
    var intent = Intent();
    intent.setAction("android.intent.action.VIEW");
    intent.setData(Uri.parse("http://api.map.baidu.com/marker?location=${gcLatLng.latitude},${gcLatLng.longitude}&title=$title&content=$content&output=html"));
    startActivity(intent);
}

/**
 * 坐标
 */
data class MapLatLng(
    var latitude: Double,
    var longitude: Double,
    // 坐标类型，true为BD09LL百度经纬度坐标，false为GCJ02国测火星坐标
    var isBD09LL: Boolean = true
) {
    var pi = 3.1415926535897932384626
    val toGCJ02: MapLatLng
        get() {
            if (!isBD09LL) {
                return this
            }
            val x = longitude - 0.0065
            val y = latitude - 0.006
            val z =
                sqrt(x * x + y * y) - 0.00002 * sin(y * pi)
            val theta =
                Math.atan2(y, x) - 0.000003 * cos(x * pi)
            return MapLatLng(latitude = z * sin(theta), longitude = z * cos(theta))
        }
    val toBD09LL: MapLatLng
        get() {
            if (isBD09LL) {
                return this
            }
            val x = longitude
            val y = latitude
            val z =
                sqrt(x * x + y * y) + 0.00002 * sin(y * pi)
            val theta =
                atan2(y, x) + 0.000003 * cos(x * pi)
            return MapLatLng(latitude = z * sin(theta) + 0.006, longitude = z * cos(theta) + 0.0065)
        }
}