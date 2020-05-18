package com.arcns.core.util

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec


/**
 * MD5加密
 */
fun String.toMD5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.toHex()
}

/**
 * 以十六进制格式打印每个字节，以确保在字符不足的情况下用0填充输出
 */
fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}

fun String.desEncrypt(password: String): String {
    //1.创建cipher
    val c = Cipher.getInstance("DES")
    //2.初始化cipher(参数1:加密/解密模式)
    val kf = SecretKeyFactory.getInstance("DES")
    val keySpec = DESKeySpec(password.toByteArray())

    val key = kf.generateSecret(keySpec)
    c.init(Cipher.ENCRYPT_MODE, key)
    //3.加密/解密
    val encrypt = c.doFinal(toByteArray())
    //通过Base64解决乱码问题
    return encrypt.base64Encode()
}

fun String.desDecode(password: String): String {
    //1.创建cipher对象 学习查看api
    val cipher = Cipher.getInstance("DES")
    //2.初始化cirpher（参数1：加密/解密模式）
    val kf = SecretKeyFactory.getInstance("DES")
    val keySpe = DESKeySpec(password.toByteArray())
    val key = kf.generateSecret(keySpe)
    //解密模式
    cipher.init(Cipher.DECRYPT_MODE, key)
    //3.加密/解密
    val encrypt = cipher.doFinal(base64Decode())
    return String(encrypt)
}

fun ByteArray.base64Encode(): String {
    val base64EncodeChars = charArrayOf(
        'A',
        'B',
        'C',
        'D',
        'E',
        'F',
        'G',
        'H',
        'I',
        'J',
        'K',
        'L',
        'M',
        'N',
        'O',
        'P',
        'Q',
        'R',
        'S',
        'T',
        'U',
        'V',
        'W',
        'X',
        'Y',
        'Z',
        'a',
        'b',
        'c',
        'd',
        'e',
        'f',
        'g',
        'h',
        'i',
        'j',
        'k',
        'l',
        'm',
        'n',
        'o',
        'p',
        'q',
        'r',
        's',
        't',
        'u',
        'v',
        'w',
        'x',
        'y',
        'z',
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        '+',
        '/'
    )
    val sb = StringBuffer()
    val len = size
    var i = 0
    var b1: Int
    var b2: Int
    var b3: Int
    while (i < len) {
        b1 = ((this[i++]).toInt() and 0xff).toInt()
        if (i == len) {
            sb.append(base64EncodeChars[b1.ushr(2)])
            sb.append(base64EncodeChars[b1 and 0x3 shl 4])
            sb.append("==")
            break
        }
        b2 = (this[i++]).toInt() and 0xff
        if (i == len) {
            sb.append(base64EncodeChars[b1.ushr(2)])
            sb.append(base64EncodeChars[b1 and 0x03 shl 4 or (b2 and 0xf0).ushr(4)])
            sb.append(base64EncodeChars[b2 and 0x0f shl 2])
            sb.append("=")
            break
        }
        b3 = (this[i++]).toInt() and 0xff
        sb.append(base64EncodeChars[b1.ushr(2)])
        sb.append(base64EncodeChars[b1 and 0x03 shl 4 or (b2 and 0xf0).ushr(4)])
        sb.append(base64EncodeChars[b2 and 0x0f shl 2 or (b3 and 0xc0).ushr(6)])
        sb.append(base64EncodeChars[b3 and 0x3f])
    }
    return sb.toString()
}

fun String.base64Decode(): ByteArray {
    val base64DecodeChars = byteArrayOf(
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        62,
        -1,
        -1,
        -1,
        63,
        52,
        53,
        54,
        55,
        56,
        57,
        58,
        59,
        60,
        61,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        17,
        18,
        19,
        20,
        21,
        22,
        23,
        24,
        25,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        26,
        27,
        28,
        29,
        30,
        31,
        32,
        33,
        34,
        35,
        36,
        37,
        38,
        39,
        40,
        41,
        42,
        43,
        44,
        45,
        46,
        47,
        48,
        49,
        50,
        51,
        -1,
        -1,
        -1,
        -1,
        -1
    )
    val sb = StringBuffer()
    val data = toByteArray(charset("US-ASCII"))
    val len = data.size
    var i = 0
    var b1: Int
    var b2: Int
    var b3: Int
    var b4: Int
    while (i < len) {
        /* b1 */
        do {
            b1 = base64DecodeChars[(data[i++]).toInt()].toInt()
        } while (i < len && b1 == -1)
        if (b1 == -1) break
        /* b2 */
        do {
            b2 = base64DecodeChars[(data[i++]).toInt()].toInt()
        } while (i < len && b2 == -1)
        if (b2 == -1) break
        sb.append((b1 shl 2 or (b2 and 0x30).ushr(4)).toChar())
        /* b3 */
        do {
            b3 = data[i++].toInt()
            if (b3 == 61) return sb.toString().toByteArray(charset("ISO-8859-1"))
            b3 = base64DecodeChars[b3].toInt()
        } while (i < len && b3 == -1)
        if (b3 == -1) break
        sb.append((b2 and 0x0f shl 4 or (b3 and 0x3c).ushr(2)).toChar())
        /* b4 */
        do {
            b4 = data[i++].toInt()
            if (b4 == 61) return sb.toString().toByteArray(charset("ISO-8859-1"))
            b4 = base64DecodeChars[b4].toInt()
        } while (i < len && b4 == -1)
        if (b4 == -1) break
        sb.append((b3 and 0x03 shl 6 or b4).toChar())
    }
    return sb.toString().toByteArray(charset("ISO-8859-1"))
}