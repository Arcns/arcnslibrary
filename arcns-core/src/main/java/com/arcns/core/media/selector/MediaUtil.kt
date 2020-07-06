package com.arcns.core.media.selector

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.arcns.core.APP
import com.arcns.core.util.file.MIME_TYPE_WILDCARD
import com.arcns.core.util.file.getRandomPhotoCacheFilePath
import com.arcns.core.util.file.getRandomVideoCacheFilePath
import com.arcns.core.util.fileProviderAuthority
import java.io.File
import kotlin.collections.ArrayList


fun getMediasFromMediaStore(
    vararg mediaQuerys: EMediaQuery = arrayOf(
        com.arcns.core.media.selector.EMediaQuery(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
    )
): ArrayList<EMedia> {
    var medias = ArrayList<EMedia>()
    if (mediaQuerys.isEmpty()) {
        return medias
    }
    mediaQuerys.forEach {
        getMediasFromMediaStore(medias, it)
    }
    if (mediaQuerys.size > 1) {
        medias.sortByDescending {
            // 按时间进行倒序排序
            it.added
        }
    }
    return medias
}


fun getMediasFromMediaStore(medias: ArrayList<EMedia>, mediaQuery: EMediaQuery): ArrayList<EMedia> {
    val queryProjection = when (mediaQuery.queryContentUri) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DURATION,// 只有视频和音频才有持续时间
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA
        )
        else -> arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA
        )
    }
    val cursor = APP.INSTANCE.contentResolver.query(
        mediaQuery.queryContentUri,
        queryProjection,
        mediaQuery.querySelection,
        null,
        "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    )
    while (cursor?.moveToNext() == true) {
        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
        medias.add(
            EMedia(
                id = id,
                name = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)),
                uri = ContentUris.withAppendedId(mediaQuery.queryContentUri, id),
                added = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)),
                mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)),
                size = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE))
            ).apply {
                when (mediaQuery.queryContentUri) {
                    // 只有视频和音频才有持续时间
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> {
                        duration =
                            cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION))
                    }
                    else -> Unit
                }
            })
    }
    if (cursor?.isClosed == false) {
        cursor.close()
    }
    return medias
}



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
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
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
        fragment.startActivityForResult(
            intent,
            requestCodeForOpenFileSelector
        )
    }

    /**
     * 打开系统相机拍照
     */
    fun onImageCapture(resultCode: Int? = null, phtotoListener: PhtotoListener? = null) {
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
        fragment.startActivityForResult(intent, requestCodeForImageCapture)
    }

    /**
     * 打开系统相机录制视频
     */
    fun onVideoCapture(resultCode: Int? = null, phtotoListener: PhtotoListener? = null) {
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
        fragment.startActivityForResult(intent, requestCodeForVideoCapture)
    }

    /**
     * 打开系统相册选择相片
     */
    fun onPhotoAlbum(resultCode: Int? = null, phtotoUriListener: FileUriListener? = null) {
        this.captureSaveFilePath = null
        this.listeneRequestCode = resultCode
        this.photoUriListener = phtotoUriListener
        fragment.startActivityForResult(Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
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

