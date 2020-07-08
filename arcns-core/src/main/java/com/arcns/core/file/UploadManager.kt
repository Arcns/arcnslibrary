package com.arcns.core.file

import android.net.Uri
import com.arcns.core.APP
import com.arcns.core.util.keepDecimalPlaces
import okhttp3.*
import okio.Buffer
import okio.BufferedSink
import okio.Okio
import okio.Source
import java.io.*
import java.util.concurrent.TimeUnit

//任务失败
typealias OnTaskFailure<T> = (T, Exception?, Response?) -> Unit

//任务成功
typealias OnTaskSuccess<T> = (T, Response) -> Unit

/**
 * 任务进度类
 */
data class TaskProgress(
    var total: Long,
    var current: Long
) {
    fun getPercentage(decimalPlaces: Int = 2, isRounding: Boolean = true): Double =
        (current.toDouble() / total * 100).keepDecimalPlaces(decimalPlaces, isRounding)

    val percentage: Int get() = (current / total * 100).toInt()
}



//上传任务各个文件事件
typealias OnUploadFileSuccess = (UploadTask, UploadFileParameter) -> Unit
typealias OnUploadFileFailure = (UploadTask, UploadFileParameter, Exception?) -> Unit
typealias OnUploadFileProgressUpdate = (UploadTask, UploadFileParameter, TaskProgress) -> Unit

/**
 * 上传任务基类
 */
data class UploadTask(
    var url: String? = null,
    var parameters: List<UploadTaskBaseParameter>,
    var okHttpClient: OkHttpClient? = null,
    var onUploadFileProgressUpdate: OnUploadFileProgressUpdate? = null,
    var onUploadFileFailure: OnUploadFileFailure? = null,
    var onUploadFileSuccess: OnUploadFileSuccess? = null,
    var onTaskFailure: OnTaskFailure<UploadTask>? = null,
    var onTaskSuccess: OnTaskSuccess<UploadTask>? = null
)


/**
 * 上传参数基类
 */
abstract class UploadTaskBaseParameter(
    var name: String
)

/**
 * 上传参数（string格式）
 */
open class UploadTaskParameter(
    name: String,
    var value: String
) : UploadTaskBaseParameter(
    name = name
)

/**
 * 上传参数（文件格式）
 */
open class UploadFileParameter : UploadTaskBaseParameter {
    private var source: Source? = null
    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var file: File? = null
    private var filePath: String? = null
    var fileName: String? = null
    var fileMimeType: String  // mimeType，例如 application/octet-stream
    var contentLength: Long = 0
    var breakpoint: Long = 0
    var currentTaskProgress: TaskProgress? = null
        private set

    // 文件源
    private var standardSource: Source? = null
    private var breakpointResumeSource: RandomAccessFile? = null

    constructor(
        name: String,
        fileName: String,
        source: Source,
        contentLength: Long,
        fileMimeType: String
    ) : super(name) {
        this.fileName = fileName
        this.source = source
        this.contentLength = contentLength
        this.fileMimeType = fileMimeType
    }

    constructor(
        name: String,
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        fileMimeType: String
    ) : super(name) {
        this.fileName = fileName
        this.inputStream = inputStream
        this.contentLength = contentLength
        this.fileMimeType = fileMimeType
    }

    constructor(
        name: String,
        fileName: String,
        uri: Uri,
        contentLength: Long? = null,
        fileMimeType: String? = null
    ) : super(name) {
        this.fileName = fileName
        this.uri = uri
        this.contentLength = contentLength ?: FileUtil.getFileLengthWithUri(APP.INSTANCE, uri)
        this.fileMimeType =
            fileMimeType ?: FileUtil.getFileSuffixWithUri(APP.INSTANCE, uri).mimeType
    }

    constructor(
        name: String,
        fileName: String,
        file: File,
        breakpoint: Long = 0,
        contentLength: Long? = null,
        fileMimeType: String? = null
    ) : super(name) {
        this.fileName = fileName
        this.file = file
        this.breakpoint = breakpoint
        this.contentLength = contentLength ?: file.length()
        this.fileMimeType =
            fileMimeType ?: FileUtil.getFileSuffix(file.absolutePath).mimeType
    }

    constructor(
        name: String,
        fileName: String,
        filePath: String,
        breakpoint: Long = 0,
        contentLength: Long? = null,
        fileMimeType: String? = null
    ) : super(name) {
        this.fileName = fileName
        this.filePath = filePath
        this.breakpoint = breakpoint
        this.contentLength = contentLength ?: File(filePath).length()
        this.fileMimeType =
            fileMimeType ?: FileUtil.getFileSuffix(filePath).mimeType
    }

    /**
     * 设置当前进度
     */
    fun updateTaskProgress(progress: Long): TaskProgress =
        TaskProgress(contentLength, progress).apply {
            currentTaskProgress = this
        }

    /**
     * 创建标准源（不支持断点续传）
     */
    fun createStandardSource(): Source? {
        standardSource?.run { return this }
        if (source != null) {
            standardSource = source
        } else if (inputStream != null) {
            standardSource = Okio.source(inputStream)
        } else if (uri != null) {
            standardSource =
                APP.INSTANCE.contentResolver.openInputStream(uri!!)?.let { Okio.source(it) }
        } else if (file != null) {
            standardSource = if (!file!!.exists()) null else Okio.source(file)
        } else if (filePath != null) {
            standardSource = File(filePath).let { if (!it.exists()) null else Okio.source(it) }
        }
        return standardSource
    }

