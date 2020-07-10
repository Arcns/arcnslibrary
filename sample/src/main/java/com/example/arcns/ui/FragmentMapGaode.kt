package com.example.arcns.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.amap.api.maps.*
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.ScaleAnimation
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.district.DistrictResult
import com.amap.api.services.district.DistrictSearch
import com.amap.api.services.district.DistrictSearchQuery
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.arcns.core.APP
import com.arcns.core.map.MapPosition
import com.arcns.core.map.MapPositionGroup
import com.arcns.core.map.MapPositionType
import com.arcns.core.util.*
import com.arcns.map.gaode.GaodeMapViewManager
import com.example.arcns.NavMainDirections
import com.example.arcns.databinding.FragmentMapGaodeBinding
import com.example.arcns.databinding.LayoutInfoWindowBinding
import com.example.arcns.viewmodel.*
import kotlinx.android.synthetic.main.fragment_empty.toolbar
import kotlinx.android.synthetic.main.fragment_map_gaode.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.internal.wait
import java.lang.Exception

/**
 *
 */
class FragmentMapGaode : Fragment() {
    private var binding by autoCleared<FragmentMapGaodeBinding>()
    private val viewModel by viewModels<ViewModelMap>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private lateinit var mapViewManager: GaodeMapViewManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapGaodeBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@FragmentMapGaode
            viewModel = this@FragmentMapGaode.viewModel
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
        viewModel.toast.observe(viewLifecycleOwner, EventObserver {
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
//            mapViewManager.refresh()
            mapViewManager.centerFixedMarkerEnabled = !mapViewManager.centerFixedMarkerEnabled
        }
        btnDownload.setOnClickListener {
//            startActivity(Intent(context, OfflineMapActivity::class.java))
            findNavController().navigate(NavMainDirections.actionGlobalFragmentEmpty())
//            mapView.map
        }
        compassView.setLifecycleOwner(this)
        btnCompass.setOnClickListener {
            compassView.toggleVisibility()
        }
        btnAddPin.setOnClickListener {
            mapViewManager.addCenterFixedMarker(viewModel.calculateAreaMapPositionGroup)
            mapViewManager.addOrUpdatePolygons(viewModel.calculateAreaMapPositionGroup)
            viewModel.calculateAreaValue.value =
                mapViewManager.calculateArea(mapPositionGroup = viewModel.calculateAreaMapPositionGroup)
                    .toString()
        }
        btnDelPin.setOnClickListener {
            mapViewManager.removeLastMarker(viewModel.calculateAreaMapPositionGroup)
            mapViewManager.addOrUpdatePolygons(viewModel.calculateAreaMapPositionGroup)
            viewModel.calculateAreaValue.value =
                mapViewManager.calculateArea(mapPositionGroup = viewModel.calculateAreaMapPositionGroup)
                    .toString()
        }
        btnClearPin.setOnClickListener {
            mapViewManager.clear(polygonMapPositionGroups = listOf(viewModel.calculateAreaMapPositionGroup))
            viewModel.calculateAreaValue.value =
                mapViewManager.calculateArea(mapPositionGroup = viewModel.calculateAreaMapPositionGroup)
                    .toString()
        }
        btnAddLinePin.setOnClickListener {
            mapViewManager.addCenterFixedMarker(viewModel.calculateLineMapPositionGroup)
            mapViewManager.addOrUpdatePolyline(viewModel.calculateLineMapPositionGroup)
            viewModel.calculateLineValue.value =
                mapViewManager.calculateLineDistance(mapPositionGroup = viewModel.calculateLineMapPositionGroup)
                    .toString()
        }
        btnDelLinePin.setOnClickListener {
            mapViewManager.removeLastMarker(viewModel.calculateLineMapPositionGroup)
            mapViewManager.addOrUpdatePolyline(viewModel.calculateLineMapPositionGroup)
            viewModel.calculateLineValue.value =
                mapViewManager.calculateLineDistance(mapPositionGroup = viewModel.calculateLineMapPositionGroup)
                    .toString()
        }
        btnClearLinePin.setOnClickListener {
            mapViewManager.clear(polylineMapPositionGroups = listOf(viewModel.calculateLineMapPositionGroup))
            viewModel.calculateLineValue.value =
                mapViewManager.calculateLineDistance(mapPositionGroup = viewModel.calculateLineMapPositionGroup)
                    .toString()
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
        mapViewManager = GaodeMapViewManager(this, mapView, viewModel.mapViewManagerData)
        mapViewManager.centerFixedMarkerEnabled = true
//         加载完成回调
        mapViewManager.onMapLoaded = {
//            searchDistrict("汕头市")
        }
        // 定位到我的位置
        mapViewManager.locateMyLocation()
        // 绘制点点击
        mapView.map.setOnMarkerClickListener {
//            if (it.isInfoWindowShown) it.hideInfoWindow() else it.showInfoWindow()
            Toast.makeText(context, it.title, Toast.LENGTH_LONG).show()

            it.setAnimation(ScaleAnimation(2f, 2f, 2f, 2f).apply {
                setDuration(500)
                fillMode = 0
            })
            it.startAnimation()


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
                )?.run {
                    longitude = marker.position.longitude
                    latitude = marker.position.latitude
                }
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

        viewModelActivityMain.mapTrackRecorder.eventTrackDataUpdate.observe(
            viewLifecycleOwner,
            EventObserver {
                mapViewManager.refresh(
                    markerMapPositionGroups = listOf(viewModelActivityMain.mapTrackRecorder.trackData),
                    polylineMapPositionGroups = listOf(viewModelActivityMain.mapTrackRecorder.trackData)
                )
            })
        mapViewManager.refresh(
            markerMapPositionGroups = listOf(viewModelActivityMain.mapTrackRecorder.trackData),
            polylineMapPositionGroups = listOf(viewModelActivityMain.mapTrackRecorder.trackData)
        )
    }


    var districtSearch: DistrictSearch? = null
    private fun searchDistrict(district: String) {
        var query = DistrictSearchQuery().apply {
            keywords = district
            isShowBoundary = true
            isShowChild = true
            isShowBusinessArea = true
            pageSize = 10
            pageNum = 0
        }
        if (districtSearch == null) {
            districtSearch = DistrictSearch(APP.INSTANCE)
            /**
             * districtBoundary()
             * 以字符串数组形式返回行政区划边界值。
             * 字符串拆分规则： 经纬度，经度和纬度之间用","分隔，坐标点之间用";"分隔。
             * 例如：116.076498,40.115153;116.076603,40.115071;116.076333,40.115257;116.076498,40.115153。
             * 字符串数组由来： 如果行政区包括的是群岛，则坐标点是各个岛屿的边界，各个岛屿之间的经纬度使用"|"分隔。
             * 一个字符串数组可包含多个封闭区域，一个字符串表示一个封闭区域
             */
            districtSearch?.setOnDistrictSearchListener {
                if (it.aMapException.errorCode != 1000) return@setOnDistrictSearchListener
                viewModel.viewModelScope.launch {
                    setDistrictMapPositions(it)
                }
            }
        }
        districtSearch?.query = query
        districtSearch?.searchDistrictAsyn()

    }

    private suspend fun setDistrictMapPositions(it: DistrictResult) {
        viewModel.viewModelScope.async(Dispatchers.IO) {
            viewModel.districtMapPositions.clear()
            it.district?.forEach {
                it.districtBoundary()?.forEach {
                    var mapPositions = ArrayList<MapPosition>()
                    it.split(";").forEach {
                        val latLng = it.split(",")
                        if (latLng.size == 2) {
                            mapPositions.add(
                                MapPosition(
                                    latitude = latLng[1].toDoubleOrNull() ?: return@forEach,
                                    longitude = latLng[0].toDoubleOrNull()
                                        ?: return@forEach,
                                    type = MapPositionType.GCJ02
                                )
                            )
                        }
                    }
                    viewModel.districtMapPositions.add((MapPositionGroup().apply {
                        setMapPositions(mapPositions)
                    }))
                }
                it.subDistrict?.forEach { subItem ->
                    subItem.districtBoundary()?.forEach { boundary ->
                        var mapPositions = ArrayList<MapPosition>()
                        boundary.split(";").forEach { latlngToString ->
                            val latLng = latlngToString.split(",")
                            if (latLng.size == 2) {
                                mapPositions.add(
                                    MapPosition(
                                        latitude = latLng[1].toDoubleOrNull()
                                            ?: return@forEach,
                                        longitude = latLng[0].toDoubleOrNull()
                                            ?: return@forEach,
                                        type = MapPositionType.GCJ02
                                    )
                                )
                            }
                        }
                        viewModel.districtMapPositions.add((MapPositionGroup().apply {
                            setMapPositions(mapPositions)
                        }))
                    }
                }
            }
            mapViewManager.refreshPolylines(
                isClearOther = true,
                refreshTag = "districtMapPositions",
                polylineMapPositionGroups = viewModel.districtMapPositions
            )
//            mapViewManager.polygons[""]?.contains()

        }.await()
    }


    var poiSearch: PoiSearch? = null
    private fun searchPOI(position: LatLng, isContainsBound: Boolean = true) {
        var query = PoiSearch.Query("广场", "", "")//keyWord，type，cityCode
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
                    Toast.makeText(
                        context,
                        "count:" + result?.pois?.size + "  " + result!!.pois[0]!!.cityName!!,
                        Toast.LENGTH_LONG
                    ).show()
                    LOG("count:" + result?.pois?.size + "  " + result!!.pois[0]!!.cityName!!)
                    searchDistrict(result!!.pois[0]!!.cityName!!)
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


