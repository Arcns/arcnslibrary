package com.arcns.core.map

import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

const val ZINDEX_CENTER_FIXED_MARKER = 10000f
const val ZINDEX_MARKER = 9999f
const val ZINDEX_POLYLINE = 9998f
const val ZINDEX_POLYGON = 9997f

/**
 * 地图管理器接口
 */
abstract class MapViewManager<MapView, MyLocationStyle, Marker, Polyline, Polygon, LatLng>(
    lifecycleOwner: LifecycleOwner,
    val mapView: MapView,
    val viewModel: MapViewManagerViewModel
) {
    // 是否加载完成
    var isMapLoaded = false

    // 点、线、面
    val markers = HashMap<String, Marker>()
    val polylines = HashMap<String, Polyline>()
    val polygons = HashMap<String, Polygon>()

    // 中心点（固定）
    var centerFixedMarker: Marker? = null
        protected set
    var centerFixedMarkerApplyCustomOptions: ApplyCustomOptions? = null
    var centerFixedMarkerEnabled: Boolean = false
        set(value) {
            field = value
            updateCenterFixedMarker()
        }

    /**
     * 定位到我的位置
     */
    abstract fun locateMyLocation(
        isLocateMyLocationOnlyWhenFirst: Boolean = false, //仅定位到我的位置一次
        isMoveCameraOnlyWhenFirst: Boolean = true, //仅切换到我的位置一次
        isFirstFlagFromViewModel: Boolean = true, // 是否首次加载从viewmodel进行获取
        applyCustomMyLocationStyle: ((MyLocationStyle) -> MyLocationStyle)? = null// 自定义我的位置的配置和样式
    )

    /**
     * 更新中心点（固定），注意该更新操作将以centerFixedMarkerEnabled为依据
     */
    abstract fun updateCenterFixedMarker()


    /**
     * 清空所有数据
     */
    open fun clear() {
        markers.clear()
        polygons.clear()
        polygons.clear()
    }


    /**
     * 按传入的新数据进行清空
     */
    open fun clear(
        markerMapPositionGroups: List<MapPositionGroup>? = null,
        polygonMapPositionGroups: List<MapPositionGroup>? = null,
        polylineMapPositionGroups: List<MapPositionGroup>? = null
    ) {
        markerMapPositionGroups?.forEach {
            removeMarkers(it)
        }
        polygonMapPositionGroups?.forEach {
            removePolygon(it)
        }
        polylineMapPositionGroups?.forEach {
            removePolyline(it)
        }
    }


    /**
     * 按传入的新数据进行刷新
     */
    open fun refresh(
        markerMapPositionGroups: List<MapPositionGroup>? = null,
        polygonMapPositionGroups: List<MapPositionGroup>? = null,
        polylineMapPositionGroups: List<MapPositionGroup>? = null
    ) {
        clear()
        markerMapPositionGroups?.forEach {
            addOrUpdateMarkers(it)
        }
        polygonMapPositionGroups?.forEach {
            addOrUpdatePolygons(it)
        }
        polylineMapPositionGroups?.forEach {
            addOrUpdatePolyline(it)
        }
    }

    /**
     * 删除线
     */
    abstract fun removePolyline(polyline: Polyline)

    /**
     * 删除线
     */
    open fun removePolyline(mapPositionGroup: MapPositionGroup, isRemoveMarkers: Boolean = true) {
        polylines[mapPositionGroup.groupID]?.run {
            removePolyline(this)
            polylines.remove(mapPositionGroup.groupID)
            mapPositionGroup.clearGroupID()
        }
        if (isRemoveMarkers) {
            removeMarkers(mapPositionGroup)
        }
    }

    /**
     * 删除多边形
     */
    abstract fun removePolygon(polyline: Polygon)

    /**
     * 删除多边形
     */
    open fun removePolygon(mapPositionGroup: MapPositionGroup, isRemoveMarkers: Boolean = true) {
        // 状态为线时
        if (polylines.containsKey(mapPositionGroup.groupID)) {
            removePolyline(mapPositionGroup, isRemoveMarkers)
            return
        }
        // 状态为多边形时
        polygons[mapPositionGroup.groupID]?.run {
            removePolygon(this)
            polygons.remove(mapPositionGroup.groupID)
            mapPositionGroup.clearGroupID()
        }
        if (isRemoveMarkers) {
            removeMarkers(mapPositionGroup)
        }
    }


    /**
     * 添加或刷新线
     */
    abstract fun addOrUpdatePolyline(mapPositionGroup: MapPositionGroup)


    /**
     * 添加或刷新多边形
     */
    abstract fun addOrUpdatePolygons(mapPositionGroup: MapPositionGroup)


    /**
     * 添加或更新多个点
     */
    open fun addOrUpdateMarkers(mapPositionGroup: MapPositionGroup) {
        mapPositionGroup.setMapPositions(mapPositionGroup.mapPositions.apply {
            forEach {
                addOrUpdateMarker(it, mapPositionGroup)
            }
        })
    }


    /**
     * 添加或更新点（注意，如果数据集合未包含该点的id，则会在添加后把id赋值给对象）
     */
    abstract fun addOrUpdateMarker(
        mapPosition: MapPosition,
        mapPositionGroup: MapPositionGroup? = null
    )


    /**
     * 添加中心点（固定）的坐标到坐标组
     */
    abstract fun addCenterFixedMarker(mapPositionGroup: MapPositionGroup): String?


    /**
     * 添加点（若mapPositionGroup不为空则同时更新数据到MapPositionGroup）
     */
    abstract fun addMarker(
        position: MapPosition,
        mapPositionGroup: MapPositionGroup? = null,
        applyCustomOptions: ApplyCustomOptions? = mapPositionGroup?.applyCustomOptions
    ): String


    /**
     * 删除最后一个点（同时更新数据到MapPositionGroup）
     */
    abstract fun removeLastMarker(mapPositionGroup: MapPositionGroup)


    /**
     * 删除点（同时更新数据到MapPositionGroup）
     */
    abstract fun removeMarker(id: String?, mapPositionGroup: MapPositionGroup? = null)


    /**
     * 删除多个点（同时更新数据到MapPositionGroup）
     */
    abstract fun removeMarkers(mapPositionGroup: MapPositionGroup)


    /**
     * 计算长度
     */
    abstract fun calculateLineDistance(mapPositionGroup: MapPositionGroup): Double


    /**
     * 计算面积
     */
    abstract fun calculateArea(mapPositionGroup: MapPositionGroup): Double
}