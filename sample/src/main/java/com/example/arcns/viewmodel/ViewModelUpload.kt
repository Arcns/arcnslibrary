package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arcns.core.APP
import com.arcns.core.file.FileUtil
import com.arcns.core.file.cacheDirPath
import com.arcns.core.file.getCurrentDateTimeFileName
import com.arcns.core.file.getCurrentTimeMillisFileName
import com.arcns.core.network.*
import com.arcns.core.util.Event
import com.arcns.core.util.LOG
import com.arcns.core.util.eventValue
import com.example.arcns.R
import com.example.arcns.data.network.NetworkDataSource
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException


class ViewModelUpload : ViewModel() {


    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    // 上传任务管理器数据
    val uploadManagerData = UploadManagerData()

    // 任务点击
    private var _eventTaskClick = MutableLiveData<Event<UploadTask>>()
    var eventTaskClick: LiveData<Event<UploadTask>> = _eventTaskClick
    fun onEventTaskClick(task: UploadTask) {
        _eventTaskClick.eventValue = task
    }

    // 添加任务
    fun addUploadTask() {

        val fileName2 = "test.jpg"
        val file2 = File(cacheDirPath + File.separator + fileName2)
        if (!file2.exists()) {
            FileUtil.copyFile(APP.INSTANCE.assets.open(fileName2), cacheDirPath, fileName2)
            LOG("UploadTest source2 copy ok " + file2.length())
        } else {
            LOG("UploadTest source2 file exists " + file2.length())
        }

        uploadManagerData.upload(
            UploadTask(
                url = "https://api.imgur.com/3/upload",
                parameters = arrayListOf(
                    UploadTaskFileParameter(
//                        name = "video",
                        name = "image",
                        fileName = fileName2,
                        filePath = file2.absolutePath
                    )
                ),
                onCustomRequest = { task, requestBuilder ->
                    requestBuilder.addHeader("Authorization", "Client-ID {{16070e7eb7aa4d6}}")
                },
                notificationOptions = UploadNotificationOptions(
                    smallIcon = R.drawable.ic_download
                )
            )
        )
    }

    fun addUploadTask2(){
        val fileName1old = "test.mp4"
        val fileName1 = getCurrentDateTimeFileName(".mp4")
        val file1 = File(cacheDirPath + File.separator + fileName1)
        if (!file1.exists()) {
            FileUtil.copyFile(
                APP.INSTANCE.assets.open(fileName1old),
                cacheDirPath,
                fileName1
            )
            LOG("UploadTest source1 copy ok " + file1.length())
        } else {
            LOG("UploadTest source1 file exists " + file1.length())
        }

        uploadManagerData.upload(
            UploadTask(
                url = "https://api.imgur.com/3/upload",
                parameters = arrayListOf(
                    UploadTaskFileParameter(
                        name = "video",
//                        name = "image",
                        fileName = fileName1,
                        filePath = file1.absolutePath
                    )
                ),
                onCustomRequest = { task, requestBuilder ->
                    requestBuilder.addHeader("Authorization", "Client-ID {{16070e7eb7aa4d6}}")
                },
                notificationOptions = UploadNotificationOptions(
                    smallIcon = R.drawable.ic_download
                )
            )
        )
    }

    fun addUploadTask3(){
//        val client = OkHttpClient().newBuilder()
//            .build()
//        val mediaType = "text/plain".toMediaTypeOrNull()
//        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
//            .addFormDataPart(
//                "image",
//                fileName2,
//                file2.asRequestBody()
//            )
////                .addFormDataPart(
////                    "video",
////                    fileName1,
////                    file1.asRequestBody()
////                )
//            .build()
//        val request = Request.Builder()
//            .url("https://api.imgur.com/3/upload")
//            .method("POST", body)
//            .addHeader("Authorization", "Client-ID {{16070e7eb7aa4d6}}")
//            .build()
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                LOG("UploadTest error " + e.message)
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                LOG("UploadTest " + response.isSuccessful + "  " + response.body?.string())
//            }
//
//        })
    }

    fun clear() {
        uploadManagerData?.cancelAllTask(isContainsStop = true)
    }


}