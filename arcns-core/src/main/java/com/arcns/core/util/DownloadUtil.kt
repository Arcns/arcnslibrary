package com.arcns.core.util

import android.app.DownloadManager
import android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import com.arcns.core.APP
import org.greenrobot.eventbus.EventBus
import java.io.Serializable

val DOWNLOAD_SAVE_NAME_SUFFIX_APK = ".apk"

data class DownloadTask(
    // 下载地址
    val downloadUrl: String,
    // 下载标题
    val downloadTitle: String,
    // 下载简介
    val downloadDescription: String,
    // 下载保存文件夹（默认为O以上公共目录下载文件夹、O以下私有目录下载文件夹）
    var downloadSaveDir: String = Environment.DIRECTORY_DOWNLOADS,
    // 下载保存文件名（默认为当前时间戳）
    var downloadSaveName: String? = null,
    // 下载保存文件后缀名（默认为空）
    var downloadSaveNameSuffix: String? = null,
    // 是否允许重复下载，默认为不允许
    var isAllowDuplicate: Boolean = false,
    // 下载完成后是否自动打开
    var isAutoOpen: Boolean = false,
    // 下载错误提示
    val downloadFailedTips: String? = null
) : Serializable {
    var downloadId: Long? = null
    var downloadStatus: Int? = null
}

class DownloadUtil(var context: Context) {

    private var downloadTasks = HashMap<String, DownloadTask>()
    private var downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun getDownloadTaskStatus(downloadUrl: String): Int? {
        var existingDownloadId = downloadTasks[downloadUrl]?.downloadId
            ?: return null
        var cursor = downloadManager.query(DownloadManager.Query().apply {
            setFilterById(existingDownloadId)
        })
        if (cursor.moveToFirst()) {
            val state =
                cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            cursor.close()
            return state
        }
        return null
    }

