package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.arcns.core.util.Event
import com.example.arcns.data.network.NetworkDataSource
import java.util.*
import kotlin.collections.ArrayList


class ViewModelMap : ViewModel() {


    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    // 测量
    var calculateLineMapPositionGroup = MapPositionGroup()
    var calculateAreaMapPositionGroup = MapPositionGroup()
}

class MapPositionGroup {
    private var _groupID = MutableLiveData<String>()
    var groupID: LiveData<String> = _groupID

    private var _mapPositions =
        MutableLiveData<ArrayList<MapPosition>>().apply { value = ArrayList() }
    var mapPositions: LiveData<ArrayList<MapPosition>> = _mapPositions

    val mapPositionLatLngs: List<LatLng>
        get() = mapPositions.value?.map {
            it.position
        }?.toList() ?: listOf()

    fun setGroupID(groupID: String) {
        _groupID.value = groupID
    }

    fun addMapPosition(id: String, position: LatLng): MapPosition? =
        addMapPosition(MapPosition(id, position))

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

    fun removeMapPosition(id: String): MapPosition? {
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
    var id: String,
    var position: LatLng
)
