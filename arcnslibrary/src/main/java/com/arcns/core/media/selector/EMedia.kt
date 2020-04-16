package com.arcns.core.media.selector

import android.net.Uri
import com.arcns.core.R
import com.arcns.core.util.dimen
import com.arcns.core.util.file.FileUtil
import java.lang.Exception

data class EMedia(
    /**
     * 媒体文件 id
     */
    var id: Long? = null,
    /**
     * 媒体文件名
     */
    var name: String? = null,
    /**
     * 媒体文件uri
     */
    var uri: Uri? = null,
    /**
     * 媒体文件路径
     */
    var path: String? = null,
    /**
     * 媒体文件添加时间
     */
    var added: Long? = null,
    /**
     * 媒体文件minetype类型
     */
    var mimeType: String? = null,
    /**
     * 媒体文件播放持续时间
     */
    var duration: Long? = null,
    /**
     * 媒体文件大小
     */
    var size: Long? = null,
    /**
     * 用于在adapter中适应多个相同媒体文件的场景
     */
    var itemID: String? = null,
    /**
     * 完成选择后另外保存的文件路径
     */
    var saveAsPath: String? = null,
    /**
     * 完成选择后另外保存文件时若发生错误将把异常存储到该变量中
     */
    var saveAsException: Exception? = null
) {
    /**
     * 是否选中
     */
    var isSelected = false

    /**
     * id转string格式
     */
    val idToString: String get() = id.toString()

    /**
     * 文件名
     */
    val nameToString: String get() = name ?: FileUtil.getFileName(path ?: "")

    /**
     * 文件后缀名
     */
    val suffix: String get() = FileUtil.getFileSuffix(name) ?: FileUtil.getFileNameNotSuffix(path)

    /**
     * 文件值 uri优先于path
     */
    val value: Any? = uri ?: path

    /**
     * 是否为gif图片
     */
    val isGif: Boolean
        get() = ((name?.endsWith(".gif", true) ?: false) || (path?.endsWith(".gif", true) ?: false))

    /**
     * 持续时间，格式为 分:秒
     */
    val durationToString: String
        get() {
            if (duration == null) return "--:--"
            val totalSeconds = duration!! / 1000
            return "${totalSeconds / 60}:" + (totalSeconds % 60).let {
                if (it < 10) "0$it" else "$it"
            }
        }

    /**
     * 是否为图片类型
     */
    val isImage: Boolean
        get() {
            if (mimeType == null) {
                val value = name ?: path ?: return false
                return FileUtil.isImageSuffix(value)
            }
            return mimeType?.startsWith(MEDIA_MIME_TYPE_PREFIX_IMAGE, true) ?: false
        }

    /**
     * 是否为视频类型
     */
    val isVideo: Boolean
        get() {
            if (mimeType == null) {
                val value = name ?: path ?: return false
                return FileUtil.isVideoSuffix(value)
            }
            return mimeType?.startsWith(MEDIA_MIME_TYPE_PREFIX_VIDEO, true) ?: false
        }

    /**
     * 是否为音频类型
     */
    val isAudio: Boolean
        get() {
            if (mimeType == null) {
                val value = name ?: path ?: return false
                return FileUtil.isAudioSuffix(value)
            }
            return mimeType?.startsWith(MEDIA_MIME_TYPE_PREFIX_AUDIO, true) ?: false
        }

    /**
     * 获取媒体文件的MimeType，如果为空则根据后缀名来获取MimeType
     */
    val mimeTypeIfNullGetOfSuffix: String
        get() = mimeType ?: if (isImage) "$MEDIA_MIME_TYPE_PREFIX_IMAGE/*"
        else if (isVideo) "$MEDIA_MIME_TYPE_PREFIX_VIDEO/*"
        else if (isAudio) "$MEDIA_MIME_TYPE_PREFIX_AUDIO/*"
        else "*/*"
}

data class ESelectedMedia(
    /**
     * 媒体文件
     */
    var media: EMedia,
    /**
     * 是否为当前媒体文件
     */
    var isCurrentMedia: Boolean = false
)

data class EMediaSaveAsOption(
    /**
     * 是否开启转存，默认为true
     */
    var enable: Boolean = true,
    /**
     * 转存文件的宽度，默认为详情大图所设置的宽度，为0时将保持图片原有大小
     */
    var width: Float? = null,
    /**
     * 转存文件的高度，默认为详情大图所设置的高度，为0时将保持图片原有大小
     */
    var height: Float? = null,
    /**
     * 转存文件的大小，默认为详情大图所设置的大小，为0时将保持图片原有大小
     */
    var size: Float? = null,
    /**
     * 是否转存为原图（将忽略宽、高和大小设置）
     */
    var isOriginal: Boolean = false,
    /**
     * 是否自适应全部显示（默认为false：即自适应填充满）
     */
    var centerInside: Boolean = false,
    /**
     * 是否转存为高清图片，默认为true
     */
    var highQualityBitmap: Boolean = true,
    /**
     * 压缩质量 0-100
     */
    var compressQuality: Int = 80
) {
    val saveAsWidth: Float
        get() = if (isOriginal) 0f else width ?: size
        ?: R.dimen.media_selector_media_details_width.dimen

    val saveAsHeight: Float
        get() = if (isOriginal) 0f else height ?: size ?: 0f

    val saveAsCompressQuality =
        if (compressQuality > 100) 100 else if (compressQuality < 0) 0 else compressQuality
}

data class EMediaQuery(
    var queryContentUri: Uri,
    var querySelection: String? = null
)