package com.arcns.core.util



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