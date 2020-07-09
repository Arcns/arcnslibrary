package com.arcns.core.network

import com.arcns.core.app.NotificationOptions
import com.arcns.core.file.FileUtil
import com.arcns.core.file.adaptiveFileLengthUnit
import com.arcns.core.util.keepDecimalPlaces
import okhttp3.Call
import okhttp3.OkHttpClient
import java.util.*
import kotlin.math.ceil

/**
 * 上传下载任务基类
 */
abstract class NetworkTask<T>(
    var url: String,
    var notificationOptions: NotificationOptions? = null, // 建议使用UploadNotificationOptions、DownloadNotificationOptions
    var okHttpClient: OkHttpClient? = null,
    var progressUpdateInterval: Long? = null,
    var onTaskFailure: OnTaskFailure<T>? = null,
    var onTaskSuccess: OnTaskSuccess<T>? = null
) {
    val id: String = UUID.randomUUID().toString()
    var call: Call? = null
        private set
    var state: TaskState = TaskState.None
        private set
    val isRunning: Boolean get() = state == TaskState.Running
    val isStop: Boolean get() = !(isRunning || state == TaskState.None)


    fun cancel() {
        state = TaskState.Cancel
        call?.cancel()
    }

    fun pause() {
        state = TaskState.Pause
        call?.cancel()
        call = null
    }

    fun onRunning(call: Call) {
        state = TaskState.Running
        this.call = call
    }

    fun onFailure() {
        state = TaskState.Failure
        call = null
    }

    fun onFailureIfNotStop() {
        if (!isStop) onFailure()
    }

    fun onSuccess() {
        state = TaskState.Success
        call = null
    }
}

/**
 * 任务状态
 */
enum class TaskState {
    None, Running, Pause, Cancel, Failure, Success
}


/**
 * 任务进度类
 */
data class NetworkTaskProgress(
    var total: Long,
    var current: Long
) {
    fun getPercentage(decimalPlaces: Int = 2, isRounding: Boolean = true): Double =
        if (indeterminate) 0.0 else
            (current.toDouble() / total * 100).keepDecimalPlaces(decimalPlaces, isRounding)

    fun getPercentageToString(decimalPlaces: Int = 2, isRounding: Boolean = true): String =
        "${getPercentage(decimalPlaces, isRounding)}%"

    val percentage: Int get() = if (indeterminate) 0 else getPercentage(2).toInt()

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