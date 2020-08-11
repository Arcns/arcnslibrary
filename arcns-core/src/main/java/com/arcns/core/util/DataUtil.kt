package com.arcns.core.util

import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 倒序循环输出
 */
public inline fun <T> List<T>.reverseForEach(action: (T) -> Unit): Unit {
    val count = count()
    for (index in count - 1 downTo 0) action(this[index])
}
/**
 * 倒序循环输出
 */
public inline fun <T> List<T>.reverseForEachIndexed(action: (index: Int, T) -> Unit): Unit {
    val count = count()
    for (index in count - 1 downTo 0) action(index, this[index])
}

/**
 * 根据value查找key
 */
fun <R, T> Map<R, T>.getKey(value: T): R? {
    if (!values.contains(value)) return null
    keys.forEach {
        if (get(it) == value) return it
    }
    return null
}

/**
 * 保留小数点
 * @param 要保留的小数点位数
 * @param 是否四舍五入
 */
fun Double.keepDecimalPlaces(decimalPlaces: Int, isRounding: Boolean = true): Double =
    keepDecimalPlacesToString(decimalPlaces, isRounding).toDouble()

/**
 * 保留小数点
 * @param 要保留的小数点位数
 * @param 是否四舍五入
 */
fun Double.keepDecimalPlacesToString(decimalPlaces: Int, isRounding: Boolean = true): String {
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

/**
 * 计算CRC-16/MODBU值
 * 对应C语言端代码如下：
 *
 * u16 CRC16_Check(u8 *Pushdata,u16 length)
 * {
 * u16 Reg_CRC=0xffff;
 * u8 Temp_reg=0x00;
 * u16 i,j;
 *
 * for( i = 0; i<length></length>; i ++)
 * {
 * Reg_CRC^= *Pushdata++;
 *
 * for (j = 0; j<8; j++)
 * {
 * if (Reg_CRC & 0x0001)
 * Reg_CRC=Reg_CRC>>1^0xA001;
 * else
 * Reg_CRC >>=1;
 *
 * }
 * }
 * return (Reg_CRC);
 * }
 * @param data
 * @return
 */
fun calculatedCRC16(data: ByteArray): Int {
    var Reg_CRC = 0xffff
    var temp: Int
    var j: Int
    var i = 0
    while (i < data.size) {
        temp = data[i].toInt()
        if (temp < 0) temp += 256
        temp = temp and 0xff
        Reg_CRC = Reg_CRC xor temp
        j = 0
        while (j < 8) {
            Reg_CRC =
                if (Reg_CRC and 0x0001 == 0x0001) Reg_CRC shr 1 xor 0xA001 else Reg_CRC shr 1
            j++
        }
        i++
    }
    return Reg_CRC and 0xffff
}

/**
 * 计算CRC-16/MODBU值
 * @param dataStr
 * @return
 */
fun calculatedCRC16ToHex(dataStr: String, charset: String = "GB2312"): String? {
    return try {
        Integer.toHexString(
            calculatedCRC16(dataStr.toByteArray(charset(charset)))
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}