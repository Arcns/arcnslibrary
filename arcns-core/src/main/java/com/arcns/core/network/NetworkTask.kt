package com.arcns.core.network

import com.arcns.core.app.NotificationOptions
import com.arcns.core.file.adaptiveFileLengthUnit
import com.arcns.core.util.keepDecimalPlaces
import okhttp3.Call
import okhttp3.OkHttpClient
import java.util.*

/**
 * 上传下载任务基类
 */
abstract class NetworkTask<T>(
    var url: String,//任务下载地址/上传地址
    var notificationOptions: NotificationOptions? = null, // 建议使用UploadNotificationOptions、DownloadNotificationOptions
    var okHttpClient: OkHttpClient? = null,//okhttp客户端对象
    var progressUpdateInterval: Long? = null,//进度更新间隔
    var onTaskFailure: OnTaskFailure<T>? = null,//任务失败回调
    var onTaskSuccess: OnTaskSuccess<T>? = null,//任务成功回调
    var extraData: Any? = null//自定义的附带数据
) {
    // 任务唯一性id
    val id: String = UUID.randomUUID().toString()

    // 任务Call（Okhttp）
    var call: Call? = null
        private set

    // 任务状态
    var state: TaskState = TaskState.None
        private set

    // 任务是否已经成功完成
    val isSuccess: Boolean get() = state == TaskState.Success

    // 任务是否正在等待
    val isWait: Boolean get() = state == TaskState.Wait

    // 任务是否正在运行中
    val isRunning: Boolean get() = state == TaskState.Running

    // 任务是否已经停止
    val isStop: Boolean get() = !(isRunning || state == TaskState.None || state == TaskState.Wait)

    // 停止原因
    var stopReason = NetworkTaskStopReason.Normal
        private set

    /**
     * 强制停止任务（注意此方法通常由管理器中调用，请勿随意调用）
     */
    fun forceStop(
        state: TaskState,
        reason: NetworkTaskStopReason = NetworkTaskStopReason.Human
    ) {
        this.state = state
        this.stopReason = reason
        call?.cancel()
        call = null
    }

    /**
     * 停止任务（仅当任务在运行中有效）
     */
    fun stop(
        state: TaskState,
        reason: NetworkTaskStopReason = NetworkTaskStopReason.Human
    ): Boolean {
        if (!isRunning) return false
        forceStop(state, reason)
        return true
    }

    /**
     * 取消任务（仅当任务在运行中有效）
     */
    fun cancel(reason: NetworkTaskStopReason = NetworkTaskStopReason.Human): Boolean =
        stop(TaskState.Cancel, reason)

    /**
     * 暂停任务（仅当任务在运行中有效）
     */
    fun pause(reason: NetworkTaskStopReason = NetworkTaskStopReason.Human): Boolean =
        stop(TaskState.Pause, reason)

    /**
     * 手动使任务错误失败（仅当任务在运行中有效）
     */
    fun failure(reason: NetworkTaskStopReason = NetworkTaskStopReason.Human): Boolean =
        stop(TaskState.Failure, reason)

    /**
     * 更新任务状态为运行中（注意此方法通常由管理器中调用，请勿随意调用）
     */
    fun onChangeStateToRunning(call: Call) {
        state = TaskState.Running
        this.call = call
    }

    /**
     * 更新任务状态为失败（注意此方法通常由管理器中调用，请勿随意调用）
     */
    fun onChangeStateToFailure() {
        state = TaskState.Failure
        call = null
    }

    /**
     * 若任务未停止，则更新任务状态为失败（注意此方法通常由管理器中调用，请勿随意调用）
     */
    fun onChangeStateToFailureIfNotStop() {
        if (!isStop) onChangeStateToFailure()
    }

    /**
     * 更新任务状态为成功（注意此方法通常由管理器中调用，请勿随意调用）
     */
    fun onChangeStateToSuccess() {
        state = TaskState.Success
        call = null
    }

    /**
     * 更新任务状态为等待（注意此方法通常由管理器中调用，请勿随意调用）
     */
    fun onChangeStateToWait() {
        state = TaskState.Wait
    }

    /**
     * 更新任务状态为空（注意此方法通常由管理器中调用，请勿随意调用）
     */
    fun onChangeStateToNone() {
        state = TaskState.None
    }
}

/**
 * 任务状态
 */
enum class TaskState {
    None, Wait, Running, Pause, Cancel, Failure, Success
}


/**
 * 任务进度类
 */
data class NetworkTaskProgress(
    var total: Long,
    var current: Long
) {
    // 返回下载进度百分比（double）
    fun getPercentage(decimalPlaces: Int = 2, isRounding: Boolean = true): Double =
        if (indeterminate) 0.0 else
            (current.toDouble() / total * 100).keepDecimalPlaces(decimalPlaces, isRounding)

    // 返回下载进度百分比（double string）
    fun getPercentageToString(decimalPlaces: Int = 2, isRounding: Boolean = true): String =
        "${getPercentage(decimalPlaces, isRounding)}%"

    // 返回下载进度百分比（int）
    val percentage: Int get() = if (indeterminate) 0 else getPercentage(2).toInt()

    // 返回下载进度百分比（int string）
    val permissionToString: String get() = if (indeterminate) "" else "$percentage%"

    // 返回下载长度字符串并自动转换合适的单位，格式为：已下载大小/总大小
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

/**
 * 任务停止原因
 */
enum class NetworkTaskStopReason {
    Normal, // 正常留下的成功或失败
    Human, // 人为干预某个任务导致的
    HumanAll // 人为干预全部任务导致的
}