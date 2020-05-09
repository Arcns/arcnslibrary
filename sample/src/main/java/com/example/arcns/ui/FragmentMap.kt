package com.example.arcns.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.arcns.core.APP
import com.arcns.core.util.*
import com.example.arcns.R
import com.example.arcns.databinding.FragmentMapBinding
import com.example.arcns.databinding.LayoutInfoWindowBinding
import com.example.arcns.viewmodel.ViewModelActivityMain
import com.example.arcns.viewmodel.ViewModelMap
import kotlinx.android.synthetic.main.fragment_empty.toolbar
import kotlinx.android.synthetic.main.fragment_map.*
import java.net.URL


/**
 *
 */
class FragmentMap : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private val viewModel by viewModels<ViewModelMap>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()

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
                    lastGoogleTileOverlay?.remove()
                    lastGoogleTileOverlay = null
                    mapTypeSelectionIndex = index
                    when (index) {
                        0 -> binding.mapView.map.mapType = AMap.MAP_TYPE_NORMAL
                        1 -> binding.mapView.map.mapType = AMap.MAP_TYPE_SATELLITE
                        2 -> binding.mapView.map.mapType = AMap.MAP_TYPE_NIGHT
                        3 -> binding.mapView.map.mapType = AMap.MAP_TYPE_NAVI
                        4 -> addGoogleTileOverlay("m")
                        5 -> addGoogleTileOverlay("p")
                        6 -> addGoogleTileOverlay("y")
                    }

                }
            }
        }
        btnToggleTraffic.setOnClickListener {
            mapView.map.isTrafficEnabled = !binding.mapView.map.isTrafficEnabled
        }
        btnDownload.setOnClickListener {
//            startActivity(Intent(context, OfflineMapActivity::class.java))
            addPolyline()
        }
    }

    private fun addMarker() {
        val latLng = LatLng(39.906901, 116.397972)
        val marker = mapView.map.addMarker(
            MarkerOptions().position(latLng).title("北京").snippet("DefaultMarker").icon(
                BitmapDescriptorFactory.fromBitmap(
                    R.drawable.ic_my_location.bitmap(
                        context,
                        100,
                        100
                    )
                )
            )
                .draggable(true).setFlat(true)
        )
        mapView.map.animateCamera(
            CameraUpdateFactory.newLatLng(latLng)
        )
    }

    private fun addMarker2() {
        val latLng = LatLng(39.906901, 116.397972)
        val view = LayoutInfoWindowBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).root
        val marker = mapView.map.addMarker(
            MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromView(view))
                .draggable(true)//.setFlat(true)
        )
        mapView.map.animateCamera(
            CameraUpdateFactory.newLatLng(latLng)
        )
    }

    private fun addPolyline() {
        val latLngs = ArrayList<LatLng>()
        latLngs.add(LatLng(39.999391, 116.135972))
        latLngs.add(LatLng(39.898323, 116.057694))
        latLngs.add(LatLng(39.900430, 116.265061))
        latLngs.add(LatLng(39.955192, 116.140092))
        var polyline = mapView.map.addPolyline(
            PolylineOptions().addAll(latLngs).width(10f).color(R.color.red.color).zIndex(1000f)
        )

        mapView.map.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().apply {
            latLngs.forEach {
                include(it)
            }
        }.build(), 100));
    }

    /*谷歌瓦片图层地址 lyrs参数:
    m：谷歌交通图
    t：谷歌地形图
    p：带标签的谷歌地形图
    s：谷歌卫星图
    y：带标签的谷歌卫星图
    h：谷歌标签层（路名、地名等）
    */
    private var lastGoogleTileOverlay: TileOverlay? = null
    private fun addGoogleTileOverlay(lyrs: String) {
        val url = "https://mt3.google.cn/maps/vt?lyrs=%s@167000000&hl=zh-CN&gl=cn&x=%d&y=%d&z=%d"
        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
                return URL(String.format(url, lyrs, x, y, zoom))
            }
        }
        lastGoogleTileOverlay =
            mapView.map.addTileOverlay(
                TileOverlayOptions().tileProvider(tileProvider).diskCacheEnabled(true)
                    .diskCacheDir(APP.INSTANCE.cacheDir?.absoluteFile.toString())
                    .diskCacheSize(100000)
                    .memoryCacheEnabled(true)
                    .memCacheSize(100000)
            )
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
        mapView.map.myLocationStyle = MyLocationStyle()
        mapView.map.isMyLocationEnabled = true
        mapView.map.addOnMyLocationChangeListener {
            if (mapView.map.myLocationStyle.myLocationType != MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                mapView.map.myLocationStyle = MyLocationStyle().apply {
                    myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                    interval(2000)
                    myLocationIcon(
                        BitmapDescriptorFactory.fromBitmap(
                            R.drawable.ic_my_location.bitmap(
                                context,
                                100,
                                100
                            )
                        )
                    )
                }
        }
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
    }


    override fun onDestroyView() {
        mapView.onDestroy()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}

