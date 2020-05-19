package com.arcns.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
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
    getString(R.string.text_map_navigation_item_gaode) -> openGaoDeMapNavigation(position)
    else -> Unit
}

/**
 * 打开高德导航
 */
fun Context.openGaoDeMapNavigation(
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
 * 坐标类型
 */
enum class MapLatLngType {
    // BD09LL百度经纬度坐标
    BD09LL,

    // GCJ02国测火星坐标
    GCJ02
}

/**
 * 坐标
 */
data class MapPosition(
    var id: String? = null,
    var latitude: Double,
    var longitude: Double,
    var type: MapLatLngType
) {

    constructor(latitude: Double, longitude: Double, type: MapLatLngType) : this(
        id = null,
        latitude = latitude,
        longitude = longitude,
        type = type
    )

    var pi = 3.1415926535897932384626
    val toGCJ02: MapPosition
        get() {
            if (type == MapLatLngType.GCJ02) {
                return this
            }
            val x = longitude - 0.0065
            val y = latitude - 0.006
            val z =
                sqrt(x * x + y * y) - 0.00002 * sin(y * pi)
            val theta =
                Math.atan2(y, x) - 0.000003 * cos(x * pi)
            return MapPosition(
                latitude = z * sin(theta),
                longitude = z * cos(theta),
                type = MapLatLngType.GCJ02
            )
        }
    val toBD09LL: MapPosition
        get() {
            if (type == MapLatLngType.BD09LL) {
                return this
            }
            val x = longitude
            val y = latitude
            val z =
                sqrt(x * x + y * y) + 0.00002 * sin(y * pi)
            val theta =
                atan2(y, x) + 0.000003 * cos(x * pi)
            return MapPosition(
                latitude = z * sin(theta) + 0.006,
                longitude = z * cos(theta) + 0.0065,
                type = MapLatLngType.BD09LL
            )
        }
}

interface IMapViewManager {
    var centerFixedMarkerEnabled: Boolean

    /**
     * 清空所有数据
     */
    fun clear()


    /**
     * 按传入的新数据进行清空
     */
    fun clear(
        markerMapPositionGroups: List<MapPositionGroup>? = null,
        polygonMapPositionGroups: List<MapPositionGroup>? = null,
        polylineMapPositionGroups: List<MapPositionGroup>? = null
    )

    /**
     * 按传入的新数据进行刷新
     */
    fun refresh(
        markerMapPositionGroups: List<MapPositionGroup>? = null,
        polygonMapPositionGroups: List<MapPositionGroup>? = null,
        polylineMapPositionGroups: List<MapPositionGroup>? = null
    )


    /**
     * 删除线
     */
    fun removePolyline(mapPositionGroup: MapPositionGroup, isRemoveMarkers: Boolean = true)


    /**
     * 删除多边形
     */
    fun removePolygon(mapPositionGroup: MapPositionGroup, isRemoveMarkers: Boolean = true)


    /**
     * 添加或刷新线
     */
    fun addOrUpdatePolyline(mapPositionGroup: MapPositionGroup)


    /**
     * 添加或刷新多边形
     */
    fun addOrUpdatePolygons(mapPositionGroup: MapPositionGroup)


    /**
     * 添加或更新多个点
     */
    fun addOrUpdateMarkers(mapPositionGroup: MapPositionGroup)


    /**
     * 添加或更新点（注意，如果数据集合未包含该点的id，则会在添加后把id赋值给对象）
     */
    fun addOrUpdateMarker(
        mapPosition: MapPosition,
        mapPositionGroup: MapPositionGroup? = null
    )


    /**
     * 添加中心点（固定）的坐标到坐标组
     */
    fun addCenterFixedMarker(mapPositionGroup: MapPositionGroup): String?


    /**
     * 添加点（若mapPositionGroup不为空则同时更新数据到MapPositionGroup）
     */
    fun addMarker(
        position: MapPosition,
        mapPositionGroup: MapPositionGroup? = null,
        applyCustomOptions: ApplyCustomOptions? = mapPositionGroup?.applyCustomOptions
    ): String


    /**
     * 删除最后一个点（同时更新数据到MapPositionGroup）
     */
    fun removeLastMarker(mapPositionGroup: MapPositionGroup)


    /**
     * 删除点（同时更新数据到MapPositionGroup）
     */
    fun removeMarker(id: String?, mapPositionGroup: MapPositionGroup? = null)


    /**
     * 删除多个点（同时更新数据到MapPositionGroup）
     */
    fun removeMarkers(mapPositionGroup: MapPositionGroup)


    /**
     * 计算长度
     */
    fun calculateLineDistance(mapPositionGroup: MapPositionGroup): Double


    /**
     * 计算面积
     */
    fun calculateArea(mapPositionGroup: MapPositionGroup): Double
}


typealias ApplyCustomOptions = ((options: Any) -> Unit)

class MapPositionGroup {
    private var _groupID = MutableLiveData<String>()
    val groupID: String? get() = _groupID.value

    private var _mapPositions =
        MutableLiveData<ArrayList<MapPosition>>().apply { value = ArrayList() }
    val mapPositions: ArrayList<MapPosition> get() = _mapPositions.value ?: arrayListOf()

    /**
     * 添加时样式配置格式化，如果如果使用自定义样式时，可以使用该变量，其中options为MarkerOptions或PolylineOptions或PolygonOptions等
     */
    var applyCustomOptions: ApplyCustomOptions? = null

    /**
     * 根据id查找地图点
     */
    fun findMapPositionByID(id: String): MapPosition? {
        mapPositions.forEach {
            if (it.id == id) {
                return it
            }
        }
        return null
    }

    /**
     * 根据id判断组合中是否包含地图点
     */
    fun containMapPositionID(id: String): Boolean = findMapPositionByID(id) != null

    /**
     * 设置地图组
     */
    fun setGroupID(groupID: String) {
        _groupID.value = groupID
    }

    /**
     * 清空地图组ID
     */
    fun clearGroupID() {
        _groupID.value = null
    }

    /**
     * 设置地图点列表
     */
    fun setMapPositions(mapPositions: ArrayList<MapPosition>?) {
        _mapPositions.value = mapPositions ?: arrayListOf()
    }

    /**
     * 清空地图点列表
     */
    fun clearMapPositions() = setMapPositions(null)

    /**
     * 添加地图点
     */
    fun addMapPosition(
        id: String? = null,
        latitude: Double,
        longitude: Double,
        type: MapLatLngType
    ): MapPosition? =
        addMapPosition(MapPosition(latitude, longitude, type))

    /**
     * 添加地图点
     */
    fun addMapPosition(position: MapPosition): MapPosition? {
        _mapPositions.value =
            _mapPositions.value?.apply {
                if (!contains(position)) {
                    add(position)
                    return position
                }
            }
        return null
    }

    fun clearMapPosition() {
        _mapPositions.value = arrayListOf()
    }


    /**
     * 删除指定的地图点，注意如果不传入值，则默认会删除最后一个添加的地图点
     */
    fun removeMapPosition(position: MapPosition? = _mapPositions.value?.lastOrNull()): MapPosition? {
        _mapPositions.value =
            _mapPositions.value?.apply {
                if (contains(position)) {
                    remove(position)
                    return position
                }
            }

        return null
    }

    /**
     * 按id进行对地图点进行删除
     */
    fun removeMapPosition(id: String?): MapPosition? {
        _mapPositions.value =
            _mapPositions.value?.apply {
                var removeItem: MapPosition? = null
                run removeByID@{
                    forEach {
                        if (it.id == id) {
                            removeItem = it
                            remove(it)
                            return@removeByID
                        }
                    }
                }
                return removeItem
            }

        return null
    }
}