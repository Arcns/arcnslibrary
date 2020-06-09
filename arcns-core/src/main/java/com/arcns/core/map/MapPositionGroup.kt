package com.arcns.core.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*
import kotlin.collections.ArrayList


typealias ApplyCustomOptions = ((group: MapPositionGroup?, options: Any) -> Unit)

/**
 * 通用地图坐标组（请在ViewModel中创建）
 */
class MapPositionGroup {
    val uniqueID: String = UUID.randomUUID().toString()

    private var _groupID = MutableLiveData<String>()
    val groupID: String? get() = _groupID.value

    private var _mapPositions =
        MutableLiveData<ArrayList<MapPosition>>().apply { value = ArrayList() }
    val mapPositionsLiveData: LiveData<ArrayList<MapPosition>> = _mapPositions
    val mapPositions: ArrayList<MapPosition> get() = _mapPositions.value ?: arrayListOf()

    /**
     * 添加时样式配置格式化，如果如果使用自定义样式时，可以使用该变量，其中options为MarkerOptions或PolylineOptions或PolygonOptions等
     */
    var applyCustomOptions: ApplyCustomOptions? = null

    /**
     * 根据id查找地图点
     */
    fun findMapPositionByID(id: String): MapPosition? {
        mapPositions.forEach {
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
    fun addMapPosition(
        id: String? = null,
        latitude: Double,
        longitude: Double,
        type: MapPositionType
    ): MapPosition? =
        addMapPosition(MapPosition(latitude, longitude, type))

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

    fun clearMapPosition() {
        _mapPositions.value = arrayListOf()
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