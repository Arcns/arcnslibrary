package com.arcns.core.map

/**
 * 地图管理器接口
 */
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