    /**
     * 创建断电续传源
     */
    fun createBreakpointResumeSource(): RandomAccessFile? {
        breakpointResumeSource?.run { return this }
        val sourceFile = file ?: filePath?.let { File(it) }
        if (sourceFile == null || !sourceFile.exists()) return null
        return RandomAccessFile(sourceFile, "rw").apply {
            breakpointResumeSource = this
            if (breakpoint > 0) seek(breakpoint)
        }
    }

    /**
     * 当前任务是否支持断点续传
     */
    val isSupportBreakpointResume get() = breakpoint > 0 && createBreakpointResumeSource() != null


    /**
     * 上传文件类型
     */
    val fileMediaType: MediaType?
        get() = MediaType.parse(fileMimeType)

    /**
     * 关闭源
     */
    fun closeSource() {
        if (standardSource != null) {
            try {
                standardSource?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            standardSource = null
        }
        if (breakpointResumeSource != null) {
            try {
                breakpointResumeSource?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            breakpointResumeSource = null
        }
    }

}

/**
 * 上传管理器
 */
class UploadManager {

    // OkHttpClient
    var httpClient: OkHttpClient
        private set

    //上传任务列表
    var tasks = ArrayList<UploadTask>()

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
    var onUploadFileProgressUpdate: OnUploadFileProgressUpdate? = null


    constructor(httpClient: OkHttpClient? = null) {
        this.httpClient =
            httpClient ?: OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
    }

    /**
     * 上传文件
     */
    fun upLoad(task: UploadTask) {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)//设置类型
        //追加参数
        task.parameters.forEach {
            when (it) {
                is UploadTaskParameter -> builder.addFormDataPart(it.name, it.value)
                is UploadFileParameter -> builder.addFormDataPart(
                    it.name,
                    it.fileName,
                    createUploadFileRequestBody(task, it, perByteCount)
                )
            }
        }
        (task.okHttpClient ?: httpClient).newCall(
            Request.Builder().url(task.url).post(builder.build()).build()
        )
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onTaskFailure?.invoke(task, e, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        onTaskSuccess?.invoke(task, response)
                    } else {
                        onTaskFailure?.invoke(task, null, response)
                    }
                }
            })
    }

    /**
     * 创建上传文件的请求体RequestBody
     */
    fun createUploadFileRequestBody(
        task: UploadTask,
        parameter: UploadFileParameter,
        uploadPerByteCount: Int
    ) =
        object : RequestBody() {
            override fun contentType(): MediaType? = parameter.fileMediaType

            override fun contentLength(): Long = parameter.contentLength

            override fun writeTo(sink: BufferedSink) {
                var current: Long = 0
                try {
                    if (parameter.isSupportBreakpointResume) {
                        // 断点续传
                        current = parameter.breakpoint
                        val source = parameter.createBreakpointResumeSource() ?: return
                        val bytes = ByteArray(uploadPerByteCount)
                        var len: Int
                        while (source.read(bytes).also { len = it } != -1) {
                            sink.write(bytes, 0, len)
                            current += len
                            // 更新进度
                            updateTaskProgress(current)
                        }
                        updateTaskProgress(parameter.contentLength)
                    } else {
                        // 标准上传
                        var source = parameter.createStandardSource() ?: return
                        val buf = Buffer()
                        var len: Long
                        while (source.read(buf, uploadPerByteCount.toLong())
                                .also { len = it } != -1L
                        ) {
                            sink.write(buf, len)
                            current += len
                            // 更新进度
                            updateTaskProgress(current)
                        }
                        updateTaskProgress(parameter.contentLength)
                    }
                    // 上传任务的文件成功回调
                    task.onUploadFileSuccess?.invoke(task, parameter)
                    onUploadFileSuccess?.invoke(task, parameter)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 上传任务的文件失败回调
                    task.onUploadFileFailure?.invoke(task, parameter, e)
                    onUploadFileFailure?.invoke(task, parameter, e)
                } finally {
                    parameter.closeSource()
                }
            }

            /**
             * 上传任务的文件进度回调
             */
            private fun updateTaskProgress(current: Long) {
                // 避免重复回调
                if (parameter.currentTaskProgress?.current == current) return
                // 更新到任务中，然后进行回调
                parameter.updateTaskProgress(current).run {
                    task.onUploadFileProgressUpdate?.invoke(task, parameter, this)
                    onUploadFileProgressUpdate?.invoke(task, parameter, this)
                }
            }

        }
}


// 下载任务进度事件
typealias OnDownloadProgressUpdate = (UploadTask, TaskProgress) -> Unit


/**
 * 下载管理器
 */
class DownLoadManager {

    // OkHttpClient
    var httpClient: OkHttpClient
        private set

    // 下载任务列表
    var tasks = ArrayList<UploadTask>()

    // 每次下载的字节数
    var perByteCount = 2048

    constructor(httpClient: OkHttpClient? = null) {
        this.httpClient =
            httpClient ?: OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
    }


    fun downLoad(url: String, toFilePath: String) {
        httpClient.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body() ?: return
                val total = body.contentLength()
                var inputStream = body.byteStream()
                var outputStream = FileOutputStream(File(""));
                var len = 0
                var current = 0L
                val buf = ByteArray(2048)
                while (inputStream.read(buf).also { len = it } != -1) {
                    current += len
                    outputStream.write(buf, 0, len)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            }
        })
    }

}