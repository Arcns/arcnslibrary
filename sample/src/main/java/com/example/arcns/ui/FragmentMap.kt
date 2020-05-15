package com.example.arcns.ui

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.Observable
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.TranslateAnimation
import com.amap.api.maps.offlinemap.OfflineMapActivity
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.arcns.core.APP
import com.arcns.core.util.*
import com.example.arcns.NavMainDirections
import com.example.arcns.R
import com.example.arcns.databinding.FragmentEmptyBinding
import com.example.arcns.databinding.FragmentMapBinding
import com.example.arcns.databinding.LayoutInfoWindowBinding
import com.example.arcns.viewmodel.*
import kotlinx.android.synthetic.main.fragment_empty.toolbar
import kotlinx.android.synthetic.main.fragment_map.*
import java.net.URL
import kotlin.math.sqrt

/**
 *
 */
class FragmentMap : Fragment() {
    private var binding by autoCleared<FragmentMapBinding>()
    private val viewModel by viewModels<ViewModelMap>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private lateinit var mapViewManager: MapViewManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@FragmentMap
            viewModel = this@FragmentMap.viewModel
        }
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarAsToolbar(toolbar)
        setupResult()
        setupMap(savedInstanceState)
    }

    var mapTypeSelectionIndex = 0

    private fun setupResult() {
        viewModel.toast.observe(this, EventObserver {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        })
        btnUpdMapType.setOnClickListener {
            showDialog {
                title(text = "切换图层")
                listItemsSingleChoice(
                    items = listOf(
                        "高德标准图层",
                        "高德卫星图层",
                        "高德夜间图层",
                        "高德导航图层",
                        "谷歌交通图层",
                        "谷歌地形图层",
                        "谷歌卫星图层"
                    ),
                    initialSelection = mapTypeSelectionIndex
                ) { dialog, index, text ->
                    mapViewManager.clearGoogleTileOverlay()
                    mapTypeSelectionIndex = index
                    when (index) {
                        0 -> binding.mapView.map.mapType = AMap.MAP_TYPE_NORMAL
                        1 -> binding.mapView.map.mapType = AMap.MAP_TYPE_SATELLITE
                        2 -> binding.mapView.map.mapType = AMap.MAP_TYPE_NIGHT
                        3 -> binding.mapView.map.mapType = AMap.MAP_TYPE_NAVI
                        4 -> mapViewManager.setGoogleTileOverlay("m")
                        5 -> mapViewManager.setGoogleTileOverlay("p")
                        6 -> mapViewManager.setGoogleTileOverlay("y")
                    }

                }
            }
        }
        btnToggleTraffic.setOnClickListener {
            mapView.map.isTrafficEnabled = !binding.mapView.map.isTrafficEnabled
        }
        btnDownload.setOnClickListener {
//            startActivity(Intent(context, OfflineMapActivity::class.java))
//            findNavController().navigate(NavMainDirections.actionGlobalFragmentEmpty())
//            mapView.map
        }
        compassView.setLifecycleOwner(this)
        btnCompass.setOnClickListener {
            compassView.toggleVisibility()
        }
        btnAddPin.setOnClickListener {
            mapViewManager.addMarker(
                mapViewManager.centerFixedMarker?.position ?: return@setOnClickListener,
                viewModel.calculateLineMapPositionGroup
            )
            mapViewManager.addOrUpdatePolygons(viewModel.calculateLineMapPositionGroup)
        }
        btnDelPin.setOnClickListener {
            mapViewManager.removeLastMarker(viewModel.calculateLineMapPositionGroup)
            mapViewManager.addOrUpdatePolygons(viewModel.calculateLineMapPositionGroup)
        }
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState) // 此方法必须重写
        mapView.map.uiSettings.apply {
            isMyLocationButtonEnabled = true
            isZoomControlsEnabled = false
//            isTiltGesturesEnabled = false
            isCompassEnabled = true
            isScaleControlsEnabled = true
        }
        mapViewManager = MapViewManager(mapView, viewModel)
        mapViewManager.setLifecycleOwner(this)
        mapViewManager.centerFixedMarkerEnabled = true
        // 定位
        mapViewManager.locateMyLocation(followUpType = MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        // 绘制点点击
        mapView.map.setOnMarkerClickListener {
            if (it.isInfoWindowShown) it.hideInfoWindow() else it.showInfoWindow()
            Toast.makeText(context, it.title, Toast.LENGTH_LONG).show()
            true
        }
        // 绘制点拖拽
        mapView.map.setOnMarkerDragListener(object : AMap.OnMarkerDragListener {
            override fun onMarkerDragEnd(marker: Marker?) {
                Toast.makeText(context, "结束拖动" + marker?.position?.latitude, Toast.LENGTH_LONG)
                    .show()
            }

            override fun onMarkerDragStart(marker: Marker?) {
                Toast.makeText(context, "开始拖动" + marker?.position?.latitude, Toast.LENGTH_LONG)
                    .show()
            }

            override fun onMarkerDrag(marker: Marker?) {
                viewModel.calculateLineMapPositionGroup.findMapPositionByID(
                    marker?.id ?: return
                )?.position = marker.position
                mapViewManager.addOrUpdatePolygons(viewModel.calculateLineMapPositionGroup)
            }

        })
        // 信息窗体点击
        mapView.map.setOnInfoWindowClickListener {
            Toast.makeText(context, "setOnInfoWindowClickListener", Toast.LENGTH_LONG).show()
        }
        // 信息窗体适配器
        mapView.map.setInfoWindowAdapter(object : AMap.InfoWindowAdapter {
            override fun getInfoContents(marker: Marker?): View? {
                return LayoutInfoWindowBinding.inflate(
                    LayoutInflater.from(context),
                    null,
                    false
                ).root
            }

            override fun getInfoWindow(marker: Marker?): View? {
                return null
            }

        })

        mapView.map.addOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChangeFinish(position: CameraPosition?) {
//                addCameraCenterMarker(p0?.target)
//                cameraCenterMarker?.startBeatingAnimation(mapView)
                searchPOI(position!!.target)
            }

            override fun onCameraChange(position: CameraPosition?) {
            }

        })
    }

    var poiSearch: PoiSearch? = null
    private fun searchPOI(position: LatLng, isContainsBound: Boolean = true) {
        var query = PoiSearch.Query("黑龙江", "", "")//keyWord，type，cityCode
        query.pageSize = 10;
        query.pageNum = 0
        if (poiSearch == null) {
            poiSearch = PoiSearch(context, query)
        } else {
            poiSearch?.query = query
        }
        if (isContainsBound) {
            //设置周边搜索的中心点以及半径
            poiSearch?.bound = PoiSearch.SearchBound(
                LatLonPoint(
                    position.latitude,
                    position.longitude
                ), Int.MAX_VALUE
            )
        } else {
            poiSearch?.bound = null
        }
        poiSearch?.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
            override fun onPoiItemSearched(item: PoiItem?, rCode: Int) {
            }

            override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                if (result?.pois?.size ?: 0 == 0 && isContainsBound) {
                    searchPOI(position, false)
                } else {
                    Toast.makeText(context, "count:" + result?.pois?.size, Toast.LENGTH_LONG).show()
                }
            }
        });
        poiSearch?.searchPOIAsyn();
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}


