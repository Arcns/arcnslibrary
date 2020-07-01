package com.arcns.core.util

import java.math.RoundingMode
import java.text.DecimalFormat


/**
 * 保留小数点
 * @param 要保留的小数点位数
 * @param 是否四舍五入
 */
fun Double.KeepDecimalPlaces(decimalPlaces: Int, isRounding: Boolean = true): Double =
    KeepDecimalPlacesToString(decimalPlaces, isRounding).toDouble()

/**
 * 保留小数点
 * @param 要保留的小数点位数
 * @param 是否四舍五入
 */
fun Double.KeepDecimalPlacesToString(decimalPlaces: Int, isRounding: Boolean = true): String {
    // %.2f % 表示 小数点前任意位数 2 表示两位小数 格式后的结果为 f 表示浮点型
    // return String.format("%.${decimalPlaces}f", this).toDouble()
    return DecimalFormat().let {
        it.maximumFractionDigits = decimalPlaces
        it.minimumFractionDigits = decimalPlaces
        it.roundingMode = if (isRounding) RoundingMode.HALF_UP else RoundingMode.FLOOR
        it.groupingSize = 0
        it.isGroupingUsed = false
        it.format(this)
    }
}

/**
 * 不足位数时在前面补零
 */
fun String.zeroPadding(digits: Int): String = (digits - this.length).let {
    if (it <= 0) this else String.format(
        "%0${it}d",
        0
    ) + this
}


/**
 * 16进制转化为字母
 * @param hex  要转化的16进制数，用逗号隔开
 * 如：53,68,61,64,6f,77
 * @return
 */
fun hex2Str(hex: String): String? {
    val sb = StringBuilder()
    val split = hex.split(",".toRegex()).toTypedArray()
    for (str in split) {
        val i = str.toInt(16)
        sb.append(i.toChar())
    }
    return sb.toString()
}

/**
 * 字符串中每个字母转化为16进制
 * @param letter
 * @return
 */
fun str2Hex(letter: String): String? {
    val sb = java.lang.StringBuilder()
    for (element in letter) {
        val c = element
        sb.append(Integer.toHexString(c.toInt()))
        sb.append(", ")
    }
    sb.deleteCharAt(sb.length - 2)
    return sb.toString()
}