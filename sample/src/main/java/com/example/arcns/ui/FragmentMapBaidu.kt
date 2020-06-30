package com.example.arcns.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.arcns.core.APP
import com.arcns.core.util.EventObserver
import com.arcns.core.util.autoCleared
import com.arcns.core.util.setActionBarAsToolbar
import com.arcns.core.util.showDialog
import com.arcns.map.baidu.BaiduMapViewManager
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.InfoWindow
import com.baidu.mapapi.map.MapStatus
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.RouteNode.location
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.*
import com.baidu.mapapi.search.poi.*
import com.example.arcns.NavMainDirections
import com.example.arcns.databinding.FragmentMapBaiduBinding
import com.example.arcns.databinding.LayoutInfoWindowBinding
import com.example.arcns.viewmodel.ViewModelActivityMain
import com.example.arcns.viewmodel.ViewModelMap
import kotlinx.android.synthetic.main.fragment_map_baidu.*
import java.util.*


/**
 *
 */
class FragmentMapBaidu : Fragment() {
    private var binding by autoCleared<FragmentMapBaiduBinding>()
    private val viewModel by viewModels<ViewModelMap>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private lateinit var mapViewManager: BaiduMapViewManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        SDKInitializer.initialize(APP.INSTANCE)
        binding = FragmentMapBaiduBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@FragmentMapBaidu
            viewModel = this@FragmentMapBaidu.viewModel
        }
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarAsToolbar(toolbar)
        setupResult()
        setupMap()
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
                        "百度标准图层",
                        "百度卫星图层"
                    ),
                    initialSelection = mapTypeSelectionIndex
                ) { dialog, index, text ->
                    mapTypeSelectionIndex = index
                    when (index) {
                        0 -> binding.mapView.map.mapType = BaiduMap.MAP_TYPE_NORMAL
                        1 -> binding.mapView.map.mapType = BaiduMap.MAP_TYPE_SATELLITE
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

    private fun setupMap() {
        mapView.map.uiSettings.apply {
            isOverlookingGesturesEnabled = false
            isZoomGesturesEnabled = true
            isCompassEnabled = true
        }
        mapView.showScaleControl(true)//比例尺
        mapView.showZoomControls(false)//缩放按钮

        mapViewManager = BaiduMapViewManager(this, mapView, viewModel.mapViewManagerData)
        mapViewManager.centerFixedMarkerEnabled = true
        // 定位到我的位置
        mapViewManager.locateMyLocation()
        // 绘制点点击
        mapView.map.setOnMarkerClickListener {
            if (it.isInfoWindowEnabled) it.hideInfoWindow() else it.showInfoWindow(
                InfoWindow(
                    LayoutInfoWindowBinding.inflate(
                        LayoutInflater.from(context),
                        null,
                        false
                    ).root, it.position, 0
                )
            )
            Toast.makeText(context, it.title, Toast.LENGTH_LONG).show()
            true
        }
        // 绘制点拖拽
        mapView.map.setOnMarkerDragListener(object : BaiduMap.OnMarkerDragListener {
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
        mapView.map.setOnMapStatusChangeListener(object : BaiduMap.OnMapStatusChangeListener {
            override fun onMapStatusChangeStart(status: MapStatus?) {
            }

            override fun onMapStatusChangeStart(status: MapStatus?, p1: Int) {
            }

            override fun onMapStatusChange(status: MapStatus?) {
            }

            override fun onMapStatusChangeFinish(status: MapStatus?) {
                if (status?.target == null) return
                var geoCoder = GeoCoder.newInstance()
                geoCoder.setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
                    override fun onGetGeoCodeResult(result: GeoCodeResult?) {
                    }

                    override fun onGetReverseGeoCodeResult(result: ReverseGeoCodeResult?) {
                        if (result == null
                            || result.error != SearchResult.ERRORNO.NO_ERROR
                        ) {
                            // 没有检测到结果
                            return;
                        }
                        var kk = result
                    }

                })
                geoCoder.reverseGeoCode(ReverseGeoCodeOption().location(status.target).pageSize(10))

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

    var poiSearch: PoiSearch? = null
    private fun searchPOI(position: LatLng, isContainsBound: Boolean = true) {
        if (poiSearch == null) {
            poiSearch = PoiSearch.newInstance()
            poiSearch?.setOnGetPoiSearchResultListener(object : OnGetPoiSearchResultListener {
                override fun onGetPoiIndoorResult(p0: PoiIndoorResult?) {
                }

                override fun onGetPoiResult(p0: PoiResult?) {
                }

                override fun onGetPoiDetailResult(p0: PoiDetailResult?) {
                }

                override fun onGetPoiDetailResult(p0: PoiDetailSearchResult?) {
                }

            })
        }
        poiSearch?.searchInCity(
            PoiCitySearchOption()
                .city("北京") //必填
                .keyword("广场") //必填
                .pageNum(10)
                .cityLimit(false)
        )
    }
}