    /**
     * 开始下载任务
     */
    fun startDownloadTask(
        downloadTask: DownloadTask
    ) {
        // 安卓Q疑似与downloadManager存在bug，暂时调用浏览器直接下载
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            openBrowserDownloadApk(downloadApkUrl)
//            return
//        }


        // 防止重复提交下载任务
        var existing = run checkAllowDuplicate@{
            if (!downloadTask.isAllowDuplicate) {
                var state = getDownloadTaskStatus(downloadTask.downloadUrl)
                when (state) {
                    DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> return@checkAllowDuplicate true
                }
            }
            return@checkAllowDuplicate false
        }
        if (existing) {
            return
        }

        // 添加到下载任务列表
        downloadTasks[downloadTask.downloadUrl] = downloadTask

        // 下载保存的文件夹和文件名
        val saveDir = downloadTask.downloadSaveDir
        val saveName = downloadTask.downloadSaveName
            ?: (System.currentTimeMillis().toString() + (downloadTask.downloadSaveNameSuffix ?: ""))
        // 设置下载路径
        var request = DownloadManager.Request(Uri.parse(downloadTask.downloadUrl))
        // 设置文件名和目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // android Q以上必须使用公共目录，否则需要调用文件框架供用户手动选择目录
            request.setDestinationInExternalPublicDir(saveDir, saveName)
        } else {
            // 如果android Q以下，我们则使用内部目录即可
            request.setDestinationInExternalFilesDir(context, saveDir, saveName)
        }
        // 配置wifi、移动网络均可下载
        request.setAllowedNetworkTypes(
            DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI
        )
        // 显示下载进度到通知栏
        request.setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        // 配置通知栏下载标题与描述
        request.setTitle(downloadTask.downloadTitle)
        request.setDescription(downloadTask.downloadDescription)
        // 启动下载任务
        downloadTask.downloadId = downloadManager.enqueue(request)
        //注册广播接收者，监听下载状态
        context.registerReceiver(
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    // 根据id获取下载任务
    fun getDownloadTaskByID(id: Long): DownloadTask? {
        downloadTasks.values.forEach {
            if (it.downloadId == id) {
                return it
            }
        }
        return null
    }

    // 发送下载完成事件
    fun sendDownloadCompleteEvent(id: Long, status: Int) =
        sendDownloadCompleteEvent(getDownloadTaskByID(id), status)

    // 发送下载完成事件
    fun sendDownloadCompleteEvent(downloadTask: DownloadTask?, status: Int) = downloadTask?.run {
        downloadStatus = status
        EventBus.getDefault().post(this)
    }

    /**
     * 下载状态广播接收者
     */
    private var downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val currentId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) ?: return
            var cursor = downloadManager.query(DownloadManager.Query().apply {
                setFilterById(currentId)
            })
            if (!cursor.moveToFirst()) {
                return
            }
            val currentStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            when (currentStatus) {
                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> Unit
                DownloadManager.STATUS_SUCCESSFUL -> { // 下载成功
                    // android N以上需要接收content协议的Uri，而android N以下则需要file协议的Uri
                    var uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        downloadManager.getUriForDownloadedFile(currentId)
                    else Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))
                    // 关闭游标
                    cursor.close()
                    // 获取相应下载任务
                    var downloadTask = getDownloadTaskByID(currentId)
                    // 自动安装
                    if (downloadTask?.isAutoOpen == true) {
                        if (DOWNLOAD_SAVE_NAME_SUFFIX_APK.equals(
                                downloadTask.downloadSaveNameSuffix,
                                true
                            )
                        ) {
                            // 安装APK
                            onInstallApk(context, uri)
                        }
                    }
                    // 解除广播接收
                    context?.unregisterReceiver(this)
                    // 发送下载完成事件
                    sendDownloadCompleteEvent(downloadTask, currentStatus)
                }
                DownloadManager.STATUS_FAILED -> { // 下载错误
                    // 关闭游标
                    cursor.close();
                    // 获取相应下载任务
                    var downloadTask = getDownloadTaskByID(currentId)
                    // 弹出错误提示
                    downloadTask?.downloadFailedTips?.run {
                        Toast.makeText(
                            context,
                            this,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    // 解除广播接收
                    context?.unregisterReceiver(this)
                    // 发送下载完成事件
                    sendDownloadCompleteEvent(downloadTask, currentStatus)
                }
            }
        }
    }

    // 解除广播接收
    fun unregisterDownloadCompleteReceiver() = try {
        context?.unregisterReceiver(downloadCompleteReceiver)
    } catch (e: Exception) {
    }

    /**
     * 安装APK
     */
    fun onInstallApk(context: Context?, apkUri: Uri) {
        var intent = Intent(Intent.ACTION_VIEW);
        val type = "application/vnd.android.package-archive"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // android N以上在设置安装Uri时必须获取Uri权限，否则无法正常安装
//        var apkUri = FileProvider.getUriForFile(context, getFileProviderAuthority(context), apkFile)
            intent.setDataAndType(apkUri, type);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            // anddroid N以下可以直接设置安装Uri
            intent.setDataAndType(apkUri, type)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context?.startActivity(intent)
    }

    fun openBrowserDownload(downloadApkUrl: String) =
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadApkUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
}


/**
 * 下载服务
 */
class DownloadService : Service() {
    private val downloadUtil: DownloadUtil by lazy {
        return@lazy DownloadUtil(APP.CONTEXT)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        run startDownloadApk@{
            var downloadTask =
                intent?.getSerializableExtra(dataNameFordownloadTask) as? DownloadTask
                    ?: return@startDownloadApk
            downloadUtil.startDownloadTask(downloadTask)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        downloadUtil.unregisterDownloadCompleteReceiver()
        super.onDestroy()
    }

    companion object {
        private const val dataNameFordownloadTask: String = "dataNameFordownloadTask"
        fun startService(
            context: Context?,
            downloadTask: DownloadTask
        ) {
            context?.startService(Intent(context, DownloadService::class.java).apply {
                putExtra(dataNameFordownloadTask, downloadTask)
            })
        }
    }
}