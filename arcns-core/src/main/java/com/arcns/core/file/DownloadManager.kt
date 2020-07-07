package com.arcns.core.file

import android.net.Uri
import com.arcns.core.APP
import okhttp3.*
import okio.Buffer
import okio.BufferedSink
import okio.Okio
import okio.Source
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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
    var fileSourcePath: Any, //支持格式：filePath(string)，fileUri(uri)
    var fileMimeType: String // mimeType，例如 application/octet-stream
) : UploadTaskBaseParameter(
    name = name
) {
    val fileMediaType: MediaType?
        get() = MediaType.parse(fileMimeType) ?: MediaType.parse(MIME_TYPE_APPLICATION_OCTET_STREAM)

    val fileSource: Source?
        get() = when (fileSourcePath) {
            is Uri -> APP.INSTANCE.contentResolver.openInputStream(fileSourcePath as Uri)
                ?.let { Okio.source(it) }
            is String -> File(fileSourcePath as String).let {
                if (!it.exists()) null else Okio.source(
                    it
                )
            }
            else -> null
        }
}

class DownloadManager {

    val httpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()

    fun upLoad(task: UploadTask) {
        val builder = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)//设置类型
        }
        //追加参数
        task.parameters.forEach {
            when (it) {
                is UploadTaskParameter -> builder.addFormDataPart(it.name, it.value)
                is UploadFileParameter -> builder.addFormDataPart(
                    it.name,
                    it.fileName,
                    object : RequestBody() {
                        override fun contentType(): MediaType? = it.fileMediaType

                        override fun writeTo(sink: BufferedSink) {
                            try {
                                var source = it.fileSource ?: return
                                val buf = Buffer()
                                val total = contentLength()
                                var current: Long = 0
                                var len: Long
                                while (source.read(buf, 2048).also { len = it } != -1L) {
                                    sink.write(buf, len)
                                    current += len
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                    })
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