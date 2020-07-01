package com.arcns.core.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.util.fastValue
import com.arcns.core.util.isMainThread
import java.util.*
import kotlin.collections.ArrayList


typealias ApplyCustomOptions = ((group: MapPositionGroup?, options: Any, position: MapPosition?) -> Unit)

/**
 * 通用地图坐标组（请在ViewModel中创建）
 */
class MapPositionGroup {
    val uniqueID: String = UUID.randomUUID().toString()

    var groupID: String? = null
        private set

    private var _mapPositions =
        MutableLiveData<ArrayList<MapPosition>>().apply { fastValue = ArrayList() }
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
    fun setGroupID(value: String) {
        groupID = value
    }

    /**
     * 清空地图组ID
     */
    fun clearGroupID() {
        groupID = null
    }

    /**
     * 设置地图点列表
     */
    fun setMapPositions(mapPositions: ArrayList<MapPosition>?) {
        _mapPositions.fastValue = mapPositions ?: arrayListOf()
    }

    /**
     * 刷新地图点列表
     * 与setMapPositions的区别在于，该方法会对新老列表进行对比，并在遇到相同坐标点的时候会回调onRefreshSameItemCallback，全部判断完成后会调用setMapPositions进行设置
     */
    fun refreshMapPositions(
        newMapPositions: ArrayList<MapPosition>?,
        decimalPlaces: Int? = null,//刷新时忽略坐标小数点的位数，默认为空即不忽略
        isRounding: Boolean = true,//刷新时忽略坐标小数点时是否四舍五入
        isEqualtExtraData: Boolean = false, //刷新时是否同时对比坐标的ExtraData
        isUniquenessItem: Boolean = true, // 刷新时是否每个item在列表中都是唯一的，若是则不进行重复对比
        isFillMapPositionIDWhenSame: Boolean = true, // 对比相同时，自动把老坐标中的id填充给新坐标
        onRefreshSameItemCallback: (MapPosition, MapPosition) -> Unit // 对比相同时的回调
    ) {
        if (!mapPositions.isNullOrEmpty() && !newMapPositions.isNullOrEmpty()) {
            equaltMapPositions(
                newMapPositions,
                mapPositions,
                decimalPlaces,
                isRounding,
                isEqualtExtraData,
                isUniquenessItem
            ) { newMapPosition, oldMapPosition ->
                if (isFillMapPositionIDWhenSame) {
                    newMapPosition.id = oldMapPosition.id
                }
                onRefreshSameItemCallback(newMapPosition, oldMapPosition)
            }
        }
        setMapPositions(newMapPositions)
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
        _mapPositions.fastValue =
            _mapPositions.value?.apply {
                if (!contains(position)) {
                    add(position)
                    return position
                }
            }
        return null
    }

    fun clearMapPosition() {
        _mapPositions.fastValue = arrayListOf()
    }


    /**
     * 删除指定的地图点，注意如果不传入值，则默认会删除最后一个添加的地图点
     */
    fun removeMapPosition(position: MapPosition? = _mapPositions.value?.lastOrNull()): MapPosition? {
        _mapPositions.fastValue =
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
        _mapPositions.fastValue =
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