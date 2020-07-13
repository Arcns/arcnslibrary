package com.arcns.core.network

import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.NotificationProgressOptions
import com.arcns.core.app.cancelIfDisable
import com.arcns.core.app.show
import com.arcns.core.util.LOG
import okhttp3.*
import okio.Buffer
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


// 上传任务的文件成功回调
typealias OnUploadFileSuccess = (UploadTask, UploadTaskFileParameter) -> Unit
// 上传任务的文件失败回调
typealias OnUploadFileFailure = (UploadTask, UploadTaskFileParameter, Exception?) -> Unit
// 上传任务的文件进度更新回调
typealias OnUploadFileProgressUpdate = (UploadTask, UploadTaskFileParameter, NetworkTaskProgress) -> Unit

/**
 * 上传管理器
 */
class UploadManager {

    // OkHttpClient
    var httpClient: OkHttpClient
        private set

    //上传任务列表
    var currentTasks = ArrayList<UploadTask>()

    // 每次上传的字节数
    var perByteCount = 2048

    // 上传任务成功回调
    var onTaskSuccess: OnTaskSuccess<UploadTask>? = null

    //上传任务失败回调
    var onTaskFailure: OnTaskFailure<UploadTask>? = null

    // 上传任务的文件成功回调
    var onUploadFileSuccess: OnUploadFileSuccess? = null

    // 上传任务的文件失败回调
    var onUploadFileFailure: OnUploadFileFailure? = null

    // 上传任务的文件进度回调
    var onProgressUpdate: OnUploadFileProgressUpdate? = null

    // 上传时间间隔
    var progressUpdateInterval: Long = 1000

    // 自定义MultipartBody回调
    var onCustomMultipartBody: ((UploadTask, MultipartBody.Builder) -> Unit)? = null

    // 自定义Request回调
    var onCustomRequest: ((UploadTask, Request.Builder) -> Unit)? = null


