package com.arcns.media.selector

import android.content.ContentUris
import android.provider.MediaStore
import com.arcns.core.APP


/**
 * 从系统媒体库获取媒体列表
 */
fun getMediasFromMediaStore(
    vararg mediaQuerys: EMediaQuery = arrayOf(
        EMediaQuery(
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

/**
 * 从系统媒体库获取媒体列表
 */
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