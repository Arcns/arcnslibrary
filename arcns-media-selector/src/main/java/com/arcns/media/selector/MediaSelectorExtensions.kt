package com.arcns.media.selector

import android.annotation.SuppressLint
import android.content.ContentUris
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.arcns.core.APP
import com.arcns.core.media.durationOrNull
import com.arcns.core.util.toArrayList


/**
 * 从系统媒体库获取媒体列表
 */
fun getMediasFromMediaStore(
    vararg mediaQuerys: EMediaQuery = arrayOf(
        EMediaQuery(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ),
        EMediaQuery(
            MediaStore.Video.Media.INTERNAL_CONTENT_URI
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

/**
 * 从系统媒体库获取媒体列表
 */
@SuppressLint("Range")
fun getMediasFromMediaStore(medias: ArrayList<EMedia>, mediaQuery: EMediaQuery): ArrayList<EMedia> {
    val queryProjection = arrayListOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATA
    ).apply {
        when (mediaQuery.queryContentUri) {
            // 只有视频和音频才有持续时间
            MediaStore.Video.Media.INTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Files.FileColumns.DURATION)
                }
        }
        // 添加额外查询参数
        mediaQuery.extraQueryProjection?.forEach {
            if (!contains(it)) add(it)
        }
    }.toTypedArray()
    val cursor = APP.INSTANCE.contentResolver.query(
        mediaQuery.queryContentUri,
        queryProjection,
        mediaQuery.querySelection,
        null,
        "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    )
    loop@ while (cursor?.moveToNext() == true) {
        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
        val uri = ContentUris.withAppendedId(mediaQuery.queryContentUri, id)
        // 只有视频和音频才有持续时间
        var duration: Long? = null
        when (mediaQuery.queryContentUri) {
            MediaStore.Video.Media.INTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    duration =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION))
                }
                if (duration == null) {
                    duration = uri.durationOrNull
                }
                if (duration == null) {
                    // 过滤掉无持续时间的视频和音频
                    continue@loop
                }
            }
            else -> Unit
        }
        val media = EMedia(
            id = id,
            name = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)),
            uri = uri,
            added = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)),
            mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)),
            size = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)),
            duration = duration
        ).apply {
            if (!mediaQuery.extraQueryProjection.isNullOrEmpty()) {
                // 获取额外查询参数的值
                val values = HashMap<String, String?>()
                mediaQuery.extraQueryProjection?.forEach {
                    values[it] = cursor.getStringOrNull(cursor.getColumnIndex(it))
                }
                extraValues = values
            }
        }
        if (mediaQuery.onFilter?.invoke(media) == false) {
            continue@loop // 过滤
        }
        medias.add(media)
    }
    if (cursor?.isClosed == false) {
        cursor.close()
    }
    return medias
}