package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.arcns.core.util.Event
import com.example.arcns.data.network.NetworkDataSource
import com.example.arcns.ui.MapViewManagerViewModel
import kotlin.collections.ArrayList


class ViewModelMap : MapViewManagerViewModel() {


    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    // 测量
    var calculateLineMapPositionGroup = MapPositionGroup().apply {
        applyCustomOptions = {
            when (it) {
                is MarkerOptions -> it.draggable(true)
            }
        }
    }
    var calculateAreaMapPositionGroup = MapPositionGroup()
}

typealias ApplyCustomOptions = ((options: Any) -> Unit)

class MapPositionGroup {
    private var _groupID = MutableLiveData<String>()
    var groupID: LiveData<String> = _groupID

    private var _mapPositions =
        MutableLiveData<ArrayList<MapPosition>>().apply { value = ArrayList() }
    var mapPositions: LiveData<ArrayList<MapPosition>> = _mapPositions

    /**
     * 添加时样式配置格式化，如果如果使用自定义样式时，可以使用该变量，其中options为MarkerOptions或PolylineOptions或PolygonOptions等
     */
    var applyCustomOptions: ApplyCustomOptions? = null

    /**
     * 返回所有地图点坐标
     */
    val mapPositionLatLngs: List<LatLng>
        get() = mapPositions.value?.map {
            it.position
        }?.toList() ?: listOf()

    /**
     * 根据id查找地图点
     */
    fun findMapPositionByID(id: String): MapPosition? {
        mapPositions.value?.forEach {
            if (it.id == id) {
                return it
            }
        }
        return null
    }

    /**
     * 根据id判断组合中是否包含地图点
     */
    fun containMapPositionID(id: String): Boolean = findMapPositionByID(id) != null

    /**
     * 设置地图组
     */
    fun setGroupID(groupID: String) {
        _groupID.value = groupID
    }

    /**
     * 清空地图组ID
     */
    fun clearGroupID() {
        _groupID.value = null
    }

    /**
     * 设置地图点列表
     */
    fun setMapPositions(mapPositions: ArrayList<MapPosition>?) {
        _mapPositions.value = mapPositions ?: arrayListOf()
    }

    /**
     * 清空地图点列表
     */
    fun clearMapPositions() = setMapPositions(null)

    /**
     * 添加地图点
     */
    fun addMapPosition(latitude: Double, longitude: Double): MapPosition? =
        addMapPosition(MapPosition(latitude, longitude))

    /**
     * 添加地图点
     */
    fun addMapPosition(id: String? = null, position: LatLng): MapPosition? =
        addMapPosition(MapPosition(id, position))

    /**
     * 添加地图点
     */
    fun addMapPosition(position: MapPosition): MapPosition? {
        _mapPositions.value =
            _mapPositions.value?.apply {
                if (!contains(position)) {
                    add(position)
                    return position
                }
            }
        return null
    }

    /**
     * 删除指定的地图点，注意如果不传入值，则默认会删除最后一个添加的地图点
     */
    fun removeMapPosition(position: MapPosition? = _mapPositions.value?.lastOrNull()): MapPosition? {
        _mapPositions.value =
            _mapPositions.value?.apply {
                if (contains(position)) {
                    remove(position)
                    return position
                }
            }

        return null
    }

    /**
     * 按id进行对地图点进行删除
     */
    fun removeMapPosition(id: String?): MapPosition? {
        _mapPositions.value =
            _mapPositions.value?.apply {
                var removeItem: MapPosition? = null
                run removeByID@{
                    forEach {
                        if (it.id == id) {
                            removeItem = it
                            remove(it)
                            return@removeByID
                        }
                    }
                }
                return removeItem
            }

        return null
    }
}

data class MapPosition(
    var id: String? = null,
    var position: LatLng
) {
    constructor(latitude: Double, longitude: Double) : this(position = LatLng(latitude, longitude))
}
