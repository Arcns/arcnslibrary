package com.arcns.core.util.file

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.arcns.core.APP
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val MIME_TYPE_PREFIX_IMAGE = "image/"
const val MIME_TYPE_PREFIX_VIDEO = "video/"
const val MIME_TYPE_PREFIX_AUDIO = "audio/"
const val MIME_TYPE_WILDCARD = "*/*"

// application
const val MIME_TYPE_APPLICATION_APK = "application/vnd.android.package-archive"
const val MIME_TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream"
const val MIME_TYPE_APPLICATION_DOC = "application/msword"
const val MIME_TYPE_APPLICATION_GTAR = "application/x-gtar"
const val MIME_TYPE_APPLICATION_GZ = "application/x-gzip"
const val MIME_TYPE_APPLICATION_JAR = "application/java-archive"
const val MIME_TYPE_APPLICATION_JS = "application/x-javascript"
const val MIME_TYPE_APPLICATION_MPC = "application/vnd.mpohun.certificate"
const val MIME_TYPE_APPLICATION_MSG = "application/vnd.ms-outlook"
const val MIME_TYPE_APPLICATION_PDF = "application/pdf"
const val MIME_TYPE_APPLICATION_VND_MS_POWERPOINT = "application/vnd.ms-powerpoint"
const val MIME_TYPE_APPLICATION_RAR = "application/x-rar-compressed"
const val MIME_TYPE_APPLICATION_RTF = "application/rtf"
const val MIME_TYPE_APPLICATION_TAR = "application/x-tar"
const val MIME_TYPE_APPLICATION_TGZ = "application/x-compressed"
const val MIME_TYPE_APPLICATION_WPS = "application/vnd.ms-works"
const val MIME_TYPE_APPLICATION_Z = "application/x-compress"
const val MIME_TYPE_APPLICATION_ZIP = "application/zip"
const val MIME_TYPE_APPLICATION_M3U8 = "application/x-mpegURL"

// text
const val MIME_TYPE_TEXT_PLAIN = "text/plain"
const val MIME_TYPE_TEXT_HTML = "text/html"
const val MIME_TYPE_TEXT_XML = "text/xml"

// image
const val MIME_TYPE_IMAGE_WILDCARD = "image/*"
const val MIME_TYPE_IMAGE_BMP = "image/bmp"
const val MIME_TYPE_IMAGE_JPEG = "image/jpeg"
const val MIME_TYPE_IMAGE_GIF = "image/gif"
const val MIME_TYPE_IMAGE_PNG = "image/png"
const val MIME_TYPE_IMAGE_ICO = "image/x-icon"
const val MIME_TYPE_IMAGE_PSD = "image/vnd.adobe.photoshop"
const val MIME_TYPE_IMAGE_WEBP = "image/webp"
const val MIME_TYPE_IMAGE_TIFF = "image/tiff"

// video
const val MIME_TYPE_VIDEO_WILDCARD = "video/*"
const val MIME_TYPE_VIDEO_3GP = "video/3gpp"
const val MIME_TYPE_VIDEO_ASF = "video/x-ms-asf"
const val MIME_TYPE_VIDEO_AVI = "video/x-msvideo"
const val MIME_TYPE_VIDEO_A4U = "video/vnd.mpegurl"
const val MIME_TYPE_VIDEO_M4V = "video/x-m4v"
const val MIME_TYPE_VIDEO_QUICKTIME = "video/quicktime"
const val MIME_TYPE_VIDEO_MP4 = "video/mp4"
const val MIME_TYPE_VIDEO_MPEG = "video/mpeg"
const val MIME_TYPE_VIDEO_MNG = "video/x-mng"
const val MIME_TYPE_VIDEO_MOVIE = "video/x-sgi-movie"
const val MIME_TYPE_VIDEO_PVX = "video/x-pv-pvx"
const val MIME_TYPE_VIDEO_RV = "video/vnd.rn-realvideo"
const val MIME_TYPE_VIDEO_WM = "video/x-ms-wm"
const val MIME_TYPE_VIDEO_WMX = "video/x-ms-wmx"
const val MIME_TYPE_VIDEO_WV = "video/wavelet"
const val MIME_TYPE_VIDEO_WVX = "video/x-ms-wvx"
const val MIME_TYPE_VIDEO_RMVB = "video/vnd.rn-realvideo"
const val MIME_TYPE_VIDEO_WMV = "video/x-ms-wmv"
const val MIME_TYPE_VIDEO_FLV = "video/x-flv"
const val MIME_TYPE_VIDEO_FLI = "video/x-fli"
const val MIME_TYPE_VIDEO_F4V = "video/x-f4v"
const val MIME_TYPE_VIDEO_TS = "video/MP2T"
const val MIME_TYPE_VIDEO_OGG = "video/ogg"

