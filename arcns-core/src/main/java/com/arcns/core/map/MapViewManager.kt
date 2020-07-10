package com.arcns.core.map

import androidx.lifecycle.LifecycleOwner

const val ZINDEX_CENTER_FIXED_MARKER = 10000f
const val ZINDEX_MARKER = 9999f
const val ZINDEX_POLYLINE = 9998f
const val ZINDEX_POLYGON = 9997f

/**
 * 地图管理器基类
 */
abstract class MapViewManager<MapView, MyLocationStyle, Marker, Polyline, Polygon, LatLng, MoveCameraData, CameraData>(
    val lifecycleOwner: LifecycleOwner,
    val mapView: MapView,
    val viewManagerData: MapViewManagerData
) {
    // 是否加载完成
    var isMapLoaded = false

    // 点、线、面
    val markers = HashMap<String, Marker>()
    val polylines = HashMap<String, Polyline>()
    val polygons = HashMap<String, Polygon>()

    // 坐标组和点关联器
    val groupMarkers = HashMap<String, ArrayList<String>>()

    // 标志和坐标组关联器
    val tagGroups = HashMap<String, ArrayList<String>>()

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
     * 地图加载完成后的回调
     */
    var onMapLoaded: ((Boolean) -> Unit)? = null

    /**
     * 添加时样式配置格式化，如果如果使用自定义样式时，可以使用该变量，其中options为MarkerOptions或PolylineOptions或PolygonOptions等
     */
    var globalApplyCustomOptions: ApplyCustomOptions? = null


    /**
     * 保存关闭页面时的地图场景相关数据
     */
    abstract fun saveDestroyCamera()


    /**
     * 恢复关闭页面时保存的地图场景相关数据
     */
    abstract fun resumeDestroyCamera()


    /**
     * 停止监听定位
     */
    abstract fun stopLocateMyLocation()

    /**
     * 定位到我的位置
     */
    abstract fun locateMyLocation(
        isLocateMyLocationOnlyWhenFirst: Boolean = false, //仅定位到我的位置一次
        isMoveCamera: Boolean = true,//是否切换场景到我的位置
        isMoveCameraOnlyWhenFirst: Boolean = true, //仅切换到我的位置一次，如果未否则为保持场景在我的定位
        isPriorityResumeDestroyCamera: Boolean = true, // 是否优先恢复上次关闭时保存的场景
        applyCustomMyLocationStyle: ((MyLocationStyle) -> MyLocationStyle)? = null// 自定义我的位置的配置和样式
    )

    /**
     * 移动地图到指定位置
     */
    abstract fun moveCamera(
        mapPosition: MapPosition? = null,
        moveCameraData: MoveCameraData? = null,
        moveCameraAnimationDuration: Long = 0,
        onCompletionCallback: (() -> Unit)? = null
    )

    /**
     * 返回地图的当前场景信息（层级、坐标等）
     */
    abstract fun getCamera(): CameraData

    /**
     * 返回我的定位
     */
    abstract fun getMyLocationData(): MapPosition?

    /**
     * 更新中心点（固定），注意该更新操作将以centerFixedMarkerEnabled为依据
     */
    abstract fun updateCenterFixedMarker()


    /**
     * 重置地图缓存数据
     */
    abstract fun mapViewInvalidate()

    /**
     * 关联点和坐标组
     */
    open fun associateMarkerToGroup(group: MapPositionGroup, markerID: String) {
        groupMarkers[group.uniqueID] = (groupMarkers[group.uniqueID] ?: arrayListOf()).apply {
            if (!this.contains(markerID)) {
                this.add(markerID)
            }
        }
    }

    /**
     * 关联标志和坐标组
     */
    open fun associateGroupsToTag(tag: String, groupsId: String) {
        tagGroups[tag] = (tagGroups[tag] ?: arrayListOf()).apply {
            if (!this.contains(groupsId)) this.add(groupsId)
        }
    }

    /**
     * 取消关联标志和坐标组
     */
    open fun unAssociateGroupsToTag(tag: String? = null, groupsId: String? = null) {
        if (tag == null && groupsId == null) {
            // tag 和 groupsId 都为空时，清空关联
            tagGroups.clear()
        } else if (tag == null && groupsId != null) {
            // 根据groupsId删除关联
            tagGroups.forEach {
                it.value.remove(groupsId)
            }
        } else if (tag != null && groupsId == null) {
            // 根据Tag删除关联
            tagGroups.remove(tag)
        } else {
            // 根据tag 和 groupsId 删除关联
            tagGroups[tag]?.remove(groupsId)
        }
    }

    /**
     *  删除和组不存在关联的点（适用于数据刷新后，自动清理已不能存在的点）
     */
    open fun removeNoAssociateGroupMarkers(group: MapPositionGroup) {
        val markerIDs = groupMarkers[group.uniqueID]
        var noExitGroupMarkerIDs = ArrayList<String>()
        markerIDs?.forEach {
            if (!group.containMapPositionID(it)) {
                noExitGroupMarkerIDs.add(it)
                // 从地图中删除
                if (markers.containsKey(it)) {
                    removeMarker(it)
                }
            }
        }
        markerIDs?.removeAll(noExitGroupMarkerIDs)
    }


    /**
     * 清空所有数据
     */
    open fun clearAll() {
        markers.clear()
        polygons.clear()
        polygons.clear()
        groupMarkers.clear()
        tagGroups.clear()
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
        updateCenterFixedMarker()
    }

    /**
     * 按传入的Tag进行清空
     */
    open fun clear(clearTag: String) {
        var clearIDs = ArrayList<String>()
        // 查找要删除的组
        groupMarkers.forEach {
            if (tagGroups[clearTag]?.contains(it.key) != true) {
                // 只查找相同Tag的组
                return@forEach
            }
            clearIDs.add(it.key)
        }
        polygons.forEach {
            if (tagGroups[clearTag]?.contains(it.key) != true) {
                // 只查找相同Tag的组
                return@forEach
            }
            clearIDs.add(it.key)
        }
        polylines.forEach {
            if (tagGroups[clearTag]?.contains(it.key) != true) {
                // 只查找相同Tag的组
                return@forEach
            }
            clearIDs.add(it.key)
        }
        // 删除
        clearIDs.forEach {
            // 删除点（组）
            groupMarkers[it]?.forEach {
                removeMarker(it) // 从地图中删除
            }
            groupMarkers.remove(it) // 从关系链中删除
            // 删除面
            polygons[it]?.run { removePolygon(this) }
            polygons.remove(it)
            // 删除线
            polylines[it]?.run { removePolyline(this) }
            polylines.remove(it)
        }
        tagGroups.remove(clearTag)
    }

    /**
     * 按传入的新数据进行刷新
     */
    open fun refresh(
        isClearOther: Boolean = false, //是否在刷新的同时删除其他不存在的覆盖物
        refreshTag: String? = null,// 限制只刷新相同Tag的组，为空时不限制
        markerMapPositionGroups: List<MapPositionGroup>? = null,
        polygonMapPositionGroups: List<MapPositionGroup>? = null,
        polylineMapPositionGroups: List<MapPositionGroup>? = null
    ) {
        if (refreshTag == null) {
            if (isClearOther) {
                clearAll()
            }
            refreshMarkers(markerMapPositionGroups = markerMapPositionGroups)
            refreshPolygons(polygonMapPositionGroups = polygonMapPositionGroups)
            refreshPolylines(polylineMapPositionGroups = polylineMapPositionGroups)
            updateCenterFixedMarker()
        } else {
            refreshMarkers(isClearOther, refreshTag, markerMapPositionGroups)
            refreshPolygons(isClearOther, refreshTag, polygonMapPositionGroups)
            refreshPolylines(isClearOther, refreshTag, polylineMapPositionGroups)
        }
    }

    /**
     * 刷新点
     */
    open fun refreshMarkers(
        isClearOther: Boolean = false, //是否在刷新的同时删除其他不存在的点
        refreshTag: String? = null,// 限制只刷新相同Tag的组，为空时不限制
        markerMapPositionGroups: List<MapPositionGroup>? = null
    ) {
        if (isClearOther) {
            var groupIDs = markerMapPositionGroups?.map { it.groupID } ?: arrayListOf()
            var noExitGroupIDs = ArrayList<String>()
            // 查找已经不存在的点（组）
            groupMarkers.forEach {
                if (refreshTag != null && tagGroups[refreshTag]?.contains(it.key) != true) {
                    // 只查找相同Tag的组
                    return@forEach
                }
                if (!groupIDs.contains(it.key)) {
                    noExitGroupIDs.add(it.key)
                }
            }
            // 删除已经不存在的点（组）
            noExitGroupIDs.forEach {
                groupMarkers[it]?.forEach {
                    removeMarker(it) // 从地图中删除
                }
                groupMarkers.remove(it) // 从关系链中删除
                tagGroups[refreshTag]?.remove(it)
            }
        }
        markerMapPositionGroups?.forEach {
            removeNoAssociateGroupMarkers(it)
            addOrUpdateMarkers(it)
            if (refreshTag != null && it.groupID != null) {
                associateGroupsToTag(refreshTag, it.groupID!!)
            }
        }
    }


    /**
     * 刷新线
     */
    open fun refreshPolylines(
        isClearOther: Boolean = false, //是否在刷新的同时删除不存在的线
        refreshTag: String? = null,// 限制只刷新相同Tag的组，为空时不限制
        polylineMapPositionGroups: List<MapPositionGroup>? = null
    ) {
        if (isClearOther) {
            var groupIDs = polylineMapPositionGroups?.map { it.groupID } ?: arrayListOf()
            var noExitGroupIDs = ArrayList<String>()
            // 查找已经不存在的线
            polylines.forEach {
                if (refreshTag != null && tagGroups[refreshTag]?.contains(it.key) != true) {
                    // 只查找相同Tag的组
                    return@forEach
                }
                if (!groupIDs.contains(it.key)) {
                    noExitGroupIDs.add(it.key)
                }
            }
            // 删除已经不存在的线
            noExitGroupIDs.forEach {
                polylines[it]?.run {
                    removePolyline(this)
                }
                polylines.remove(it)
                tagGroups[refreshTag]?.remove(it)
            }
        }
        polylineMapPositionGroups?.forEach {
            addOrUpdatePolyline(it)
            if (refreshTag != null && it.groupID != null) {
                associateGroupsToTag(refreshTag, it.groupID!!)
            }
        }
    }


    /**
     * 刷新面
     */
    open fun refreshPolygons(
        isClearOther: Boolean = false, //是否在刷新的同时删除不存在的面
        refreshTag: String? = null,// 限制只刷新相同Tag的组，为空时不限制
        polygonMapPositionGroups: List<MapPositionGroup>? = null
    ) {
        if (isClearOther) {
            var groupIDs = polygonMapPositionGroups?.map { it.groupID } ?: arrayListOf()
            var noExitGroupIDs = ArrayList<String>()
            // 查找已经不存在的面
            polygons.forEach {
                if (refreshTag != null && tagGroups[refreshTag]?.contains(it.key) != true) {
                    // 只查找相同Tag的组
                    return@forEach
                }
                if (!groupIDs.contains(it.key)) {
                    noExitGroupIDs.add(it.key)
                }
            }
            // 删除已经不存在的面
            noExitGroupIDs.forEach {
                polygons[it]?.run { removePolygon(this) }
                polygons.remove(it)
                tagGroups[refreshTag]?.remove(it)
            }
        }
        polygonMapPositionGroups?.forEach {
            addOrUpdatePolygons(it)
            if (refreshTag != null && it.groupID != null) {
                associateGroupsToTag(refreshTag, it.groupID!!)
            }
        }
    }


    /**
     * 设置点坐标
     */
    abstract fun setMarkerPosition(marker: Marker, position: MapPosition)

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
    open fun addOrUpdateMarker(
        mapPosition: MapPosition,
        mapPositionGroup: MapPositionGroup? = null
    ) {
        if (markers.containsKey(mapPosition.id)) {
            setMarkerPosition(markers[mapPosition.id] ?: return, mapPosition)
        } else {
            addMarker(mapPosition, mapPositionGroup)
        }
    }


    /**
     * 返回中心点坐标
     */
    abstract fun getCenterFixedPosition(): MapPosition

    /**
     * 返回左上角坐标
     */
    abstract fun getLeftTopFixedPosition(): MapPosition

    /**
     * 返回左下角坐标
     */
    abstract fun getLeftBottomFixedPosition(): MapPosition

    /**
     * 返回右上角坐标
     */
    abstract fun getRightTopFixedPosition(): MapPosition

    /**
     * 返回右下角坐标
     */
    abstract fun getRightBottomFixedPosition(): MapPosition


    /**
     * 添加中心点（固定）的坐标到坐标组
     */
    open fun addCenterFixedMarker(mapPositionGroup: MapPositionGroup): String? =
        addMarker(getCenterFixedPosition(), mapPositionGroup)


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
    open fun removeLastMarker(mapPositionGroup: MapPositionGroup) {
        mapPositionGroup.removeMapPosition()?.run {
            groupMarkers[mapPositionGroup?.uniqueID]?.remove(id) //删除关联关系
            removeMarker(id)
        }
    }

    /**
     * 删除点
     */
    abstract fun removeMarker(marker: Marker)


    /**
     * 删除点（同时更新数据到MapPositionGroup）
     */
    open fun removeMarker(id: String?, mapPositionGroup: MapPositionGroup? = null) {
        mapPositionGroup?.removeMapPosition(id)
        groupMarkers[mapPositionGroup?.uniqueID]?.remove(id) //删除关联关系
        markers[id]?.run {
            removeMarker(this)
            mapViewInvalidate()
        }
    }


    /**
     * 删除多个点（同时更新数据到MapPositionGroup）
     */
    open fun removeMarkers(mapPositionGroup: MapPositionGroup) {
        // 删除关联关系
        removeNoAssociateGroupMarkers(mapPositionGroup)
        groupMarkers.remove(mapPositionGroup.uniqueID)
        // 从地图中删除
        mapPositionGroup.mapPositions.forEach {
            removeMarker(markers[it.id] ?: return@forEach)
        }
        mapPositionGroup.clearMapPosition()
        mapViewInvalidate()
    }


    /**
     * 计算长度
     */
    abstract fun calculateLineDistance(mapPositionGroup: MapPositionGroup): Double


    /**
     * 计算面积
     */
    abstract fun calculateArea(mapPositionGroup: MapPositionGroup): Double


    /**
     * 比较坐标是否一致
     */
    abstract fun equaltLatLng(
        latLng1: LatLng,
        latLng2: LatLng,
        decimalPlaces: Int? = null,
        isRounding: Boolean = true
    ): Boolean

    /**
     * 比较场景是否一致
     * 可以仅对比某些项，但必须至少对比一项，否则返回false
     */
    abstract fun equaltCamera(
        latLng: LatLng? = null, //坐标,
        latLngDecimalPlaces: Int? = null,//坐标保留的小数位数，若为空则不做处理，保持原有位数
        latLngIsRounding: Boolean = true,//坐标保留小数位时是否四舍五入
        zoom: Float? = null, //缩放层级
        tilt: Float? = null, //俯仰角（overlook）
        bearing: Float? = null //偏航角（rotate）
    ): Boolean

    /**
     * 坐标点是否包含在多边形内
     */
    abstract fun isPolygonContainsPoint(polygonLatLngs: List<LatLng>, latLng: LatLng): Boolean
}