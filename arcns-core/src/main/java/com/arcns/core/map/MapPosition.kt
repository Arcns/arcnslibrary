package com.arcns.core.map

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


/**
 * 通用坐标
 */
data class MapPosition(
    var id: String? = null,
    var latitude: Double,
    var longitude: Double,
    var type: MapLatLngType,
    var extraData: Any? = null
) {

    constructor(latitude: Double, longitude: Double, type: MapLatLngType) : this(
        id = null,
        latitude = latitude,
        longitude = longitude,
        type = type
    )

    private val pi = 3.1415926535897932384626
    val toGCJ02: MapPosition
        get() {
            if (type == MapLatLngType.GCJ02) {
                return this
            }
            val x = longitude - 0.0065
            val y = latitude - 0.006
            val z =
                sqrt(x * x + y * y) - 0.00002 * sin(y * pi)
            val theta =
                Math.atan2(y, x) - 0.000003 * cos(x * pi)
            return MapPosition(
                latitude = z * sin(theta),
                longitude = z * cos(theta),
                type = MapLatLngType.GCJ02
            )
        }
    val toBD09LL: MapPosition
        get() {
            if (type == MapLatLngType.BD09LL) {
                return this
            }
            val x = longitude
            val y = latitude
            val z =
                sqrt(x * x + y * y) + 0.00002 * sin(y * pi)
            val theta =
                atan2(y, x) + 0.000003 * cos(x * pi)
            return MapPosition(
                latitude = z * sin(theta) + 0.006,
                longitude = z * cos(theta) + 0.0065,
                type = MapLatLngType.BD09LL
            )
        }
}

/**
 * 通用坐标类型
 */
enum class MapLatLngType {
    // BD09LL百度经纬度坐标
    BD09LL,

    // GCJ02国测火星坐标
    GCJ02
}

