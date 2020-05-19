package com.arcns.map.gaode

import android.location.Location
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.amap.api.maps.AMap
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.arcns.core.APP
import com.arcns.core.util.*
import com.arcns.map.gaode.R
import java.net.URL

/**
 * 高德地图视图管理器
 */
class GaoDeMapViewManager(
    lifecycleOwner: LifecycleOwner,
    val mapView: MapView,
    val viewModel: MapViewManagerViewModel
) : IMapViewManager {
    constructor(fragment: Fragment, mapView: MapView, viewModel: MapViewManagerViewModel) : this(
        fragment.viewLifecycleOwner,
        mapView,
        viewModel
    )

    // 是否加载完成
    var isMapLoaded = false

    // 点、线、面
    val markers = HashMap<String, Marker>()
    val polylines = HashMap<String, Polyline>()
    val polygons = HashMap<String, Polygon>()

    // 中心点（固定）
    var centerFixedMarker: Marker? = null
        private set
    var centerFixedMarkerApplyCustomOptions: ApplyCustomOptions? = null
    override var centerFixedMarkerEnabled: Boolean = false
        set(value) {
            field = value
            updateCenterFixedMarker()
        }


    // 当前谷歌图层
    private var currentGoogleTileOverlay: TileOverlay? = null

    init {
        // 加载完成回调
        mapView.map.addOnMapLoadedListener(object : AMap.OnMapLoadedListener {
            override fun onMapLoaded() {
                isMapLoaded = true
                // 更新中心点（固定）
                updateCenterFixedMarker()
                mapView.map.removeOnMapLoadedListener(this)
            }
        })

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
                viewModel.savePauseCameraPosition(
                    mapView.map.cameraPosition.target.toMapPosition,
                    mapView.map.cameraPosition.zoom,
                    mapView.map.cameraPosition.tilt,
                    mapView.map.cameraPosition.bearing
                )
                mapView.onPause()
            }
        })
    }

    /**
     * 定位到我的位置
     */
    fun locateMyLocation(
        firstType: Int = MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE, //第一次定位类型
        followUpType: Int = firstType,//后续定位类型
        applyCustomMyLocationStyle: ((MyLocationStyle) -> Unit)? = null,
        firstLoadMode: Boolean = true // 首次加载模式，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType
    ) {
        val initType = if (firstLoadMode && !viewModel.isfirstLoad) followUpType else firstType
        mapView.map.myLocationStyle = MyLocationStyle().apply {
            applyCustomMyLocationStyle?.invoke(this)
            myLocationType(initType)
        }
        mapView.map.isMyLocationEnabled = true
        if (firstLoadMode && !viewModel.isfirstLoad) {
            // 首次加载模式，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType

            viewModel.cameraPositionTarget?.run {
                // 返回到暂停时保存的状态
                mapView.map.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition(
                            this.toGaoDe,
                            viewModel.cameraPositionZoom!!,
                            viewModel.cameraPositionTilt!!,
                            viewModel.cameraPositionBearing!!
                        )
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
        mapView.map.addOnMyLocationChangeListener(object : AMap.OnMyLocationChangeListener {
            override fun onMyLocationChange(location: Location?) {
                if (mapView.map.myLocationStyle.myLocationType != followUpType) {
                    mapView.map.myLocationStyle = MyLocationStyle().apply {
                        applyCustomMyLocationStyle?.invoke(this)
                        myLocationType(followUpType)
                    }
                }
                mapView.map.removeOnMyLocationChangeListener(this)
            }
        })
    }

    /**
     * 更新中心点（固定）
     */
    private fun updateCenterFixedMarker() {
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
        // 开始创建
        val screenPosition =
            mapView.map.projection.toScreenLocation(mapView.map.cameraPosition.target)
        centerFixedMarker = mapView.map.addMarker(
            MarkerOptions().anchor(0.5f, 0.5f).apply {
                if (centerFixedMarkerApplyCustomOptions != null) {
                    centerFixedMarkerApplyCustomOptions?.invoke(this)
                } else {
                    icon(BitmapDescriptorFactory.fromResource(R.drawable.purple_pin))
                }
            }
        )
        //设置Marker在屏幕上,不跟随地图移动
        centerFixedMarker?.setPositionByPixels(screenPosition.x, screenPosition.y)
    }


    /**
     * 清空所有数据
     */
    override fun clear() {
        mapView.isEnabled
        mapView.map.clear()
        markers.clear()
        polygons.clear()
        polygons.clear()
    }

    /**
     * 按传入的新数据进行清空
     */
    override fun clear(
        markerMapPositionGroups: List<MapPositionGroup>?,
        polygonMapPositionGroups: List<MapPositionGroup>?,
        polylineMapPositionGroups: List<MapPositionGroup>?
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
    override fun refresh(
        markerMapPositionGroups: List<MapPositionGroup>?,
        polygonMapPositionGroups: List<MapPositionGroup>?,
        polylineMapPositionGroups: List<MapPositionGroup>?
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
    override fun removePolyline(mapPositionGroup: MapPositionGroup, isRemoveMarkers: Boolean) {
        polylines[mapPositionGroup.groupID]?.run {
            remove()
            polylines.remove(mapPositionGroup.groupID)
            mapPositionGroup.clearGroupID()
            if (isRemoveMarkers) {
                removeMarkers(mapPositionGroup)
            }
        }
    }

    /**
     * 删除多边形
     */
    override fun removePolygon(mapPositionGroup: MapPositionGroup, isRemoveMarkers: Boolean) {
        // 状态为线时
        if (polylines.containsKey(mapPositionGroup.groupID)) {
            removePolyline(mapPositionGroup, isRemoveMarkers)
            return
        }
        // 状态为多边形时
        polygons[mapPositionGroup.groupID]?.run {
            remove()
            polygons.remove(mapPositionGroup.groupID)
            mapPositionGroup.clearGroupID()
            if (isRemoveMarkers) {
                removeMarkers(mapPositionGroup)
            }
        }

    }

    /**
     * 添加或刷新线
     */
    override fun addOrUpdatePolyline(mapPositionGroup: MapPositionGroup) {
        if (mapPositionGroup.groupID.isNullOrBlank()) {
            val polyline = mapView.map.addPolyline(
                PolylineOptions().addAll(mapPositionGroup.mapPositions.map { it.toGaoDe })
                    .apply {
                        // 应用自定义样式
                        mapPositionGroup.applyCustomOptions?.invoke(this)
//                        .color(R.color.colorAccent.color).width(4f).zIndex(900f)
                    }
            )
            mapPositionGroup.setGroupID(polyline.id)
            polylines[polyline.id] = polyline
        } else {
            polylines[mapPositionGroup.groupID ?: return]?.points =
                mapPositionGroup.mapPositions.map { it.toGaoDe }
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
            val polygon = mapView.map.addPolygon(
                PolygonOptions().addAll(mapPositionGroup.mapPositions.map { it.toGaoDe })
                    .apply {
                        // 应用自定义样式
                        mapPositionGroup.applyCustomOptions?.invoke(this)
                    }
//                    .fillColor(
//                        R.color.tmchartblue.color
//                    ).strokeColor(R.color.colorAccent.color).strokeWidth(4f).zIndex(900f)
            )
            mapPositionGroup.setGroupID(polygon.id)
            polygons[polygon.id] = polygon
        } else {
            polygons[mapPositionGroup.groupID ?: return]?.points =
                mapPositionGroup.mapPositions.map { it.toGaoDe }
        }
    }

    /**
     * 添加或更新多个点
     */
    override fun addOrUpdateMarkers(mapPositionGroup: MapPositionGroup) {
        mapPositionGroup.setMapPositions(mapPositionGroup.mapPositions.apply {
            forEach {
                addOrUpdateMarker(it, mapPositionGroup)
            }
        })
    }

    /**
     * 添加或更新点（注意，如果数据集合未包含该点的id，则会在添加后把id赋值给对象）
     */
    override fun addOrUpdateMarker(
        mapPosition: MapPosition,
        mapPositionGroup: MapPositionGroup?
    ) {
        if (markers.containsKey(mapPosition.id)) {
            markers[mapPosition.id]?.position = mapPosition.toGaoDe
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
            centerFixedMarker?.position?.toMapPosition ?: return null,
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
        val marker = mapView.map.addMarker(
            MarkerOptions().position(position.toGaoDe).apply {
                applyCustomOptions?.invoke(this)
            }
        )
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
     * 设置谷歌瓦片图层
     * 地址 lyrs参数:
     * m：谷歌交通图
     * t：谷歌地形图
     * p：带标签的谷歌地形图
     * s：谷歌卫星图
     * y：带标签的谷歌卫星图
     * h：谷歌标签层（路名、地名等）
     */
    fun setGoogleTileOverlay(lyrs: String) {
        clearGoogleTileOverlay()
        val url = "https://mt3.google.cn/maps/vt?lyrs=%s@167000000&hl=zh-CN&gl=cn&x=%d&y=%d&z=%d"
        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
                return URL(String.format(url, lyrs, x, y, zoom))
            }
        }
        currentGoogleTileOverlay =
            mapView.map.addTileOverlay(
                TileOverlayOptions().tileProvider(tileProvider).diskCacheEnabled(true)
                    .diskCacheDir(APP.INSTANCE.cacheDir?.absoluteFile.toString())
                    .diskCacheSize(100000)
                    .memoryCacheEnabled(true)
                    .memCacheSize(100000)
            )
    }

    /**
     * 清空谷歌瓦片图层
     */
    fun clearGoogleTileOverlay() {
        if (currentGoogleTileOverlay != null) {
            currentGoogleTileOverlay?.remove()
            currentGoogleTileOverlay = null
        }
    }

    /**
     * 计算长度
     */
    override fun calculateLineDistance(mapPositionGroup: MapPositionGroup): Double {
        var lastPosition: MapPosition? = null
        var lineDistance: Double = 0.0
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
     * 计算面积
     */
    override fun calculateArea(mapPositionGroup: MapPositionGroup): Double {
        return AMapUtils.calculateArea(mapPositionGroup.mapPositions.map {
            it.toGaoDe
        }).toDouble()
    }

}
