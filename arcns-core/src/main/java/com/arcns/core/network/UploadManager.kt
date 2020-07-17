package com.arcns.core.network

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.arcns.core.util.EventObserver
import com.arcns.core.util.LOG
import okhttp3.*
import okio.Buffer
import okio.BufferedSink
import java.io.IOException


// 上传任务的文件成功回调
typealias OnUploadFileSuccess = (UploadTaskFileParameterUpdate) -> Unit
// 上传任务的文件失败回调
typealias OnUploadFileFailure = (UploadTaskFileParameterUpdate, Exception?) -> Unit
// 上传任务的文件进度更新回调
typealias OnUploadFileProgressUpdate = (UploadTaskFileParameterUpdate, NetworkTaskProgress) -> Unit

/**
 * 上传管理器
 */
class UploadManager {

    // 地图管理器绑定的数据
    var managerData: UploadManagerData
        private set

    // 无生命周期管理时与managerdata连接的对象
    var eventUploadManagerNotifyObserver: EventObserver<UploadManagerNotify>? = null
        private set

    constructor(
        managerData: UploadManagerData
    ) {
        this.managerData = managerData
        // 无生命周期管理
        eventUploadManagerNotifyObserver = EventObserver {
            handleUploadManagerNotify(it)
        }
        managerData.eventUploadManagerNotify.observeForever(eventUploadManagerNotifyObserver!!)
    }

    constructor(
        lifecycleOwner: LifecycleOwner,
        managerData: UploadManagerData
    ) {
        this.managerData = managerData
        // 拥有生命周期管理
        managerData.eventUploadManagerNotify.observe(lifecycleOwner, EventObserver {
            handleUploadManagerNotify(it)
        })
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                lifecycleOwner.lifecycle.removeObserver(this)
                releaseManagerData()
            }
        })
    }

    private fun handleUploadManagerNotify(notify: UploadManagerNotify) {
        when (notify.type) {
            UploadManagerNotifyType.Upload -> startUpLoad(notify.task)
            UploadManagerNotifyType.UpdateNotification -> managerData.updateNotification(notify.task)
        }
    }

    /**
     * 上传文件列表
     */
    @Synchronized
    fun upLoad(
        tasks: List<UploadTask>
    ): Int = managerData.upload(tasks)

    /**
     * 上传文件
     */
    @Synchronized
    fun upLoad(task: UploadTask): Boolean = managerData.upload(task)

    /**
     * 上传文件
     */
    @Synchronized
    private fun startUpLoad(task: UploadTask): Boolean {
        if (task.isRunning) return false
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
                        managerData.perByteCount,
                        task.progressUpdateInterval ?: managerData.progressUpdateInterval
                    )
                )
            }
        }
        // 自定义MultipartBody回调
        managerData.onCustomMultipartBody?.invoke(task, bodyBuilder)
        //
        LOG("UploadManager ${task.id} start")
        (task.okHttpClient ?: managerData.httpClient).newCall(
            Request.Builder().apply {
                // 设置下载路径
                url(task.url)
                // 设置MultipartBody
                post(bodyBuilder.build())
                // 自定义Request回调
                (task.onCustomRequest ?: managerData.onCustomRequest)?.invoke(task, this)
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
                    managerData.onTaskFailure?.invoke(task, e, null)
                    // 更新到通知栏
                    managerData.updateNotification(task)
                    managerData.onEventTaskUpdateByState(task)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        LOG("UploadManager ${task.id} task ok")
                        // 更新状态
                        task.onChangeStateToSuccess()
                        // 成功回调
                        task.onTaskSuccess?.invoke(task, response)
                        managerData.onTaskSuccess?.invoke(task, response)
                        // 更新到通知栏
                        managerData.updateNotification(task)
                        managerData.onEventTaskUpdateByState(task)
                    } else {
                        LOG("UploadManager ${task.id} task not ok ")
                        // 更新状态
                        task.onChangeStateToFailureIfNotStop()
                        // 失败回调
                        task.onTaskSuccess?.invoke(task, response)
                        managerData.onTaskFailure?.invoke(task, null, response)
                        // 更新到通知栏
                        managerData.updateNotification(task)
                        managerData.onEventTaskUpdateByState(task)
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
                parameter.state = TaskState.Running
                updateProgress(current)
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
                task.onUploadFileSuccess?.invoke(UploadTaskFileParameterUpdate(task, parameter))
                managerData.onUploadFileSuccess?.invoke(
                    UploadTaskFileParameterUpdate(
                        task,
                        parameter
                    )
                )
                // 更新到通知栏
                LOG("UploadManager ${task.id} parameter ok ")
                parameter.state = TaskState.Success
                parameter.updateNotification(task.notificationOptions)
                managerData.onEventTaskUpdateByProgress(
                    UploadTaskFileParameterUpdate(
                        task,
                        parameter
                    )
                )
            }

            //上传任务的文件失败回调
            private fun uploadFileFailure(e: Exception) {
                //上传任务的文件失败回调
                task.onUploadFileFailure?.invoke(UploadTaskFileParameterUpdate(task, parameter), e)
                managerData.onUploadFileFailure?.invoke(
                    UploadTaskFileParameterUpdate(
                        task,
                        parameter
                    ), e
                )
                // 更新到通知栏
                LOG("UploadManager ${task.id} parameter error " + e.message)
                parameter.state = if (task.isStop) task.state else TaskState.Failure
                parameter.updateNotification(
                    task.notificationOptions
                )
                managerData.onEventTaskUpdateByProgress(
                    UploadTaskFileParameterUpdate(
                        task,
                        parameter
                    )
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
                    task.onUploadFileProgressUpdate?.invoke(
                        UploadTaskFileParameterUpdate(
                            task,
                            parameter
                        ), this
                    )
                    managerData.onProgressUpdate?.invoke(
                        UploadTaskFileParameterUpdate(
                            task,
                            parameter
                        ), this
                    )
                }
                // 更新到通知栏
                parameter.updateNotification(backupNotificationOptions = task.notificationOptions)
                managerData.onEventTaskUpdateByProgress(
                    UploadTaskFileParameterUpdate(
                        task,
                        parameter
                    )
                )
            }
        }

    /**
     * 释放ManagerData
     */
    fun releaseManagerData() {
        eventUploadManagerNotifyObserver?.run {
            managerData.eventUploadManagerNotify.removeObserver(this)
            eventUploadManagerNotifyObserver = null
        }
        managerData.cancelAllTask()
    }

}


