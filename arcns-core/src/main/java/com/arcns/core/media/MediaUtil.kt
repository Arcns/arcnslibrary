package com.arcns.core.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.arcns.core.APP
import com.arcns.core.file.MIME_TYPE_WILDCARD
import com.arcns.core.file.getRandomPhotoCacheFilePath
import com.arcns.core.file.getRandomVideoCacheFilePath
import com.arcns.core.util.fileProviderAuthority
import java.io.File
import java.lang.Exception


typealias PhtotoListener = (Intent?, String?, Int?) -> Unit
typealias FileUriListener = (Intent?, Uri?, Int?) -> Unit

/**
 * 资源库工具类（打开系统相机拍照和打开系统相册选择相片等）
 */
class MediaUtil(var fragment: Fragment) {

    private var captureSaveFilePath: String? = null
    private val requestCodeForImageCapture: Int = 101
    private val requestCodeForPhotoAlbum: Int = 102
    private val requestCodeForVideoCapture: Int = 103
    private val requestCodeForOpenFileSelector: Int = 104
    private val requestCodeForCreateFileSelector: Int = 105
    private var phtotoListener: PhtotoListener? = null
    private var photoUriListener: FileUriListener? = null
    private var fileUriListener: FileUriListener? = null
    private var listeneRequestCode: Int? = null


    /**
     * SAF创建文件选择器
     */
    fun onCreateFileSelector(
        fileName: String,
        fileMimeType: String = MIME_TYPE_WILDCARD,
        resultCode: Int? = null,
        onCustomIntent: ((Intent) -> Unit)? = null,
        fileUriListener: FileUriListener? = null
    ) {
        this.listeneRequestCode = resultCode
        this.fileUriListener = fileUriListener
        // 启动一个用于选择创建文件的目录的选择器
        var intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        // 设置文件类型
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = fileMimeType
        // 设置默认文件名
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        onCustomIntent?.invoke(intent)
        fragment.startActivityForResult(
            intent,
            requestCodeForCreateFileSelector
        )
    }

    /**
     * SAF打开文件选择器
     */
    fun onOpenFileSelector(
        fileMimeType: String = MIME_TYPE_WILDCARD,
        resultCode: Int? = null,
        onCustomIntent: ((Intent) -> Unit)? = null,
        fileUriListener: FileUriListener? = null
    ) {
        this.listeneRequestCode = resultCode
        this.fileUriListener = fileUriListener
        // 启动一个展示所有匹配到的document provider的选择器
        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        // 设置仅可打开过滤类型的文件
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        // 设置过滤类型
        intent.type = fileMimeType
        onCustomIntent?.invoke(intent)
        fragment.startActivityForResult(
            intent,
            requestCodeForOpenFileSelector
        )
    }

    /**
     * 打开系统相机拍照
     */
    fun onImageCapture(
        resultCode: Int? = null,
        onCustomIntent: ((Intent) -> Unit)? = null,
        phtotoListener: PhtotoListener? = null
    ) {
        this.captureSaveFilePath = null
        this.listeneRequestCode = resultCode
        this.phtotoListener = phtotoListener
        var context = fragment.context
        if (context == null) {
            phtotoListener?.invoke(null, null, listeneRequestCode)
            this.phtotoListener = null
            return
        }
        val saveFileUri: Uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var saveFile = File(getRandomPhotoCacheFilePath())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 安卓N及以上需要通过文件提供者把路径提供给系统相机
            saveFileUri = FileProvider.getUriForFile(
                context,
                context.fileProviderAuthority,
                saveFile
            )
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            captureSaveFilePath = saveFile.absolutePath
        } else {
            saveFileUri = Uri.fromFile(saveFile)
            captureSaveFilePath = saveFileUri.encodedPath
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, saveFileUri)
        onCustomIntent?.invoke(intent)
        fragment.startActivityForResult(intent, requestCodeForImageCapture)
    }

    /**
     * 打开系统相机录制视频
     */
    fun onVideoCapture(
        resultCode: Int? = null,
        onCustomIntent: ((Intent) -> Unit)? = null,
        phtotoListener: PhtotoListener? = null
    ) {
        this.captureSaveFilePath = null
        this.listeneRequestCode = resultCode
        this.phtotoListener = phtotoListener
        var context = fragment.context
        if (context == null) {
            phtotoListener?.invoke(null, null, listeneRequestCode)
            this.phtotoListener = null
            return
        }
        val saveFileUri: Uri
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        var saveFile = File(getRandomVideoCacheFilePath())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 安卓N及以上需要通过文件提供者把路径提供给系统相机
            saveFileUri = FileProvider.getUriForFile(
                context,
                context.fileProviderAuthority,
                saveFile
            )
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            captureSaveFilePath = saveFile.absolutePath
        } else {
            saveFileUri = Uri.fromFile(saveFile)
            captureSaveFilePath = saveFileUri.encodedPath
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, saveFileUri)
        onCustomIntent?.invoke(intent)
        fragment.startActivityForResult(intent, requestCodeForVideoCapture)
    }

    /**
     * 打开系统相册选择相片
     */
    fun onPhotoAlbum(
        resultCode: Int? = null,
        onCustomIntent: ((Intent) -> Unit)? = null,
        phtotoUriListener: FileUriListener? = null
    ) {
        this.captureSaveFilePath = null
        this.listeneRequestCode = resultCode
        this.photoUriListener = phtotoUriListener
        fragment.startActivityForResult(Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            onCustomIntent?.invoke(this)
        }, requestCodeForPhotoAlbum);
    }

    /**
     * 回调处理，注意一定要在onActivityResult中回调该方法，PhtotoListener才能正确被回调
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode == Activity.RESULT_CANCELED) {
            return false
        }
        when (requestCode) {
            requestCodeForImageCapture, requestCodeForVideoCapture -> {
                phtotoListener?.invoke(data, captureSaveFilePath, listeneRequestCode)
                phtotoListener = null
                return true
            }
            requestCodeForPhotoAlbum -> {
                photoUriListener?.invoke(data, data?.data, listeneRequestCode)
                photoUriListener = null
                return true
            }
            requestCodeForOpenFileSelector, requestCodeForCreateFileSelector -> {
                fileUriListener?.invoke(data, data?.data, listeneRequestCode)
                fileUriListener = null
            }
        }
        return false
    }

}


/**
 * 获取Uri文件的播放时长
 */
val Uri.duration: Long
    get() {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(APP.INSTANCE, this)
        val duration =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                .toLongOrNull()
        retriever.release()
        return duration ?: -1
    }


/**
 * 获取Uri文件的播放时长
 */
val Uri.durationOrNull: Long?
    get() = duration.let { if (it == -1L) null else it }