// audio
const val MIME_TYPE_AUDIO_WILDCARD = "audio/*"
const val MIME_TYPE_AUDIO_MP4A_LATM = "audio/mp4a-latm"
const val MIME_TYPE_AUDIO_M3U = "audio/x-mpegurl"
const val MIME_TYPE_AUDIO_X_MPEG = "audio/x-mpeg"
const val MIME_TYPE_AUDIO_MPEG = "audio/mpeg"
const val MIME_TYPE_AUDIO_WAV = "audio/x-wav"
const val MIME_TYPE_AUDIO_WMA = "audio/x-ms-wma"
const val MIME_TYPE_AUDIO_RMP = "audio/x-pn-realaudio-plugin"
const val MIME_TYPE_AUDIO_WAX = "audio/x-ms-wax"
const val MIME_TYPE_AUDIO_AIF = "audio/x-aiff"
const val MIME_TYPE_AUDIO_AAC = "audio/x-aac"
const val MIME_TYPE_AUDIO_AU = "audio/basic"
const val MIME_TYPE_AUDIO_ADP = "audio/adp"


val File.mimeType: String get() = absolutePath.mimeType
val String.isVideoMimeType: Boolean get() = mimeType.startsWith(MIME_TYPE_PREFIX_VIDEO, true)
val String.isAudioMimeType: Boolean get() = mimeType.startsWith(MIME_TYPE_PREFIX_AUDIO, true)
val String.isImageMimeType: Boolean get() = mimeType.startsWith(MIME_TYPE_PREFIX_IMAGE, true)
val String.mimeType: String
    get() = when (FileUtil.getFileSuffix(this.toLowerCase(Locale.ROOT))) {
        // application
        ".gz" -> MIME_TYPE_APPLICATION_GZ
        ".js" -> MIME_TYPE_APPLICATION_JS
        ".jar" -> MIME_TYPE_APPLICATION_JAR
        ".gtar" -> MIME_TYPE_APPLICATION_GTAR
        ".doc" -> MIME_TYPE_APPLICATION_DOC
        ".apk" -> MIME_TYPE_APPLICATION_APK
        ".bin", ".class", ".exe" -> MIME_TYPE_APPLICATION_OCTET_STREAM
        ".mpc" -> MIME_TYPE_APPLICATION_MPC
        ".msg" -> MIME_TYPE_APPLICATION_MSG
        ".pdf" -> MIME_TYPE_APPLICATION_PDF
        ".pps", ".ppt" -> MIME_TYPE_APPLICATION_VND_MS_POWERPOINT
        ".rar" -> MIME_TYPE_APPLICATION_RAR
        ".rtf" -> MIME_TYPE_APPLICATION_RTF
        ".tar" -> MIME_TYPE_APPLICATION_TAR
        ".tgz" -> MIME_TYPE_APPLICATION_TGZ
        ".wps" -> MIME_TYPE_APPLICATION_WPS
        ".z" -> MIME_TYPE_APPLICATION_Z
        ".zip" -> MIME_TYPE_APPLICATION_ZIP
        ".m3u8" -> MIME_TYPE_APPLICATION_M3U8
        // text
        ".c", ".conf", ".cpp", ".h", ".prop", ".rc", ".sh", ".txt", ".log" -> MIME_TYPE_TEXT_PLAIN
        ".htm", ".html" -> MIME_TYPE_TEXT_HTML
        ".xml" -> MIME_TYPE_TEXT_XML
        ".java" -> MIME_TYPE_TEXT_PLAIN
        // image
        ".jpeg", ".jpg" -> MIME_TYPE_IMAGE_JPEG
        ".gif" -> MIME_TYPE_IMAGE_GIF
        ".bmp" -> MIME_TYPE_IMAGE_BMP
        ".png" -> MIME_TYPE_IMAGE_PNG
        ".ico" -> MIME_TYPE_IMAGE_ICO
        ".psd" -> MIME_TYPE_IMAGE_PSD
        ".webp" -> MIME_TYPE_IMAGE_WEBP
        ".tif", ".tiff" -> MIME_TYPE_IMAGE_TIFF
        // video
        ".3gp" -> MIME_TYPE_VIDEO_3GP
        ".asf" -> MIME_TYPE_VIDEO_ASF
        ".avi" -> MIME_TYPE_VIDEO_AVI
        ".m3u" -> MIME_TYPE_AUDIO_M3U
        ".m4v" -> MIME_TYPE_VIDEO_M4V
        ".mov", ".qt" -> MIME_TYPE_VIDEO_QUICKTIME
        ".mp4", ".mpg4" -> MIME_TYPE_VIDEO_MP4
        ".mpe", ".mpeg", ".mpg" -> MIME_TYPE_VIDEO_MPEG
        ".mng" -> MIME_TYPE_VIDEO_MNG
        ".movie" -> MIME_TYPE_VIDEO_MOVIE
        ".pvx" -> MIME_TYPE_VIDEO_PVX
        ".rv" -> MIME_TYPE_VIDEO_RV
        ".wm" -> MIME_TYPE_VIDEO_WM
        ".wmx" -> MIME_TYPE_VIDEO_WMX
        ".wv" -> MIME_TYPE_VIDEO_WV
        ".wvx" -> MIME_TYPE_VIDEO_WVX
        ".rmvb" -> MIME_TYPE_VIDEO_RMVB
        ".wmv" -> MIME_TYPE_VIDEO_WMV
        ".flv" -> MIME_TYPE_VIDEO_FLV
        ".fli" -> MIME_TYPE_VIDEO_FLI
        ".f4v" -> MIME_TYPE_VIDEO_F4V
        ".ts" -> MIME_TYPE_VIDEO_TS
        ".ogg", ".ogv" -> MIME_TYPE_VIDEO_OGG
        ".rm", ".swf", ".ram", ".webm", ".viv", ".uvu", ".pyv", ".mxu", "fvt", ".uvv", ".uvs",
        ".uvp", ".uvm", ".uvh", ".mj2", ".jpm", ".jpgv", ".h264", ".h263", ".h261", ".3g2" -> MIME_TYPE_VIDEO_WILDCARD
        // audio
        ".m4a", ".m4b", ".m4p" -> MIME_TYPE_AUDIO_MP4A_LATM
        ".m4u" -> MIME_TYPE_VIDEO_A4U
        ".mp2", ".mp3" -> MIME_TYPE_AUDIO_X_MPEG
        ".wav" -> MIME_TYPE_AUDIO_WAV
        ".wma" -> MIME_TYPE_AUDIO_WMA
        ".mpga" -> MIME_TYPE_AUDIO_MPEG
        ".rmp" -> MIME_TYPE_AUDIO_RMP
        ".wax" -> MIME_TYPE_AUDIO_WAX
        ".aif" -> MIME_TYPE_AUDIO_AIF
        ".aac" -> MIME_TYPE_AUDIO_AAC
        ".au" -> MIME_TYPE_AUDIO_AU
        ".adp" -> MIME_TYPE_AUDIO_ADP
        ".flac", ".amr", ".mmf", ".cda", ".weba", ".rip", ".ecelp9600", ".ecelp7470", ".ecelp4800",
        ".pya", ".lvp", ".dtshd", ".dts", ".dra", ".eol", ".uva", ".mid" -> MIME_TYPE_AUDIO_WILDCARD
        //
        else -> MIME_TYPE_WILDCARD
    }

