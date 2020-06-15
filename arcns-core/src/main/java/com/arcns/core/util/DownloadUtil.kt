package com.arcns.core.util

import android.app.DownloadManager
import android.app.Service
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.edit
import com.arcns.core.APP
import com.google.gson.Gson
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

    // 缓存的下载任务ID列表（url,id）
    private val DB_CACHED_DOWNLOAD_IDS = "DB_CACHED_DOWNLOAD_IDS"
    private val KEY_CACHED_DOWNLOAD_IDS = "KEY_CACHED_DOWNLOAD_IDS"
    private var cachedDownloadIDs = HashMap<String, Long>()

    init {
        initCachedDownloadIDs()
    }

    /**
     * 获取缓存的下载任务ID列表
     */
    private fun initCachedDownloadIDs() {
        var value = APP.CONTEXT.getSharedPreferences(DB_CACHED_DOWNLOAD_IDS, Context.MODE_PRIVATE)
            .getString(
                KEY_CACHED_DOWNLOAD_IDS,
                null
            )
        var ids: HashMap<String, Double>? = Gson().tryFromJson(value)
        ids?.forEach {
            if (checkDownloadTaskHasExistsByID(it.value.toLong())) {
                cachedDownloadIDs[it.key] = it.value.toLong()
            }
        }
        saveCachedDownloadIDs()
    }

    /**
     * 缓存下载任务ID列表
     */
    private fun saveCachedDownloadIDs() =
        APP.CONTEXT.getSharedPreferences(
            DB_CACHED_DOWNLOAD_IDS,
            Context.MODE_PRIVATE
        ).edit {
            putString(KEY_CACHED_DOWNLOAD_IDS, Gson().toJson(cachedDownloadIDs))
        }

    /**
     * 添加任务ID到缓存列表
     */
    private fun addCachedDownloadID(downloadTask: DownloadTask) {
        cachedDownloadIDs[downloadTask.downloadUrl] = downloadTask.downloadId ?: return
        saveCachedDownloadIDs()
    }

    /**
     * 获取任务ID
     */
    fun getDownloadTaskID(downloadUrl: String): Long? = downloadTasks[downloadUrl]?.downloadId
        ?: cachedDownloadIDs[downloadUrl] ?: null

    /**
     * 通过url获取下载任务状态
     */
    fun getDownloadTaskStatusByUrl(downloadUrl: String): Int? {
        return getDownloadTaskStatusByID(getDownloadTaskID(downloadUrl) ?: return null)
    }

    /**
     * 通过id获取下载任务状态
     */
    fun getDownloadTaskStatusByID(id: Long): Int? {
        var cursor = downloadManager.query(DownloadManager.Query().apply {
            setFilterById(id)
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
     * 通过url检查下载任务是否存在
     */
    fun checkDownloadTaskHasExistsByUrl(downloadUrl: String): Boolean {
        return checkDownloadTaskHasExistsByID(getDownloadTaskID(downloadUrl) ?: return false)
    }

    /**
     * 通过id检查下载任务是否存在
     */
    fun checkDownloadTaskHasExistsByID(id: Long): Boolean {
        var state = getDownloadTaskStatusByID(id)
        when (state) {
            DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> return true
        }
        return false
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
        if (!downloadTask.isAllowDuplicate && checkDownloadTaskHasExistsByUrl(downloadTask.downloadUrl)) {
            return
        }
//        var existing = run checkAllowDuplicate@{
//            if (!downloadTask.isAllowDuplicate) {
//                var state = getDownloadTaskStatusByUrl(downloadTask.downloadUrl)
//                when (state) {
//                    DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> return@checkAllowDuplicate true
//                }
//            }
//            return@checkAllowDuplicate false
//        }
//        if (existing) {
//            return
//        }

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
        addCachedDownloadID(downloadTask)
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