open class MapViewManagerViewModel : ViewModel() {
    private var _isfirstLoad = MutableLiveData<Boolean>()
    var isfirstLoad: Boolean = _isfirstLoad.value ?: true
    fun onFirstLoadComplete() {
        _isfirstLoad.value = true
    }
}

/**
 * 地图视图管理器
 */
class MapViewManager(val mapView: MapView, val viewModel: MapViewManagerViewModel) {
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
    var centerFixedMarkerEnabled: Boolean = false
        set(value) {
            field = value
            updateCenterFixedMarker()
        }

    // 设置生命周期感知，设置生命周期后可以不需要再在onDestroy、onResume、onPause中进行回调
    private var lifecycleListener: MapViewManagerLifecycleListener? = null
    private var _lifecycleOwner: LifecycleOwner? = null
    fun setLifecycleOwner(fragment: Fragment) = setLifecycleOwner(fragment.viewLifecycleOwner)
    fun setLifecycleOwner(value: LifecycleOwner) {
        if (value != null) {
            if (lifecycleListener == null) {
                lifecycleListener = MapViewManagerLifecycleListener(this)
            }
            value?.lifecycle?.addObserver(lifecycleListener!!)
        } else {
            if (lifecycleListener != null) {
                _lifecycleOwner?.lifecycle?.removeObserver(lifecycleListener!!)
            }
        }
        _lifecycleOwner = value
    }

    // 当前谷歌图层
    private var currentGoogleTileOverlay: TileOverlay? = null

