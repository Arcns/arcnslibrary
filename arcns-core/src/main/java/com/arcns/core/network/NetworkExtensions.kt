package com.arcns.core.network

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
    get() = code() == HttpURLConnection.HTTP_PARTIAL || "bytes".equals(
        header("Accept-Ranges"),
        true
    )