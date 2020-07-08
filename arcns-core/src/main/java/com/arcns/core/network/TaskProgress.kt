package com.arcns.core.network

import com.arcns.core.file.adaptiveFileLengthUnit
import com.arcns.core.util.keepDecimalPlaces


/**
 * 任务进度类
 */
data class TaskProgress(
    var total: Long,
    var current: Long
) {
    fun getPercentage(decimalPlaces: Int = 2, isRounding: Boolean = true): Double =
        if (indeterminate) 0.0 else
            (current.toDouble() / total * 100).keepDecimalPlaces(decimalPlaces, isRounding)

    fun getPercentageToString(decimalPlaces: Int = 2, isRounding: Boolean = true): String =
        "${getPercentage(decimalPlaces, isRounding)}%"

    val percentage: Int get() = if (indeterminate) 0 else (current / total * 100).toInt()

    val permissionToString: String get() = "$percentage%"

    fun getLengthToString(decimalPlaces: Int = 2, isRounding: Boolean = true): String =
        current.adaptiveFileLengthUnit(
            decimalPlaces = decimalPlaces,
            isRounding = isRounding
        ) + if (indeterminate) "" else "/" + total.adaptiveFileLengthUnit(
            decimalPlaces = decimalPlaces,
            isRounding = isRounding
        )

    // 长度是否不确定性
    val indeterminate: Boolean get() = total <= 0
}