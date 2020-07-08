package com.arcns.core.file

import android.net.Uri
import com.arcns.core.APP
import okhttp3.*
import okio.Buffer
import okio.BufferedSink
import okio.Okio
import okio.Source
import java.io.*
import java.util.concurrent.TimeUnit


data class UploadTask(
    var url: String? = null,
    var parameters: List<UploadTaskBaseParameter>,
    var okHttpClient: OkHttpClient? = null
)

abstract class UploadTaskBaseParameter(
    var name: String
)

open class UploadTaskParameter(
    name: String,
    var value: String
) : UploadTaskBaseParameter(
    name = name
)

open class UploadFileParameter(
    name: String,
    var fileName: String,
    var fileValue: UploadFileSource, //文件源
    var fileMimeType: String, // mimeType，例如 application/octet-stream
    var breakpoint: Long = 0
) : UploadTaskBaseParameter(
    name = name
) {
    // 上传文件类型
    val fileMediaType: MediaType?
        get() = MediaType.parse(fileMimeType) ?: MediaType.parse(MIME_TYPE_APPLICATION_OCTET_STREAM)

    // 上传文件源（不支持断点）
//    val fileSource: Source?
//        get() = when (fileValue) {
//            is Source -> fileSource
//            is InputStream -> Okio.source(fileSource as InputStream)
//            is Uri -> APP.INSTANCE.contentResolver.openInputStream(fileValue as Uri)
//                ?.let { Okio.source(it) }
//            is String -> File(fileValue as String).let {
//                if (!it.exists()) null else Okio.source(
//                    it
//                )
//            }
//            is File -> (fileValue as File).let {
//                if (!it.exists()) null else Okio.source(
//                    it
//                )
//            }
//            else -> null
//        }
//
//    // 文件是否支持断点续传
//    val supportBreakpointResumeFile: File?
//        get() = when (fileValue) {
//            is String -> File(fileValue as String).let {
//                if (!it.exists()) null else it
//            }
//            is File -> (fileValue as File).let {
//                if (!it.exists()) null else it
//            }
//            else -> null
//        }
//
//    // 上传文件大小
//    val fileLength: Long
//        get() = when (fileValue) {
//            is Uri -> FileUtil.getFileLengthWithUri(APP.INSTANCE, fileValue as Uri)
//            is String -> File(fileValue as String).length()
//            else -> 0
//        }
}

class UploadFileSource {
    private var source: Source? = null
    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var file: File? = null
    private var filePath: String? = null
    var contentLength: Long = 0
    var breakpoint: Long = 0

    constructor(source: Source, contentLength: Long) {
        this.source = source
        this.contentLength = contentLength
    }

    constructor(inputStream: InputStream, contentLength: Long) {
        this.inputStream = inputStream
        this.contentLength = contentLength
    }

    constructor(uri: Uri, contentLength: Long? = null) {
        this.uri = uri
        this.contentLength = contentLength ?: FileUtil.getFileLengthWithUri(APP.INSTANCE, uri)
    }

    constructor(file: File, breakpoint: Long = 0, contentLength: Long? = null) {
        this.file = file
        this.breakpoint = breakpoint
        this.contentLength = contentLength ?: file.length()
    }

    constructor(filePath: String, breakpoint: Long = 0, contentLength: Long? = null) {
        this.filePath = filePath
        this.breakpoint = breakpoint
        this.contentLength = contentLength ?: File(filePath).length()
    }

    fun createStandardSource(): Source? {
        source?.run { return this }
        inputStream?.run { return Okio.source(this) }
        uri?.run {
            return APP.INSTANCE.contentResolver.openInputStream(this)?.let { Okio.source(it) }
        }
        file?.run {
            return if (!exists()) null else Okio.source(this)
        }
        filePath?.run {
            return File(this).let { if (!it.exists()) null else Okio.source(it) }
        }
        return null
    }

    fun createBreakpointResumeSource(): RandomAccessFile? {
        val sourceFile = file ?: filePath?.let { File(it) }
        if (sourceFile == null || !sourceFile.exists()) return null
        return RandomAccessFile(sourceFile, "rw").apply {
            if (breakpoint > 0) seek(breakpoint)
        }
    }

    val isSupportBreakpointResume get() = breakpoint > 0 && createBreakpointResumeSource() != null

}

class DownloadManager {

    val httpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()

    fun upLoad(task: UploadTask) {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)//设置类型
        //追加参数
        task.parameters.forEach {
            when (it) {
                is UploadTaskParameter -> builder.addFormDataPart(it.name, it.value)
                is UploadFileParameter -> builder.addFormDataPart(
                    it.name,
                    it.fileName,
                    createUploadFileRequestBody(it)
                )
            }
        }
        httpClient.newCall(Request.Builder().url(task.url).post(builder.build()).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val string = response.body()?.string()
                    } else {
                    }
                }
            })
    }

    fun createUploadFileRequestBody(parameter: UploadFileParameter) =
        object : RequestBody() {
            override fun contentType(): MediaType? = parameter.fileMediaType

//            override fun contentLength(): Long = parameter.fileLength

            override fun writeTo(sink: BufferedSink) {
//                try {
//                    if (parameter.breakpoint > 0 && parameter.supportBreakpointResumeFile != null) {
//                        val randomAccessFile =
//                            RandomAccessFile(parameter.supportBreakpointResumeFile, "rw")
//                        val total = contentLength()
//                        randomAccessFile.seek(mAlreadyUpLength);
//                    } else {
//                        var source = parameter.fileSource ?: return
//                        val buf = Buffer()
//                        val total = contentLength()
//                        var current: Long = 0
//                        var len: Long
//                        while (source.read(buf, 2048).also { len = it } != -1L) {
//                            sink.write(buf, len)
//                            current += len
//                            // 回调进度
//                        }
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
            }

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