    constructor(httpClient: OkHttpClient? = null) {
        this.httpClient =
            httpClient ?: OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).build()
    }

    /**
     * 上传文件
     */
    @Synchronized
    fun upLoad(task: UploadTask): Boolean {
        // 确保任务不重复
        currentTasks.removeAll {
            it.isStop// 从列表中删除已结束的任务
        }
        val taskUploadFilePaths =
            task.parameters.filter { it is UploadTaskFileParameter && !it.uploadFilePath.isNullOrBlank() }
                .map { (it as UploadTaskFileParameter).uploadFilePath }
        currentTasks.forEach {
            if (it == task || it.id == task.id) {
                return false // 任务已存在
            }
            it.parameters.forEach {
                if (it is UploadTaskFileParameter && taskUploadFilePaths.contains(it.uploadFilePath)) {
                    return false // 上传的文件已被占用
                }
            }
        }
        currentTasks.add(task)
        // 开始上传
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)//设置类型
        // 添加参数
        task.parameters.forEach {
            when (it) {
                is UploadTaskParameter -> bodyBuilder.addFormDataPart(it.name, it.value) // 普通参数
                is UploadTaskFileParameter -> bodyBuilder.addFormDataPart( //文件参数
                    it.name,
                    it.fileName,
                    createUploadFileRequestBody( // 创建文件RequestBody
                        task,
                        it,
                        perByteCount,
                        task.progressUpdateInterval ?: progressUpdateInterval
                    )
                )
            }
        }
        // 自定义MultipartBody回调
        onCustomMultipartBody?.invoke(task, bodyBuilder)
        //
        LOG("UploadManager ${task.id} start")
        (task.okHttpClient ?: httpClient).newCall(
            Request.Builder().apply {
                // 设置下载路径
                url(task.url)
                // 设置MultipartBody
                post(bodyBuilder.build())
                // 自定义Request回调
                (task.onCustomRequest ?: onCustomRequest)?.invoke(task, this)
            }.build()
        ).apply {
            // 更新任务状态为运行中
            task.onChangeStateToRunning(this)
            // 封装请求回调处理
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LOG("UploadManager ${task.id} task error " + e.message)
                    // 更新状态
                    task.onChangeStateToFailureIfNotStop()
                    // 失败回调
                    task.onTaskFailure?.invoke(task, e, null)
                    onTaskFailure?.invoke(task, e, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        LOG("UploadManager ${task.id} task ok")
                        // 更新状态
                        task.onChangeStateToSuccess()
                        // 成功回调
                        task.onTaskSuccess?.invoke(task, response)
                        onTaskSuccess?.invoke(task, response)
                    } else {
                        LOG("UploadManager ${task.id} task not ok ")
                        // 更新状态
                        task.onChangeStateToFailureIfNotStop()
                        // 失败回调
                        task.onTaskSuccess?.invoke(task, response)
                        onTaskFailure?.invoke(task, null, response)
                    }
                }
            })
        }
        return true
    }

    /**
     * 创建上传文件的请求体RequestBody
     */
    private fun createUploadFileRequestBody(
        task: UploadTask,
        parameter: UploadTaskFileParameter,
        uploadPerByteCount: Int,
        updateInterval: Long
    ) =
        object : RequestBody() {

            init {
                LOG("UploadManager ${task.id} parameter init ")
            }

            private var lastProgressUpdateTime: Long = 0

            override fun contentType(): MediaType? = parameter.fileMediaType

            override fun contentLength(): Long = parameter.contentLength

            override fun writeTo(sink: BufferedSink) {
                LOG("UploadManager ${task.id} writeTo")
                var current: Long = 0
                try {
                    if (parameter.isSupportBreakpointResume) {
                        // 断点续传
                        current = parameter.breakpoint
                        // 获取支持断点续传的文件源
                        val source = parameter.getBreakpointResumeSource()
                            ?: throw Exception("file parameter source not empty")
                        // 开始循环上传
                        val bytes = ByteArray(uploadPerByteCount)
                        var len: Int
                        updateProgress(current)
                        while (source.read(bytes).also { len = it } != -1) {
                            if (task.isStop) {
                                throw Exception("task is stop")
                            }
                            sink.write(bytes, 0, len)
                            current += len
                            // 更新进度回调
                            updateProgress(current)
                        }
                    } else {
                        // 标准上传
                        var source = parameter.getStandardSource()
                            ?: throw Exception("file parameter source not empty")
                        // 开始循环上传
                        val buf = Buffer()
                        var len: Long
                        updateProgress(current)
                        while (source.read(buf, uploadPerByteCount.toLong())
                                .also { len = it } != -1L
                        ) {
                            sink.write(buf, len)
                            current += len
                            // 更新进度回调
                            updateProgress(current)
                        }
                    }
                    // 上传完成后，再更新一次进度回调
                    updateProgress(current, true)
                    // 上传任务的文件成功回调
                    uploadFileSuccess()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 上传任务的文件失败回调
                    uploadFileFailure(e)
                } finally {
                    // 释放连接
                    parameter.closeSource()
                }
            }

            //上传任务的文件成功回调
            private fun uploadFileSuccess() {
                // 上传任务的文件成功回调
                task.onUploadFileSuccess?.invoke(task, parameter)
                onUploadFileSuccess?.invoke(task, parameter)
                // 更新到通知栏
                LOG("UploadManager ${task.id} parameter ok ")
                parameter.updateNotification(TaskState.Success, task.notificationOptions)
            }

            //上传任务的文件失败回调
            private fun uploadFileFailure(e: Exception) {
                //上传任务的文件失败回调
                task.onUploadFileFailure?.invoke(task, parameter, e)
                onUploadFileFailure?.invoke(task, parameter, e)
                // 更新到通知栏
                LOG("UploadManager ${task.id} parameter error " + e.message)
                parameter.updateNotification(
                    if (task.isStop) task.state else TaskState.Failure,
                    task.notificationOptions
                )
            }


            /**
             * 上传任务的文件进度回调
             */
            private fun updateProgress(current: Long, isEnd: Boolean = false) {
                // 判断回调间隔，避免短时间内多次回调
                if (!isEnd && System.currentTimeMillis() - lastProgressUpdateTime < updateInterval) return
                lastProgressUpdateTime = System.currentTimeMillis()
                // 避免相同进度重复回调
                if (parameter.currentProgress?.current == current) return
                // 更新到任务中，然后进行回调
                parameter.updateProgress(current).run {
                    task.onUploadFileProgressUpdate?.invoke(task, parameter, this)
                    onProgressUpdate?.invoke(task, parameter, this)
                }
                // 更新到通知栏
                parameter.updateNotification(backupNotificationOptions = task.notificationOptions)
            }
        }

    /**
     * 根据url查找任务
     */
    fun findTaskByUrl(url: String): UploadTask? = currentTasks.firstOrNull { it.url == url }

    /**
     * 根据id查找任务
     */
    fun findTaskByID(id: String): UploadTask? = currentTasks.firstOrNull { it.id == id }

}


