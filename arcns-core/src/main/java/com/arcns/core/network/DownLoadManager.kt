package com.arcns.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.arcns.core.R
import com.arcns.core.app.NotificationOptions
import com.arcns.core.app.NotificationProgressOptions
import com.arcns.core.app.randomNotificationID
import com.arcns.core.util.string
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


// 下载任务进度事件
typealias OnDownloadProgressUpdate = (DownLoadTask, TaskProgress) -> Unit


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

