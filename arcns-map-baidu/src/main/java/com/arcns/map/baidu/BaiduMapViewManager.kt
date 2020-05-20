package com.arcns.map.baidu

import android.graphics.Point
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.arcns.core.APP
import com.arcns.core.map.*
import com.arcns.core.util.bitmap
import com.arcns.core.util.dp
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.AreaUtil
import com.baidu.mapapi.utils.DistanceUtil
import java.util.*
import kotlin.collections.HashMap

/**
 * 百度地图管理器
 */
class BaiduMapViewManager(
    lifecycleOwner: LifecycleOwner,
    mapView: MapView,
    viewModel: MapViewManagerViewModel
) : MapViewManager<MapView, MyLocationConfiguration, Marker, Polyline, Polygon, LatLng>(
    lifecycleOwner, mapView, viewModel
) {

    constructor(fragment: Fragment, mapView: MapView, viewModel: MapViewManagerViewModel) : this(
        fragment.viewLifecycleOwner,
        mapView,
        viewModel
    )

    // 定位
    private var locationClient: LocationClient? = null

    // 接收定位回调
    var onReceiveLocation: ((location: BDLocation?) -> Void)? = null


    init {
        // 加载完成回调
        mapView.map.setOnMapLoadedCallback {
            isMapLoaded = true
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
                if (locationClient?.isStarted == false) {
                    locationClient?.start()
                }
                mapView.onResume()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                if (locationClient?.isStarted == true) {
                    locationClient?.stop()
                }
                viewModel.savePauseCameraPosition(
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
        var followUpType: MyLocationConfiguration.LocationMode? = firstType
        if (isLocateMyLocationOnlyWhenFirst) {
            // 定位一次，且将视角移动到地图中心点
            followUpType = null
        } else {
            followUpType =
                if (isMoveCameraOnlyWhenFirst)
                    MyLocationConfiguration.LocationMode.NORMAL // 普通态，连续定位但不移动地图位置
                else firstType
        }
        locateMyLocation(firstType, followUpType, isFirstFlagFromViewModel, applyCustomMyLocation)
    }

    /**
     * 定位到我的位置
     */
    fun locateMyLocation(
        firstType: MyLocationConfiguration.LocationMode = MyLocationConfiguration.LocationMode.FOLLOWING, //第一次定位类型
        followUpType: MyLocationConfiguration.LocationMode? = MyLocationConfiguration.LocationMode.NORMAL,//后续定位类型
        isFirstFlagFromViewModel: Boolean = true, // 是否首次加载的标志从viewmodel进行获取，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType
        applyCustomMyLocation: ((MyLocationConfiguration) -> MyLocationConfiguration)? = null,
        applyCustomLocationClientOption: ((LocationClientOption) -> Void)? = null,
        onReceiveLocation: ((location: BDLocation?) -> Void)? = null
    ) {
        this.onReceiveLocation = onReceiveLocation
        val initType =
            if (isFirstFlagFromViewModel && !viewModel.isfirstLoad) followUpType else firstType
        mapView.map.setMyLocationConfiguration(MyLocationConfiguration(initType, true, null).let {
            applyCustomMyLocation?.invoke(it) ?: it
        })
        mapView.map.isMyLocationEnabled = true
        if (isFirstFlagFromViewModel && !viewModel.isfirstLoad) {
            // 首次加载模式，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType
            viewModel.cameraPositionTarget?.run {
                // 返回到暂停时保存的状态
                mapView.map.setMapStatus(
                    MapStatusUpdateFactory.newMapStatus(
                        MapStatus.Builder()
                            .target(this.toBaidu)
                            .zoom(viewModel.cameraPositionZoom!!)
                            .overlook(viewModel.cameraPositionTilt!!)
                            .rotate(viewModel.cameraPositionBearing!!)
                            .build()
                    )
                )
            }
            return
        }
        viewModel.onFirstLoadComplete()
        if (initType == followUpType) {
            // 第一次定位类型与后续定位类型一致
            return
        }
        locationClient = LocationClient(mapView.context).apply {
            locOption = LocationClientOption().apply {
                openGps = true
                coorType = "bd09ll"
                scanSpan = 1000
                setIsNeedAddress(true)
                applyCustomLocationClientOption?.invoke(this)
            }
            registerLocationListener(object : BDAbstractLocationListener() {
                override fun onReceiveLocation(location: BDLocation?) {
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
            })
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
        if (centerFixedMarker != null) {
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
                    centerFixedMarkerApplyCustomOptions?.invoke(this)
                }) as Marker?
        //设置Marker在屏幕上,不跟随地图移动
        centerFixedMarker?.setFixedScreenPosition(Point(screenPosition.x, screenPosition.y))
    }


    /**
     * 清空所有数据
     */
    override fun clear() {
        mapView.map.clear()
        super.clear()
    }

    /**
     * 删除线
     */
    override fun removePolyline(polyline: Polyline) = polyline.remove()

    /**
     * 删除多边形
     */
    override fun removePolygon(polygon: Polygon) = polygon.remove()


    /**
     * 添加或刷新线
     */
    override fun addOrUpdatePolyline(mapPositionGroup: MapPositionGroup) {
        if (mapPositionGroup.mapPositions.size <= 1) {
            removePolyline(mapPositionGroup, false)
            return
        }
        if (mapPositionGroup.groupID.isNullOrBlank()) {
            val polyline = mapView.map.addOverlay(
                PolylineOptions().points(mapPositionGroup.mapPositions.map { it.toBaidu })
                    .apply {
                        zIndex(ZINDEX_POLYLINE.toInt())
                        // 应用自定义样式
                        mapPositionGroup.applyCustomOptions?.invoke(this)
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
        if (mapPositionGroup.groupID.isNullOrBlank()) {
            val polygon = mapView.map.addOverlay(
                PolygonOptions().points(mapPositionGroup.mapPositions.map { it.toBaidu })
                    .apply {
                        zIndex(ZINDEX_POLYGON.toInt())
                        // 应用自定义样式
                        mapPositionGroup.applyCustomOptions?.invoke(this)
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
     * 添加或更新点（注意，如果数据集合未包含该点的id，则会在添加后把id赋值给对象）
     */
    override fun addOrUpdateMarker(
        mapPosition: MapPosition,
        mapPositionGroup: MapPositionGroup?
    ) {
        if (markers.containsKey(mapPosition.id)) {
            markers[mapPosition.id]?.position = mapPosition.toBaidu
        } else {
            mapPosition.id =
                addMarker(
                    position = mapPosition,
                    applyCustomOptions = mapPositionGroup?.applyCustomOptions
                )
        }
    }

    /**
     * 添加中心点（固定）的坐标到坐标组
     */
    override fun addCenterFixedMarker(mapPositionGroup: MapPositionGroup): String? {
        return addMarker(
            mapView.map.mapStatus.target?.toMapPosition ?: return null,
            mapPositionGroup
        )
    }

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
                applyCustomOptions?.invoke(this)
            }
        ) as Marker
        mapPositionGroup?.addMapPosition(marker.position.toMapPosition.apply {
            id = marker.id
        })
        markers[marker.id] = marker
        return marker.id
    }

    /**
     * 删除最后一个点（同时更新数据到MapPositionGroup）
     */
    override fun removeLastMarker(mapPositionGroup: MapPositionGroup) {
        mapPositionGroup.removeMapPosition()?.run {
            removeMarker(id)
        }
    }

    /**
     * 删除点（同时更新数据到MapPositionGroup）
     */
    override fun removeMarker(id: String?, mapPositionGroup: MapPositionGroup?) {
        mapPositionGroup?.removeMapPosition(id)
        markers[id]?.remove()
        mapView.invalidate()
    }

    /**
     * 删除多个点（同时更新数据到MapPositionGroup）
     */
    override fun removeMarkers(mapPositionGroup: MapPositionGroup) {
        mapPositionGroup.mapPositions.forEach {
            markers[it.id]?.remove()
        }
        mapPositionGroup.clearMapPosition()
        mapView.invalidate()
    }

    /**
     * 计算长度
     */
    override fun calculateLineDistance(mapPositionGroup: MapPositionGroup): Double {
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
     * 计算面积
     */
    override fun calculateArea(mapPositionGroup: MapPositionGroup): Double =
        AreaUtil.calculateArea(mapPositionGroup.mapPositions.map {
            it.toBaidu
        })
}