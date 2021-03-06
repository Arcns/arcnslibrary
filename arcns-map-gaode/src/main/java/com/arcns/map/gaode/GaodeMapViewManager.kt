package com.arcns.map.gaode

import android.location.Location
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdate
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.amap.api.maps.model.MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER
import com.arcns.core.APP
import com.arcns.core.map.*
import com.arcns.core.util.dp
import java.net.URL
import kotlin.collections.map
import kotlin.collections.set


/**
 * 高德地图管理器
 */
class GaodeMapViewManager(
    lifecycleOwner: LifecycleOwner,
    mapView: MapView,
    viewManagerData: MapViewManagerData
) : MapViewManager<MapView, MyLocationStyle, Marker, Polyline, Polygon, LatLng, CameraUpdate, CameraPosition>(
    lifecycleOwner,
    mapView,
    viewManagerData
) {

    private var locationListener: AMap.OnMyLocationChangeListener? = null

    constructor(fragment: Fragment, mapView: MapView, viewManagerData: MapViewManagerData) : this(
        fragment.viewLifecycleOwner,
        mapView,
        viewManagerData
    )

    // 当前谷歌图层
    private var currentGoogleTileOverlay: TileOverlay? = null

    init {
        // 加载完成回调
        mapView.map.addOnMapLoadedListener(object : AMap.OnMapLoadedListener {
            override fun onMapLoaded() {
                isMapLoaded = true
                // 通知回调
                onMapLoaded?.invoke(viewManagerData.isFirstLoad)
                viewManagerData.onFirstLoadComplete()
                // 更新中心点（固定）
                updateCenterFixedMarker()
                mapView.map.removeOnMapLoadedListener(this)
            }
        })

        // 设置生命周期感知，设置生命周期后可以不需要再在onDestroy、onResume、onPause中进行回调
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                lifecycleOwner.lifecycle.removeObserver(this)
                saveDestroyCamera()
                onGarbageCollection()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                mapView.onResume()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                mapView.onPause()
            }
        })

    }

    /**
     * 回收管理器所引用的资源
     * 注意：该方法用于把_lifecycleOwner、_mapView等引用的对象都置为null或清空，方便系统回收资源，否则可能会引起内存泄漏
     * 该方法通常由实现类在生命周期onDestroy时调用，请勿在使用管理器过程中调用该方法
     */
    override fun onGarbageCollection(){
        stopLocateMyLocation()
        centerFixedMarker?.remove()
        mapView.onDestroy()
        super.onGarbageCollection()
    }

    /**
     * 保存暂停时的地图场景相关数据
     */
    override fun saveDestroyCamera() = viewManagerData.saveDestroyCamera(
        getCamera().target.toMapPosition,
        getCamera().zoom,
        getCamera().tilt,
        getCamera().bearing
    )


    /**
     * 恢复暂停时保存的地图场景相关数据
     */
    override fun resumeDestroyCamera() {
        viewManagerData.onConsumedDestroyCamera()
        viewManagerData.destroyCameraTarget?.run {
            // 返回到暂停时保存的状态
            moveCamera(
                moveCameraData = CameraUpdateFactory.newCameraPosition(
                    CameraPosition(
                        this.toGaoDe,
                        viewManagerData.destroyCameraZoom!!,
                        viewManagerData.destroyCameraTilt!!,
                        viewManagerData.destroyCameraBearing!!
                    )
                )
            )
        }
    }

    /**
     * 停止监听定位
     */
    override fun stopLocateMyLocation() {
        if (locationListener != null) {
            mapView.map.removeOnMyLocationChangeListener(locationListener)
            locationListener = null
        }
    }

    /**
     * 定位到我的位置
     */
    override fun locateMyLocation(
        isLocateMyLocationOnlyWhenFirst: Boolean, //仅定位到我的位置一次，如果为False则为连续定位
        isMoveCamera: Boolean,//是否跟随视觉到我的位置，如果为False则为只定位、不移动视觉，忽略isMoveCameraOnlyWhenFirst
        isMoveCameraOnlyWhenFirst: Boolean, //仅切换到我的位置一次，如果为False则视觉会一直跟随我的位置
        isPriorityResumeDestroyCamera: Boolean,// 是否优先恢复上次关闭时保存的视觉
        applyCustomMyLocationStyle: ((MyLocationStyle) -> MyLocationStyle)?// 自定义我的位置的配置和样式
    ) {
        var firstType =
            // 连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）高德默认执行此种模式
            if (isMoveCamera) MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE
            // //连续定位、蓝点不会移动到地图中心点，定位点依照设备方向旋转，并且蓝点会跟随设备移动。
            else MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER
        var followUpType: Int
        if (isLocateMyLocationOnlyWhenFirst) {
            firstType =
                    //定位一次，将视角移动到地图中心点
                if (isMoveCamera) MyLocationStyle.LOCATION_TYPE_LOCATE
                // 定位一次，不移动
                else MyLocationStyle.LOCATION_TYPE_SHOW
            followUpType = firstType
        } else {
            followUpType =
                if (isMoveCamera && isMoveCameraOnlyWhenFirst)
                    MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER // 连续定位但不移动地图位置
                else firstType
        }
        locateMyLocationByType(
            firstType,
            followUpType,
            isPriorityResumeDestroyCamera,
            applyCustomMyLocationStyle
        )
    }

    /**
     * 定位到我的位置
     */
    fun locateMyLocationByType(
        firstType: Int = MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE, //第一次定位类型
        followUpType: Int = MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER,//后续定位类型
        isPriorityResumeDestroyCamera: Boolean = true, // 是否首次加载的标志从viewmodel进行获取，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType
        applyCustomMyLocationStyle: ((MyLocationStyle) -> MyLocationStyle)? = null
    ) {
        val initType =
            if (isPriorityResumeDestroyCamera && viewManagerData.hasUnconsumedDestroyCamera) followUpType else firstType
        mapView.map.myLocationStyle = MyLocationStyle().let {
            (applyCustomMyLocationStyle?.invoke(it) ?: it).myLocationType(initType)
        }
        mapView.map.isMyLocationEnabled = true
        if (isPriorityResumeDestroyCamera && viewManagerData.hasUnconsumedDestroyCamera) {
            // 首次加载模式，如果为该模式则只会在页面首次加载时设置firstType，若页面非首次加载则设置为followUpType
            resumeDestroyCamera()
            return
        }
        if (initType == followUpType) {
            // 第一次定位类型与后续定位类型一致
            return
        }
        mapView.map.addOnMyLocationChangeListener(object : AMap.OnMyLocationChangeListener {
            init {
                locationListener = this
            }

            override fun onMyLocationChange(location: Location?) {
                // 设置后续定位类型（如果和第一次定位类型不一致的话）
                if (mapView.map.myLocationStyle.myLocationType != followUpType) {
                    mapView.map.myLocationStyle = MyLocationStyle().apply {
                        applyCustomMyLocationStyle?.invoke(this)
                        myLocationType(followUpType)
                    }
                }
            }
        })
    }

    /**
     * 移动地图到指定位置
     */
    override fun moveCamera(
        mapPosition: MapPosition?,
        moveCameraData: CameraUpdate?,
        moveCameraAnimationDuration: Long,
        onCompletionCallback: (() -> Unit)?
    ) {
        val cameraData =
            moveCameraData ?: mapPosition?.toGaoDe?.let { CameraUpdateFactory.newLatLng(it) }
            ?: return
        if (moveCameraAnimationDuration <= 0) {
            mapView.map.moveCamera(cameraData)
            onCompletionCallback?.invoke()
        } else {
            mapView.map.animateCamera(
                cameraData,
                moveCameraAnimationDuration,
                object : AMap.CancelableCallback {
                    override fun onFinish() {
                        onCompletionCallback?.invoke()
                    }

                    override fun onCancel() {
                        onCompletionCallback?.invoke()
                    }
                })
        }
    }

    /**
     * 返回地图的当前场景信息（层级、坐标等）
     */
    override fun getCamera(): CameraPosition = mapView.map.cameraPosition


    /**
     * 返回我的定位
     */
    override fun getMyLocationData(): MapPosition? = mapView.map.myLocation?.let {
        MapPosition(
            latitude = it.latitude,
            longitude = it.longitude,
            type = MapPositionType.BD09LL,
            extraData = it
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
        if (centerFixedMarker != null && !centerFixedMarker!!.isRemoved) {
            return
        }
        // 未加载完成时停止创建
        if (!isMapLoaded) {
            return
        }
        // 开始创建
        val screenPosition =
            mapView.map.projection.toScreenLocation(getCamera().target)
        centerFixedMarker = mapView.map.addMarker(
            MarkerOptions().anchor(0.5f, 0.5f).apply {
                zIndex(ZINDEX_CENTER_FIXED_MARKER)
                icon(R.drawable.purple_pin.newGaodeIcon(height = 88.dp))
                centerFixedMarkerApplyCustomOptions?.invoke(
                    null, this, getCamera().target.toMapPosition
                )
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
        if (mapPositionGroup.groupID.isNullOrBlank() || !polylines.containsKey(mapPositionGroup.groupID!!)) {
            val polyline = mapView.map.addPolyline(
                PolylineOptions().addAll(mapPositionGroup.mapPositions.map { it.toGaoDe })
                    .apply {
                        zIndex(ZINDEX_POLYLINE)
                        // 应用自定义样式
                        globalApplyCustomOptions?.invoke(mapPositionGroup, this, null)
                        mapPositionGroup.applyCustomOptions?.invoke(mapPositionGroup, this, null)
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
        if (mapPositionGroup.groupID.isNullOrBlank() || !polygons.containsKey(mapPositionGroup.groupID!!)) {
            val polygon = mapView.map.addPolygon(
                PolygonOptions().addAll(mapPositionGroup.mapPositions.map { it.toGaoDe })
                    .apply {
                        zIndex(ZINDEX_POLYGON)
                        // 应用自定义样式
                        globalApplyCustomOptions?.invoke(mapPositionGroup, this, null)
                        mapPositionGroup.applyCustomOptions?.invoke(mapPositionGroup, this, null)
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
     * 返回中心点坐标
     */
    override fun getCenterFixedPosition(): MapPosition =
        getCamera().target.toMapPosition

    /**
     * 返回左上角坐标
     */
    override fun getLeftTopFixedPosition(): MapPosition =
        mapView.map.projection.visibleRegion.farLeft.toMapPosition

    /**
     * 返回左下角坐标
     */
    override fun getLeftBottomFixedPosition(): MapPosition =
        mapView.map.projection.visibleRegion.nearLeft.toMapPosition

    /**
     * 返回右上角坐标
     */
    override fun getRightTopFixedPosition(): MapPosition =
        mapView.map.projection.visibleRegion.farRight.toMapPosition

    /**
     * 返回右下角坐标
     */
    override fun getRightBottomFixedPosition(): MapPosition =
        mapView.map.projection.visibleRegion.nearRight.toMapPosition

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
                globalApplyCustomOptions?.invoke(mapPositionGroup, this, position)
                (applyCustomOptions ?: mapPositionGroup?.applyCustomOptions)?.invoke(
                    mapPositionGroup,
                    this,
                    position
                )
            }
        )
        position.id = marker.id
        if (mapPositionGroup?.mapPositions?.contains(position) == false) {
            // 避免重复添加
            mapPositionGroup.addMapPosition(position)
            // 创建和组的关联关系
            if (mapPositionGroup != null) {
                associateMarkerToGroup(mapPositionGroup, marker.id)
            }
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
    override fun calculateLineDistance(mapPositionGroup: MapPositionGroup): Double =
        calculateGaodeLineDistance(mapPositionGroup)

    /**
     * 计算面积
     */
    override fun calculateArea(mapPositionGroup: MapPositionGroup): Double =
        calculateGaodeArea(mapPositionGroup)

    /**
     * 比较坐标是否一致
     */
    override fun equaltLatLng(
        latLng1: LatLng,
        latLng2: LatLng,
        decimalPlaces: Int?,
        isRounding: Boolean
    ): Boolean =
        equaltGaodeLatLng(latLng1, latLng2, decimalPlaces, isRounding)

    /**
     * 比较场景是否一致
     */
    override fun equaltCamera(
        latLng: LatLng?, //坐标,
        latLngDecimalPlaces: Int?,//坐标保留的小数位数，若为空则不做处理，保持原有位数
        latLngIsRounding: Boolean,//坐标保留小数位时是否四舍五入
        zoom: Float?, //缩放层级
        tilt: Float?, //俯仰角（overlook）
        bearing: Float? //偏航角（rotate）
    ): Boolean {
        if (latLng == null && zoom == null && tilt == null && bearing == null) return false
        latLng?.run {
            if (!equaltLatLng(
                    getCamera().target,
                    this,
                    latLngDecimalPlaces,
                    latLngIsRounding
                )
            ) return false
        }
        zoom?.run {
            if (getCamera().zoom != this) return false
        }
        tilt?.run {
            if (getCamera().zoom != this) return false
        }
        bearing?.run {
            if (getCamera().bearing != this) return false
        }
        return true
    }

    /**
     * 坐标点是否包含在多边形内
     */
    override fun isPolygonContainsPoint(polygonLatLngs: List<LatLng>, latLng: LatLng): Boolean =
        mapView.map.addPolygon(PolygonOptions().apply {
            visible(false)
            addAll(polygonLatLngs)
        }).let {
            val isContains = it.contains(latLng)
            it.remove()
            return isContains
        }
}
