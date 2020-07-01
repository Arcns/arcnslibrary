package com.arcns.map.baidu

import android.graphics.Point
import android.os.Handler
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.arcns.core.map.*
import com.arcns.core.util.dp
import com.arcns.core.util.keepDecimalPlaces
import com.baidu.location.BDLocation
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng

/**
 * 百度地图管理器
 */
class BaiduMapViewManager(
    lifecycleOwner: LifecycleOwner,
    mapView: MapView,
    viewManagerData: MapViewManagerData
) : MapViewManager<MapView, MyLocationConfiguration, Marker, Polyline, Polygon, LatLng, MapStatusUpdate, MapStatus>(
    lifecycleOwner, mapView, viewManagerData
) {

    constructor(fragment: Fragment, mapView: MapView, viewManagerData: MapViewManagerData) : this(
        fragment.viewLifecycleOwner,
        mapView,
        viewManagerData
    )

    // 定位
    private var baiduMapLocator: BaiduMapLocator? = null

    // 接收定位回调
    var onReceiveLocation: ((location: BDLocation?) -> Void)? = null


    init {
        // 加载完成回调
        mapView.map.setOnMapLoadedCallback {
            isMapLoaded = true
            // 通知回调
            onMapLoaded?.invoke()
            // 更新中心点（固定）
            updateCenterFixedMarker()
        }

        // 设置生命周期感知，设置生命周期后可以不需要再在onDestroy、onResume、onPause中进行回调
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                mapView.onDestroy()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                mapView.onResume()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                viewManagerData.savePauseCameraPosition(
                    mapView.map.mapStatus.target.toMapPosition,
                    mapView.map.mapStatus.zoom,
                    mapView.map.mapStatus.overlook,
                    mapView.map.mapStatus.rotate
                )
                mapView.onPause()
            }
        })

    }


    /**
     * 停止监听定位
     */
    override fun stopLocateMyLocation() {
        baiduMapLocator?.stop()
    }

    /**
     * 定位到我的位置
     */
    override fun locateMyLocation(
        isLocateMyLocationOnlyWhenFirst: Boolean,
        isMoveCameraOnlyWhenFirst: Boolean,
        isFirstFlagFromViewModel: Boolean,
        applyCustomMyLocation: ((MyLocationConfiguration) -> MyLocationConfiguration)?
    ) {
        // 定位跟随态
        var firstType = MyLocationConfiguration.LocationMode.FOLLOWING
        var followUpType: MyLocationConfiguration.LocationMode?
        if (isLocateMyLocationOnlyWhenFirst) {
            // 定位一次，且将视角移动到地图中心点
            followUpType = null
        } else {
            followUpType =
                if (isMoveCameraOnlyWhenFirst)
                    MyLocationConfiguration.LocationMode.NORMAL // 普通态，连续定位但不移动地图位置
                else firstType
        }
        locateMyLocationByType(
            firstType,
            followUpType,
            isFirstFlagFromViewModel,
            applyCustomMyLocation
        )
    }

    /**
     * 定位到我的位置
     */
    fun locateMyLocationByType(
        firstType: MyLocationConfiguration.LocationMode = MyLocationConfiguration.LocationMode.FOLLOWING, //第一次定位类型
        followUpType: MyLocationConfiguration.LocationMode? = MyLocationConfiguration.LocationMode.NORMAL,//后续定位类型
        isFirstFlagFromViewModel: Boolean = true, // 是否首次加载的标志从viewmodel进行获取，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType
        applyCustomMyLocation: ((MyLocationConfiguration) -> MyLocationConfiguration)? = null,
        applyCustomLocationClientOption: ((LocationClientOption) -> Void)? = null,
        onReceiveLocation: ((location: BDLocation?) -> Void)? = null
    ) {
        this.onReceiveLocation = onReceiveLocation
        val initType =
            if (isFirstFlagFromViewModel && !viewManagerData.isfirstLoad) followUpType else firstType
        mapView.map.setMyLocationConfiguration(MyLocationConfiguration(initType, true, null).let {
            applyCustomMyLocation?.invoke(it) ?: it
        })
        mapView.map.isMyLocationEnabled = true
        if (isFirstFlagFromViewModel && !viewManagerData.isfirstLoad) {
            // 首次加载模式，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType
            viewManagerData.cameraPositionTarget?.run {
                // 返回到暂停时保存的状态
                moveCamera(
                    moveCameraData = MapStatusUpdateFactory.newMapStatus(
                        MapStatus.Builder()
                            .target(this.toBaidu)
                            .zoom(viewManagerData.cameraPositionZoom!!)
                            .overlook(viewManagerData.cameraPositionTilt!!)
                            .rotate(viewManagerData.cameraPositionBearing!!)
                            .build()
                    )
                )
            }
        }
        viewManagerData.onFirstLoadComplete()
        baiduMapLocator =
            BaiduMapLocator(mapView.context, applyCustomLocationClientOption, true).apply {
                lifecycleOwner = this@BaiduMapViewManager.lifecycleOwner
                onLocationChanged = {
                    var location = it.extraData as? BDLocation
                    // 设置后续定位类型（如果和第一次定位类型不一致的话）
                    if (mapView.map.locationConfiguration.locationMode != followUpType) {
                        // 把获取到的当前位置设置到地图上
                        setMyLocationData(location)
                        mapView.map.setMyLocationConfiguration(
                            MyLocationConfiguration(
                                followUpType
                                    ?: MyLocationConfiguration.LocationMode.NORMAL, //为空时设置为普通态
                                true,
                                null
                            ).let {
                                applyCustomMyLocation?.invoke(it) ?: it
                            })
                    } else if (followUpType != null) { //为空时表示不再显示定位
                        // 把获取到的当前位置设置到地图上
                        setMyLocationData(location)
                    }

                    // 回调
                    this@BaiduMapViewManager.onReceiveLocation?.invoke(location)
                }
                start()
            }

    }

    /**
     * 设置我的定位位置
     */
    private fun setMyLocationData(location: BDLocation?) {
        if (location == null) return
        mapView.map.setMyLocationData(
            MyLocationData.Builder()
                .accuracy(location.radius)
                .direction(location.direction)
                .latitude(location.latitude)
                .longitude(location.longitude)
                .build()
        )
    }

    /**
     * 移动地图到指定位置
     */
    override fun moveCamera(
        mapPosition: MapPosition?,
        moveCameraData: MapStatusUpdate?,
        moveCameraAnimationDuration: Long,
        onCompletionCallback: (() -> Unit)?
    ) {
        val cameraData =
            moveCameraData ?: mapPosition?.toBaidu?.let { MapStatusUpdateFactory.newLatLng(it) }
            ?: return
        if (moveCameraAnimationDuration <= 0) {
            mapView.map.setMapStatus(cameraData)
            onCompletionCallback?.invoke()
        } else {
            mapView.map.animateMapStatus(cameraData, moveCameraAnimationDuration.toInt())
            if (onCompletionCallback != null)
                Handler().postDelayed(
                    { onCompletionCallback.invoke() },
                    moveCameraAnimationDuration
                )
        }
    }

    /**
     * 返回地图的当前场景信息（层级、坐标等）
     */
    override fun getCamera(): MapStatus = mapView.map.mapStatus


    /**
     * 更新中心点（固定），注意该更新操作将以centerFixedMarkerEnabled为依据
     */
    override fun updateCenterFixedMarker() {
        if (!centerFixedMarkerEnabled) {
            // 禁用时，删除中心点（固定）
            if (centerFixedMarker != null) {
                centerFixedMarker?.remove()
                centerFixedMarker = null
            }
            return
        }
        // 启用时，先判断是否为空，避免重复创建
        if (centerFixedMarker != null && !centerFixedMarker!!.isRemoved) {
            return
        }
        // 未加载完成时停止创建
        if (!isMapLoaded) {
            return
        }
//        // 开始创建
        val screenPosition =
            mapView.map.projection.toScreenLocation(mapView.map.mapStatus.target)
        centerFixedMarker = mapView.map.addOverlay(
            MarkerOptions()
                .position(mapView.map.mapStatus.target)
                .anchor(0.5f, 0.5f)
                .apply {
                    zIndex(ZINDEX_CENTER_FIXED_MARKER.toInt())
                    icon(R.drawable.purple_pin.newBaiduIcon(height = 88.dp))
                    centerFixedMarkerApplyCustomOptions?.invoke(null, this)
                }) as Marker?
        //设置Marker在屏幕上,不跟随地图移动
        centerFixedMarker?.setFixedScreenPosition(Point(screenPosition.x, screenPosition.y))
    }

    /**
     * 重置点坐标
     */
    override fun setMarkerPosition(marker: Marker, position: MapPosition) {
        marker.position = position.toBaidu
    }

    /**
     * 重置地图缓存数据
     */
    override fun mapViewInvalidate() = mapView.invalidate()


    /**
     * 清空所有数据
     */
    override fun clearAll() {
        mapView.map.clear()
        super.clearAll()
    }

    /**
     * 删除线
     */
    override fun removePolyline(polyline: Polyline) {
        polyline.remove()
        polylines.remove(polyline.id)
    }

    /**
     * 删除多边形
     */
    override fun removePolygon(polygon: Polygon) {
        polygon.remove()
        polygons.remove(polygon.id)
    }


    /**
     * 添加或刷新线
     */
    override fun addOrUpdatePolyline(mapPositionGroup: MapPositionGroup) {
        if (mapPositionGroup.mapPositions.size <= 1) {
            removePolyline(mapPositionGroup, false)
            return
        }
        if (mapPositionGroup.groupID.isNullOrBlank() || !polylines.containsKey(mapPositionGroup.groupID!!)) {
            val polyline = mapView.map.addOverlay(
                PolylineOptions().points(mapPositionGroup.mapPositions.map { it.toBaidu })
                    .apply {
                        zIndex(ZINDEX_POLYLINE.toInt())
                        // 应用自定义样式
                        globalApplyCustomOptions?.invoke(mapPositionGroup, this)
                        mapPositionGroup.applyCustomOptions?.invoke(mapPositionGroup, this)
//                        .color(R.color.colorAccent.color).width(4f).zIndex(900f)
                        addNewID()
                    }
            ) as Polyline
            polyline.id?.run {
                mapPositionGroup.setGroupID(this)
                polylines[this] = polyline
            }
        } else {
            polylines[mapPositionGroup.groupID ?: return]?.points =
                mapPositionGroup.mapPositions.map { it.toBaidu }
        }
    }

    /**
     * 添加或刷新多边形
     */
    override fun addOrUpdatePolygons(mapPositionGroup: MapPositionGroup) {
        // 低于3个时画线
        if (mapPositionGroup.mapPositions.size < 3) {
            removePolygon(mapPositionGroup, false)
            addOrUpdatePolyline(mapPositionGroup)
            return
        }
        // 大于等于3个时画多边形
        removePolyline(mapPositionGroup, false)
        if (mapPositionGroup.groupID.isNullOrBlank() || !polygons.containsKey(mapPositionGroup.groupID!!)) {
            val polygon = mapView.map.addOverlay(
                PolygonOptions().points(mapPositionGroup.mapPositions.map { it.toBaidu })
                    .apply {
                        zIndex(ZINDEX_POLYGON.toInt())
                        // 应用自定义样式
                        globalApplyCustomOptions?.invoke(mapPositionGroup, this)
                        mapPositionGroup.applyCustomOptions?.invoke(mapPositionGroup, this)
                        addNewID()
                    }
//                    .fillColor(
//                        R.color.tmchartblue.color
//                    ).strokeColor(R.color.colorAccent.color).strokeWidth(4f).zIndex(900f)
            ) as Polygon
            polygon.id?.run {
                mapPositionGroup.setGroupID(this)
                polygons[this] = polygon
            }
        } else {
            polygons[mapPositionGroup.groupID ?: return]?.points =
                mapPositionGroup.mapPositions.map { it.toBaidu }
        }
    }

    /**
     * 返回中心点坐标
     */
    override fun getCenterFixedPosition(): MapPosition = mapView.map.mapStatus.target.toMapPosition

    /**
     * 返回左上角坐标
     */
    override fun getLeftTopFixedPosition(): MapPosition = MapPosition(
        longitude = getLeftBottomFixedPosition().longitude,
        latitude = getRightTopFixedPosition().latitude,
        type = MapPositionType.BD09LL
    )

    /**
     * 返回左下角坐标
     */
    override fun getLeftBottomFixedPosition(): MapPosition =
        mapView.map.mapStatus.bound.southwest.toMapPosition

    /**
     * 返回右上角坐标
     */
    override fun getRightTopFixedPosition(): MapPosition =
        mapView.map.mapStatus.bound.northeast.toMapPosition

    /**
     * 返回右下角坐标
     */
    override fun getRightBottomFixedPosition(): MapPosition = MapPosition(
        longitude = getRightTopFixedPosition().longitude,
        latitude = getLeftBottomFixedPosition().latitude,
        type = MapPositionType.BD09LL
    )

    /**
     * 添加点（若mapPositionGroup不为空则同时更新数据到MapPositionGroup）
     */
    override fun addMarker(
        position: MapPosition,
        mapPositionGroup: MapPositionGroup?,
        applyCustomOptions: ApplyCustomOptions?
    ): String {
        val marker = mapView.map.addOverlay(
            MarkerOptions().position(position.toBaidu).apply {
                zIndex(ZINDEX_MARKER.toInt())
                icon(R.drawable.icon_gcoding.newBaiduIcon(height = 38.dp))
                globalApplyCustomOptions?.invoke(mapPositionGroup, this)
                (applyCustomOptions ?: mapPositionGroup?.applyCustomOptions)?.invoke(
                    mapPositionGroup,
                    this
                )
            }
        ) as Marker
        position.id = marker.id
        // 避免重复添加
        if (mapPositionGroup?.mapPositions?.contains(position) == false) {
            mapPositionGroup.addMapPosition(position)
        }
        // 创建和组的关联关系
        if (mapPositionGroup != null) {
            associateMarkerToGroup(mapPositionGroup, marker.id)
        }
        markers[marker.id] = marker
        return marker.id
    }


    /**
     * 删除点
     */
    override fun removeMarker(marker: Marker) {
        marker.remove()
        markers.remove(marker.id)
    }

    /**
     * 计算长度
     */
    override fun calculateLineDistance(mapPositionGroup: MapPositionGroup): Double =
        calculateBaiduLineDistance(mapPositionGroup)

    /**
     * 计算面积
     */
    override fun calculateArea(mapPositionGroup: MapPositionGroup): Double =
        calculateBaiduArea(mapPositionGroup)

    /**
     * 比较坐标是否一致
     */
    override fun equaltLatLng(latLng1: LatLng, latLng2: LatLng, decimalPlaces: Int?): Boolean =
        equaltBaiduLatLng(latLng1, latLng2, decimalPlaces)
}