package com.arcns.core.network

import com.arcns.core.file.FileUtil
import okhttp3.Response
import java.net.HttpURLConnection


// 下载上传任务失败回调
typealias OnTaskFailure<T> = (T, Exception?, Response?) -> Unit

// 下载上传任务成功回到
typealias OnTaskSuccess<T> = (T, Response) -> Unit

// 下载上传任务通知占位符
const val TASK_NOTIFICATION_PLACEHOLDER_FILE_NAME = "{fileName}"
const val TASK_NOTIFICATION_PLACEHOLDER_SHOW_NAME = "{showName}"
const val TASK_NOTIFICATION_PLACEHOLDER_LENGTH = "{length}"
const val TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE = "{percentage}"

/**
 * 是否支持断点续传（Range寻址）
 */
val Response.isAcceptRange: Boolean
    get() = code == HttpURLConnection.HTTP_PARTIAL || "bytes".equals(
        header("Accept-Ranges"),
        true
    )

/**
 * 获取文件名
 */
val Response.fileName: String?
    get() {
        val value = header("Content-Disposition") ?: return null
        var start = value.lastIndexOf("filename=").let { if (it >= 0) it else null } ?: return null
        var end = value.indexOf(";", start).let { if (it >= 0) it else value.length }
        val fileName =
            value.substring(start, end).split("=").let { if (it.size == 2) it[1] else null }
        return fileName
    }

fun String.getAbbreviatedText(length: Int): String {
    if (this.length <= length) return this
    val middle = length / 2
    val start = this.substring(0, middle)
    val end = this.substring(this.length - length + middle, this.length)
    return "$start..$end"
}