/**
 * 获取URI永久权限modeFlags（如果没有获取该权限，则设备重启后Uri将自动失效）
 */
fun Uri.takePersistableUriPermission(dataIntent: Intent) =
    takePersistableUriPermission(dataIntent.flags)

/**
 * 获取URI永久权限（如果没有获取该权限，则设备重启后Uri将自动失效）
 */
fun Uri.takePersistableUriPermission(modeFlags: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        APP.INSTANCE.contentResolver.takePersistableUriPermission(
            this,
            modeFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        )
    }
}

fun getRandomPhotoCacheFilePath(suffixName: String = ".jpg"): String =
    getRandomCacheFilePath(suffixName)

fun getRandomVideoCacheFilePath(suffixName: String = ".mp4"): String =
    getRandomCacheFilePath(suffixName)

fun getRandomAudioCacheFilePath(suffixName: String = ".mp3"): String =
    getRandomCacheFilePath(suffixName)

fun getRandomCacheFilePath(suffixName: String): String =
    APP.INSTANCE.cacheDir?.absoluteFile.toString() + "/" + getCurrentTimeMillisFileName(suffixName)

fun getCurrentTimeMillisFileName(suffixName: String): String =
    System.currentTimeMillis().toString() + suffixName

fun getCurrentDateTimeFileName(
    suffixName: String,
    dateTimeFormat: String = "yyyyMMddHHmmss"
): String =
    SimpleDateFormat(dateTimeFormat).format(Date(System.currentTimeMillis())) + suffixName
