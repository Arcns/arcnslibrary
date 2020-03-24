package com.arcns.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.arcns.core.APP
import com.arcns.core.util.file.FileUtil
import java.io.File

typealias PhtotoListener = (String?) -> Unit

/**
 * 打开系统相机拍照和打开系统相册选择相片的工具类
 */
class PhotoUtil(var fragment: Fragment) {

    private var photographSaveFilePath: String? = null
    private val requestCodeForPhotograph: Int = 101
    private val requestCodeForPhotoAlbum: Int = 102
    private var phtotoListener: PhtotoListener? = null

    private fun getRandomPhotoCacheFilePath(context: Context?): String =
        context?.cacheDir?.absoluteFile.toString() + "/" + System.currentTimeMillis() + ".jpg"

    /**
     * 打开系统相机拍照
     */
    fun onPhotograph(phtotoListener: PhtotoListener? = null) {
        this.photographSaveFilePath = null
        this.phtotoListener = phtotoListener
        var context = fragment.context
        if (context == null) {
            phtotoListener?.invoke(null)
            return
        }
        val photographSaveFileUri: Uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var photographSaveFile = File(getRandomPhotoCacheFilePath(context))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 安卓N及以上需要通过文件提供者把路径提供给系统相机
            photographSaveFileUri = FileProvider.getUriForFile(
                context,
                context.fileProviderAuthority,
                photographSaveFile
            )
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            photographSaveFilePath = photographSaveFile.absolutePath
        } else {
            photographSaveFileUri = Uri.fromFile(photographSaveFile)
            photographSaveFilePath = photographSaveFileUri.encodedPath
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photographSaveFileUri)
        fragment.startActivityForResult(intent, requestCodeForPhotograph)
    }

    /**
     * 打开系统相册选择相片
     */
    fun onPhotoAlbum(phtotoListener: PhtotoListener? = null) {
        this.photographSaveFilePath = null
        this.phtotoListener = phtotoListener
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
            requestCodeForPhotograph -> {
                phtotoListener?.invoke(photographSaveFilePath)
                return true
            }
            requestCodeForPhotoAlbum -> {
                var saveFilePath = getRandomPhotoCacheFilePath(fragment.context)
                var saveResult = FileUtil.saveFileWithUri(
                    APP.CONTEXT,
                    data?.data,
                    FileUtil.getFileDirectory(saveFilePath),
                    FileUtil.getFileName(saveFilePath)
                )
                phtotoListener?.invoke(if (saveResult) saveFilePath else null)
                return true
            }
        }
        return false
    }

}