    init {
        var onMapLoadedListener: AMap.OnMapLoadedListener? = null
        onMapLoadedListener = AMap.OnMapLoadedListener {
            isMapLoaded = true
            updateCenterFixedMarker()
            mapView.map.removeOnMapLoadedListener(onMapLoadedListener)
        }
        mapView.map.addOnMapLoadedListener(onMapLoadedListener)
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
            return
        }
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
    fun updateCenterFixedMarker() {
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
    fun clear() {
        mapView.isEnabled
        mapView.map.clear()
        markers.clear()
        polygons.clear()
        polygons.clear()
    }

    /**
     * 按传入的新数据进行刷新
     */
    fun refresh(
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
    fun removePolyline(mapPositionGroup: MapPositionGroup) {
        polylines[mapPositionGroup.groupID.value]?.run {
            remove()
            polylines.remove(mapPositionGroup.groupID.value)
            mapPositionGroup.clearGroupID()
        }
    }

    /**
     * 删除多边形
     */
    fun removePolygon(mapPositionGroup: MapPositionGroup) {
        polygons[mapPositionGroup.groupID.value]?.run {
            remove()
            polygons.remove(mapPositionGroup.groupID.value)
            mapPositionGroup.clearGroupID()
        }
    }

    /**
     * 添加或刷新线
     */
    fun addOrUpdatePolyline(mapPositionGroup: MapPositionGroup) {
        if (mapPositionGroup.groupID.value.isNullOrBlank()) {
            val polyline = mapView.map.addPolyline(
                PolylineOptions().addAll(mapPositionGroup.mapPositionLatLngs).apply {
                    // 应用自定义样式
                    mapPositionGroup.applyCustomOptions?.invoke(this)
//                        .color(R.color.colorAccent.color).width(4f).zIndex(900f)
                }
            )
            mapPositionGroup.setGroupID(polyline.id)
            polylines[polyline.id] = polyline
        } else {
            polylines[mapPositionGroup.groupID.value ?: return]?.points =
                mapPositionGroup.mapPositionLatLngs
        }
    }

    /**
     * 添加或刷新多边形
     */
    fun addOrUpdatePolygons(mapPositionGroup: MapPositionGroup) {
        // 低于3个时画线
        if (mapPositionGroup.mapPositionLatLngs.size < 3) {
            removePolygon(mapPositionGroup)
            addOrUpdatePolyline(mapPositionGroup)
            return
        }
        // 大于等于3个时画多边形
        removePolyline(mapPositionGroup)
        if (mapPositionGroup.groupID.value.isNullOrBlank()) {
            val polygon = mapView.map.addPolygon(
                PolygonOptions().addAll(mapPositionGroup.mapPositionLatLngs).apply {
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
            polygons[mapPositionGroup.groupID.value ?: return]?.points =
                mapPositionGroup.mapPositionLatLngs
        }
    }

    /**
     * 添加或更新多个点
     */
    fun addOrUpdateMarkers(mapPositionGroup: MapPositionGroup) {
        mapPositionGroup.setMapPositions(mapPositionGroup.mapPositions.value?.apply {
            forEach {
                addOrUpdateMarker(it, mapPositionGroup)
            }
        })
    }

    /**
     * 添加或更新点（注意，如果数据集合未包含该点的id，则会在添加后把id赋值给对象）
     */
    fun addOrUpdateMarker(
        mapPosition: MapPosition,
        mapPositionGroup: MapPositionGroup? = null
    ) {
        if (markers.containsKey(mapPosition.id)) {
            markers[mapPosition.id]?.position = mapPosition.position
        } else {
            mapPosition.id =
                addMarker(
                    latLng = mapPosition.position,
                    applyCustomOptions = mapPositionGroup?.applyCustomOptions
                ).id
        }
    }

    /**
     * 添加点（若mapPositionGroup不为空则同时更新数据到MapPositionGroup）
     */
    fun addMarker(
        latLng: LatLng,
        mapPositionGroup: MapPositionGroup? = null,
        applyCustomOptions: ApplyCustomOptions? = mapPositionGroup?.applyCustomOptions
    ): Marker {
        val marker = mapView.map.addMarker(
            MarkerOptions().position(latLng).apply {
                applyCustomOptions?.invoke(this)
            }
        )
        mapPositionGroup?.addMapPosition(marker.id, marker.position)
        markers[marker.id] = marker
        return marker
    }

    /**
     * 删除最后一个点（同时更新数据到MapPositionGroup）
     */
    fun removeLastMarker(mapPositionGroup: MapPositionGroup) {
        mapPositionGroup.removeMapPosition()?.run {
            removeMarker(id)
        }
    }

    /**
     * 删除点（同时更新数据到MapPositionGroup）
     */
    fun removeMarker(id: String?, mapPositionGroup: MapPositionGroup? = null) {
        mapPositionGroup?.removeMapPosition(id)
        markers[id]?.remove()
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
    fun setGoogleTileOverlay(lyrs: String): TileOverlay {
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
        return currentGoogleTileOverlay!!
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

}

/**
 * 生命周期事件
 */
internal class MapViewManagerLifecycleListener(val mapViewManager: MapViewManager) :
    LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        mapViewManager.mapView.onDestroy()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        mapViewManager.mapView.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        mapViewManager.mapView.onPause()
    }
}

/**
 * 跳动动画
 */
fun Marker.startBeatingAnimation(mapView: MapView) {
    //根据屏幕距离计算需要移动的目标点
    val point = mapView.map.projection.toScreenLocation(position)
    point.y -= 125.dp
    val target = mapView.map.projection.fromScreenLocation(point)
    //使用TranslateAnimation,填写一个需要移动的目标点
    val animation = TranslateAnimation(target)
    animation.setInterpolator { input -> // 模拟重加速度的interpolator
        if (input <= 0.5) {
            (0.5f - 2 * (0.5 - input) * (0.5 - input)).toFloat()
        } else {
            (0.5f - sqrt((input - 0.5f) * (1.5f - input).toDouble())).toFloat()
        }
    }
    //整个移动所需要的时间
    animation.setDuration(600)
    //设置动画
    setAnimation(animation)
    //开始动画
    startAnimation()
}