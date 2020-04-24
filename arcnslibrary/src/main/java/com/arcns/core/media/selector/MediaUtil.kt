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
import com.arcns.core.util.fileProviderAuthority
import java.io.File

const val MEDIA_MIME_TYPE_PREFIX_IMAGE = "image"
const val MEDIA_MIME_TYPE_PREFIX_VIDEO = "video"
const val MEDIA_MIME_TYPE_PREFIX_AUDIO = "audio"

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



typealias PhtotoListener = (String?) -> Unit
typealias FileUriListener = (Uri?) -> Unit

/**
 * 打开系统相机拍照和打开系统相册选择相片的工具类
 */
class MediaUtil(var fragment: Fragment) {

    private var captureSaveFilePath: String? = null
    private val requestCodeForImageCapture: Int = 101
    private val requestCodeForPhotoAlbum: Int = 102
    private val requestCodeForVideoCapture: Int = 103
    private val requestCodeForFileSelector: Int = 104
    private var phtotoListener: PhtotoListener? = null
    private var phtotoUriListener: FileUriListener? = null
    private var fileUriListener: FileUriListener? = null

    /**
     * 打开SAF文件选择器
     */
    fun onFileSelector(type: String = "*/*", fileUriListener: FileUriListener? = null) {
        this.fileUriListener = fileUriListener
        // 启动一个展示所有匹配到的document provider的选择器
        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        // 设置仅可打开过滤类型的文件
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        // 设置过滤类型
        intent.type = type
        fragment.startActivityForResult(
            intent,
            requestCodeForFileSelector
        )
    }

    /**
     * 打开系统相机拍照
     */
    fun onImageCapture(phtotoListener: PhtotoListener? = null) {
        this.captureSaveFilePath = null
        this.phtotoListener = phtotoListener
        var context = fragment.context
        if (context == null) {
            phtotoListener?.invoke(null)
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
    fun onVideoCapture(phtotoListener: PhtotoListener? = null) {
        this.captureSaveFilePath = null
        this.phtotoListener = phtotoListener
        var context = fragment.context
        if (context == null) {
            phtotoListener?.invoke(null)
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
    fun onPhotoAlbum(phtotoUriListener: FileUriListener? = null) {
        this.captureSaveFilePath = null
        this.phtotoUriListener = phtotoUriListener
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
                phtotoListener?.invoke(captureSaveFilePath)
                return true
            }
            requestCodeForPhotoAlbum -> {
                phtotoUriListener?.invoke(data?.data)
                return true
            }
            requestCodeForFileSelector -> {
                fileUriListener?.invoke(data?.data)
            }
        }
        return false
    }

}

fun getRandomPhotoCacheFilePath(suffixName: String = ".jpg"): String =
    getRandomCacheFilePath(suffixName)

fun getRandomVideoCacheFilePath(suffixName: String = ".mp4"): String =
    getRandomCacheFilePath(suffixName)

fun getRandomAudioCacheFilePath(suffixName: String = ".mp3"): String =
    getRandomCacheFilePath(suffixName)

fun getRandomCacheFilePath(suffixName: String): String =
    APP.INSTANCE.cacheDir?.absoluteFile.toString() + "/" + System.currentTimeMillis() + suffixName
