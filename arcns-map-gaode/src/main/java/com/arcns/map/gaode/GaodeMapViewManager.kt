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
import com.arcns.core.map.*
import com.arcns.core.util.dp
import java.net.URL



/**
 * 高德地图管理器
 */
class GaodeMapViewManager(
    lifecycleOwner: LifecycleOwner,
    mapView: MapView,
    viewModel: MapViewManagerViewModel
) : MapViewManager<MapView, MyLocationStyle, Marker, Polyline, Polygon, LatLng>(
    lifecycleOwner,
    mapView,
    viewModel
) {

    constructor(fragment: Fragment, mapView: MapView, viewModel: MapViewManagerViewModel) : this(
        fragment.viewLifecycleOwner,
        mapView,
        viewModel
    )

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
    override fun locateMyLocation(
        isLocateMyLocationOnlyWhenFirst: Boolean,
        isMoveCameraOnlyWhenFirst: Boolean,
        isFirstFlagFromViewModel: Boolean,
        applyCustomMyLocationStyle: ((MyLocationStyle) -> MyLocationStyle)?
    ) {
        // 连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）高德默认执行此种模式
        var firstType = MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE
        var followUpType: Int
        if (isLocateMyLocationOnlyWhenFirst) {
            // 定位一次，且将视角移动到地图中心点
            firstType = MyLocationStyle.LOCATION_TYPE_LOCATE
            followUpType = firstType
        } else {
            followUpType =
                if (isMoveCameraOnlyWhenFirst)
                    MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER // 连续定位但不移动地图位置
                else firstType
        }
        locateMyLocationByType(firstType,followUpType,isFirstFlagFromViewModel,applyCustomMyLocationStyle)
    }

    /**
     * 定位到我的位置
     */
    fun locateMyLocationByType(
        firstType: Int = MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE, //第一次定位类型
        followUpType: Int = MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER,//后续定位类型
        isFirstFlagFromViewModel: Boolean = true, // 是否首次加载的标志从viewmodel进行获取，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType
        applyCustomMyLocationStyle: ((MyLocationStyle) -> MyLocationStyle)? = null
    ) {
        val initType =
            if (isFirstFlagFromViewModel && !viewModel.isfirstLoad) followUpType else firstType
        mapView.map.myLocationStyle = MyLocationStyle().let {
            (applyCustomMyLocationStyle?.invoke(it) ?: it).myLocationType(initType)
        }
        mapView.map.isMyLocationEnabled = true
        if (isFirstFlagFromViewModel && !viewModel.isfirstLoad) {
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
                // 设置后续定位类型（如果和第一次定位类型不一致的话）
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
        // 开始创建
        val screenPosition =
            mapView.map.projection.toScreenLocation(mapView.map.cameraPosition.target)
        centerFixedMarker = mapView.map.addMarker(
            MarkerOptions().anchor(0.5f, 0.5f).apply {
                zIndex(ZINDEX_CENTER_FIXED_MARKER)
                icon(R.drawable.purple_pin.newGaodeIcon(height = 88.dp))
                centerFixedMarkerApplyCustomOptions?.invoke(null, this)
            }
        )
        //设置Marker在屏幕上,不跟随地图移动
        centerFixedMarker?.setPositionByPixels(screenPosition.x, screenPosition.y)
    }

    /**
     * 重置点坐标
     */
    override fun setMarkerPosition(marker: Marker, position: MapPosition) {
        marker.position = position.toGaoDe
    }

    /**
     * 重置地图缓存数据
     */
    override fun mapViewInvalidate() = mapView.invalidate()

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
        if (mapPositionGroup.groupID.isNullOrBlank()) {
            val polyline = mapView.map.addPolyline(
                PolylineOptions().addAll(mapPositionGroup.mapPositions.map { it.toGaoDe })
                    .apply {
                        zIndex(ZINDEX_POLYLINE)
                        // 应用自定义样式
                        globalApplyCustomOptions?.invoke(mapPositionGroup, this)
                        mapPositionGroup.applyCustomOptions?.invoke(mapPositionGroup,this)
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
                        zIndex(ZINDEX_POLYGON)
                        // 应用自定义样式
                        globalApplyCustomOptions?.invoke(mapPositionGroup,this)
                        mapPositionGroup.applyCustomOptions?.invoke(mapPositionGroup,this)
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
                zIndex(ZINDEX_MARKER)
                icon(R.drawable.icon_gcoding.newGaodeIcon(height = 38.dp))
                globalApplyCustomOptions?.invoke(mapPositionGroup, this)
                (applyCustomOptions ?: mapPositionGroup?.applyCustomOptions)?.invoke(
                    mapPositionGroup,
                    this
                )
            }
        )
        position.id = marker.id
        if (mapPositionGroup?.mapPositions?.contains(position) == false) {
            // 避免重复添加
            mapPositionGroup.addMapPosition(position)
        }
        markers[marker.id] = marker
        return marker.id
    }

    /**
     * 删除点
     */
    override fun removeMarker(marker: Marker) = marker.remove()


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
        var lineDistance = 0.0
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
    override fun calculateArea(mapPositionGroup: MapPositionGroup): Double =
        AMapUtils.calculateArea(mapPositionGroup.mapPositions.map {
            it.toGaoDe
        }).toDouble()


}