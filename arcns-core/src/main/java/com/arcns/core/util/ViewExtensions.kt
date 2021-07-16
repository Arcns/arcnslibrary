package com.arcns.core.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.Base64
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.DialogBehavior
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.Companion.DEFAULT_BEHAVIOR
import com.arcns.core.APP
import com.arcns.core.R
import com.arcns.core.file.getRandomPhotoCacheFilePath
import com.arcns.core.file.mimeType
import com.arcns.core.file.tryClose
import com.arcns.xfile.FileUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import ezy.assist.compat.SettingsCompat
import me.shouheng.compress.Compress
import me.shouheng.compress.strategy.Strategies
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt


open class WidthHeight(
    var width: Int,
    var height: Int,
    var extra: Any? = null
)

open class ScaledWidthHeight(
    width: Int,
    height: Int,
    var newWidth: Int,
    var newHeight: Int
) : WidthHeight(width, height)

/***********************************公共**************************************/

// 是否为主线程
val isMainThread: Boolean get() = Looper.getMainLooper() == Looper.myLooper()

// 快速设置value，自动做线程判断
var <T> MutableLiveData<T>.fastValue: T?
    get() = value
    set(value) {
        if (isMainThread) this.value = value
        else postValue(value)
    }

// 数组转列表
inline fun <reified T> Collection<T>.toArrayList(): ArrayList<T> {
    return ArrayList<T>().apply {
        addAll(this@toArrayList)
    }
}

// 获取屏幕宽高
val screenWidthHeight: WidthHeight
    get() {
        val windowManager =
            APP.INSTANCE.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return WidthHeight(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            extra = displayMetrics
        )
    }

// 获取屏幕宽度
val screenWidth: Int get() = screenWidthHeight.width

// 获取屏幕高度
val screenHeight: Int get() = screenWidthHeight.height


// 获取屏幕实际可用宽高（包含status bar）
val screenAvailableWidthHeight: WidthHeight
    get() {
        val windowManager =
            APP.INSTANCE.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = screenHeight
        val screenAvailableHeight =
            if (screenHeight == displayMetrics.heightPixels || screenHeight == displayMetrics.heightPixels + APP.INSTANCE.getNavigationBarHeight())
                displayMetrics.heightPixels
            else displayMetrics.heightPixels + APP.INSTANCE.getStatusBarHeight()
        return WidthHeight(
            displayMetrics.widthPixels,
            screenAvailableHeight,
            extra = displayMetrics
        )
    }

// 获取屏幕实际可用宽度（包含status bar）
val screenAvailableWidth: Int get() = screenAvailableWidthHeight.width

// 获取屏幕实际可用高度（包含status bar）
val screenAvailableHeight: Int get() = screenAvailableWidthHeight.height


/***********************************格式转换**************************************/

// string格式转datetime
fun String.asDateTime(format: String?): Date? = try {
    SimpleDateFormat(format).parse(this)
} catch (e: java.lang.Exception) {
    null
}

// 获取string资源
val Int.string: String get() = APP.CONTEXT.getString(this)

// 获取string资源
//fun Int.string(value: Any?): String = APP.CONTEXT.getString(this, value)
//fun Int.string(value: Any?, value2: Any?): String = APP.CONTEXT.getString(this, value, value2)
fun Int.string(vararg values: Any?): String = APP.CONTEXT.getString(this, *values)

// 获取drawable资源
val Int.drawable: Drawable
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return APP.INSTANCE.resources.getDrawable(this, APP.INSTANCE.theme)
        } else {
            return APP.INSTANCE.resources.getDrawable(this)
        }
    }

val Any.drawable: Drawable?
    get() = when (this) {
        is Int -> this.drawable
        is Drawable -> this
        else -> null
    }

// 获取color资源
val Int.color: Int
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return APP.INSTANCE.resources.getColor(this, APP.INSTANCE.theme)
        } else {
            return APP.INSTANCE.resources.getColor(this)
        }
    }

// color string 转 color
val String.color: Int get() = Color.parseColor(if (this.startsWith("#")) this else "#$this")


// 普通数值转dp
val Float.dp: Float                 // [xxhdpi](360 -> 1080)
    get() = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
    )

// 普通数值转dp
val Int.dp: Int
    get() = toFloat().dp.toInt()

// 普通数值转sp
val Float.sp: Float                 // [xxhdpi](360 -> 1080)
    get() = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics
    )

// 普通数值转sp
val Int.sp: Int
    get() = toFloat().sp.toInt()

// 将dimen资源rid转换为其实际数值
val Int.dimen: Float
    get() = dimenOrNull ?: 0f

// 将dimen资源rid转换为其实际数值
val Int.dimenOrNull: Float?
    get() = try {
        APP.INSTANCE.resources.getDimension(this)
    } catch (e: java.lang.Exception) {
        null
    }

// string转换为html
val String.html: Spanned
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, 0)
    } else {
        Html.fromHtml(this)
    }

fun String.colorHtmlToString(colorRes: Int): String =
    "<font color='${String.format("#%06X", 0xFFFFFF and colorRes.color)}'>$this</font>"

fun String.colorHtml(color: Int): Spanned = colorHtmlToString(color).html

// string外层包裹a标签
fun String.hrefHtmlToString(href: String): String =
    "<a href='${href}'>$this</a>"

fun String.hrefHtml(href: String): Spanned = hrefHtmlToString(href).html

/***********************************Gson**************************************/
// string序列化为对象
inline fun <reified T> Gson.fromJson(json: String) = fromJson(json, T::class.java)
inline fun <reified T> Gson.tryFromJson(json: String?): T? = try {
    if (json.isNullOrBlank()) null
    else fromJson(json, T::class.java)
} catch (e: java.lang.Exception) {
    null
}


//添加序列化排除策略和反序列化排除策略 匹配Gson的@Expose注解，同时实现未配置@Expose的属性全部自动匹配
fun GsonBuilder.addExclusionStrategy(): GsonBuilder {
    return addSerializationExclusionStrategy().addDeserializationExclusionStrategy()
}

//添加序列化排除策略 匹配Gson的@Expose注解，同时实现未配置@Expose的属性全部自动匹配
fun GsonBuilder.addSerializationExclusionStrategy(): GsonBuilder {
    return addSerializationExclusionStrategy(
        object : ExclusionStrategy {
            override fun shouldSkipClass(clazz: Class<*>?): Boolean = false

            override fun shouldSkipField(f: FieldAttributes?): Boolean {
                val expose = f?.getAnnotation(Expose::class.java)
                return expose != null && !expose.serialize
            }

        })
}

//添加反序列化排除策略 匹配Gson的@Expose注解，同时实现未配置@Expose的属性全部自动匹配
fun GsonBuilder.addDeserializationExclusionStrategy(): GsonBuilder {
    return addDeserializationExclusionStrategy(
        object : ExclusionStrategy {
            override fun shouldSkipClass(clazz: Class<*>?): Boolean = false

            override fun shouldSkipField(f: FieldAttributes?): Boolean {
                val expose =
                    f?.getAnnotation(Expose::class.java)
                // 排除字段返回true
                return expose != null && !expose.deserialize
            }

        })
}

/***********************************键盘显示隐藏**************************************/

// 隐藏键盘
fun Fragment.hideSoftInputFromWindow() = activity?.hideSoftInputFromWindow()

// 隐藏键盘
fun Activity.hideSoftInputFromWindow() {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
        currentFocus?.windowToken,
        InputMethodManager.HIDE_NOT_ALWAYS
    )
}

// 显示键盘
fun EditText.showSoftInput() = this.run {
    requestFocus()
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)?.showSoftInput(
        this,
        0
    );
}

/***********************************系统状态栏操作**************************************/

/**
 * 设置透明状态栏
 */
fun Activity.setupTransparentStatusBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.statusBarColor = Color.TRANSPARENT
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
}

// 获取系统状态栏高度
fun Context.getStatusBarHeight(): Int {
    var result = 0;
    var resId = resources.getIdentifier("status_bar_height", "dimen", "android");
    if (resId > 0) {
        result = resources.getDimensionPixelSize(resId);
    }
    return result
}

// 获取系统导航栏高度
fun Context.getNavigationBarHeight(): Int {
    var result = 0;
    var resId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
    if (resId > 0) {
        result = resources.getDimensionPixelSize(resId);
    }
    return result
}

/**
 * 获取ActionBar默认高度
 */
fun Context?.getActionBarHeight(): Int {
    if (this == null) {
        return 48.dp
    }
    val typedValue = TypedValue()
    if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
        return TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    }
    return 48.dp
}

/**
 * 上内边距增加系统状态栏的高度大小
 */
fun View.setPaddingStatusBarHeight(
    notRepeat: Boolean = false, // 判断是否重复添加，若现有padding高度为状态栏高度，则不再添加
    autoHeightExpansion: Boolean = true // 是否自动扩展高度，增加状态栏高度（仅当非自适应时生效）
) {
    val statusBarHeight = context.getStatusBarHeight()
    if (notRepeat && paddingTop == statusBarHeight) {
        // 已添加状态栏高度时不再重复添加
        return
    }
    if (autoHeightExpansion && layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
        // 如果高度不是自适应时，需要增加状态栏的高度
        layoutParams = layoutParams.apply {
            height += statusBarHeight
        }
    }
    setPadding(
        paddingLeft,
        paddingTop + statusBarHeight,
        paddingRight,
        paddingBottom
    )
}

/**
 * 上外边距增加系统状态栏的高度大小
 */
fun View.setMarginStatusBarHeight() {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
            setMargins(
                leftMargin,
                topMargin + context.getStatusBarHeight(),
                rightMargin,
                bottomMargin
            )
        }
    }
}

/**
 * 设置状态栏文字高亮
 */

fun Fragment.setLightSystemStatusBarText() = activity?.setLightSystemStatusBarText()

/**
 * 设置状态栏文字高亮
 */
fun Activity.setLightSystemStatusBarText() {
    window?.decorView?.systemUiVisibility = window?.decorView?.systemUiVisibility?.let {
        it or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    } ?: View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
}

/**
 * 清除状态栏文字高亮
 */
fun Fragment.clearLightSystemStatusBarText() = activity?.clearLightSystemStatusBarText()

/**
 * 清除状态栏文字高亮
 */
fun Activity.clearLightSystemStatusBarText() {
    window?.decorView?.systemUiVisibility?.run {
        window?.decorView?.systemUiVisibility =
            this and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() // 与非运算，同java的&~
    }
}

/***********************************弹出框操作**************************************/
/**
 * 弹出框
 */
fun Fragment.showDialog(
    behavior: DialogBehavior = DEFAULT_BEHAVIOR,
    func: MaterialDialog.() -> Unit
): MaterialDialog? =
    this.activity?.showDialog(behavior, func)

/**
 * 弹出框
 */
fun Activity.showDialog(
    behavior: DialogBehavior = DEFAULT_BEHAVIOR,
    func: MaterialDialog.() -> Unit
): MaterialDialog =
    (this as Context).showDialog(behavior, func)

/**
 * 弹出框
 */
fun View.showDialog(
    behavior: DialogBehavior = DEFAULT_BEHAVIOR,
    func: MaterialDialog.() -> Unit
): MaterialDialog = context.showDialog(behavior, func)

/**
 * 弹出框
 */
fun Context.showDialog(
    behavior: DialogBehavior = DEFAULT_BEHAVIOR,
    func: MaterialDialog.() -> Unit
): MaterialDialog {
    return MaterialDialog(this, behavior).show {
        //        icon(R.mipmap.ic_launcher_round)
        func()
    };
}

/***********************************LOG**************************************/

/**
 * Debug模式，开启时输出LOG
 */
const val MODE_DEBUG = true

/**
 * 打印LOG
 */
fun LOG(message: String) {
    if (MODE_DEBUG)
        android.util.Log.e("ARCNS_MODE_DEBUG", message)
}

fun String.log() {
    if (MODE_DEBUG)
        android.util.Log.e("ARCNS_MODE_DEBUG", this)
}


/***********************************Uri操作**************************************/

fun File.conversionUri(authority: String = APP.fileProviderAuthority!!): Uri? = try {
    if (!exists()) null
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(APP.INSTANCE, authority, this);
    } else {
        Uri.fromFile(this);
    }
} catch (e: Exception) {
    e.printStackTrace()
    null
}


/***********************************图片显示、保存与压缩**************************************/

/**
 * 获取bitmap大小
 */
fun String.getBitmapSize(): WidthHeight? = File(this).getBitmapSize()


/**
 * 获取bitmap大小
 */
fun File.getBitmapSize(): WidthHeight? {
    if (!exists()) return null
    return FileInputStream(this).getBitmapSize()
}

/**
 * 获取bitmap大小
 */
fun Uri.getBitmapSize(): WidthHeight? {
    return APP.INSTANCE.contentResolver.openInputStream(this)?.getBitmapSize()
}

/**
 * 获取bitmap大小
 */
fun Int.getBitmapSize(): WidthHeight? = try {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeResource(APP.INSTANCE.resources, this, options)
    WidthHeight(
        width = options.outWidth,
        height = options.outHeight
    )
} catch (e: java.lang.Exception) {
    null
}

/**
 * 获取bitmap大小
 */
fun InputStream.getBitmapSize(): WidthHeight? = try {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(this, null, options)
    WidthHeight(
        width = options.outWidth,
        height = options.outHeight
    )
} catch (e: java.lang.Exception) {
    null
} finally {
    tryClose()
}

/**
 * 计算缩放后的bitmap大小
 */
fun String.calculateBitmapScaledSize(width: Int? = null, height: Int? = null): ScaledWidthHeight? =
    getBitmapSize()?.calculateBitmapScaledSize(width, height)

/**
 * 计算缩放后的bitmap大小
 */
fun File.calculateBitmapScaledSize(
    width: Int? = null,
    height: Int? = null
): ScaledWidthHeight? = getBitmapSize()?.calculateBitmapScaledSize(width, height)

/**
 * 计算缩放后的bitmap大小
 */
fun Uri.calculateBitmapScaledSize(
    width: Int? = null,
    height: Int? = null
): ScaledWidthHeight? = getBitmapSize()?.calculateBitmapScaledSize(width, height)

/**
 * 计算缩放后的bitmap大小
 */
fun Int.calculateBitmapScaledSize(
    width: Int? = null,
    height: Int? = null
): ScaledWidthHeight? = getBitmapSize()?.calculateBitmapScaledSize(width, height)

/**
 * 计算缩放后的bitmap大小
 */
fun InputStream.calculateBitmapScaledSize(
    width: Int? = null,
    height: Int? = null
): ScaledWidthHeight? = getBitmapSize()?.calculateBitmapScaledSize(width, height)

/**
 * 计算缩放后的bitmap大小
 */
fun WidthHeight.calculateBitmapScaledSize(
    width: Int? = null,
    height: Int? = null
): ScaledWidthHeight? {
    val size = this
    var newWidth = if (width == 0) null else width
    var newHeight = if (height == 0) null else height
    if (newWidth == null && newHeight == null) {
        return ScaledWidthHeight(
            width = size.width,
            height = size.height,
            newWidth = size.width,
            newHeight = size.height
        )
    }
    if (newWidth == null) {
        var scale = newHeight!!.toDouble() / size.height
        newWidth = (size.width * scale).toInt()
    }
    if (newHeight == null) {
        var scale = newWidth!!.toDouble() / size.width
        newHeight = (size.height * scale).toInt()
    }
    return ScaledWidthHeight(
        width = size.width,
        height = size.height,
        newWidth = newWidth,
        newHeight = newHeight
    )
}

// 把文件路径转换为bitmap，并设置大小
fun String.bitmap(width: Int? = null, height: Int? = null): Bitmap? =
    File(this).bitmap(width, height)

// 把文件转换为bitmap，并设置大小
fun File.bitmap(width: Int? = null, height: Int? = null): Bitmap? =
    if (!exists()) null
    else FileInputStream(this).bitmap(
        if (width == null && height == null) null
        else calculateBitmapScaledSize(width, height)
    )

// 把Uri文件转换为bitmap，并设置大小
fun Uri.bitmap(width: Int? = null, height: Int? = null): Bitmap? =
    APP.INSTANCE.contentResolver.openInputStream(this)?.bitmap(
        if (width == null && height == null) null
        else calculateBitmapScaledSize(width, height)
    )

/**
 * 把文件流转换为bitmap，并设置大小
 */
fun InputStream.bitmap(size: ScaledWidthHeight? = null): Bitmap? {
    try {
        if (size == null) return BitmapFactory.decodeStream(this)
        // 计算图片缩放比例
        val minSideLength = size.newWidth.coerceAtMost(size.newHeight)
        val options = BitmapFactory.Options()
        options.inSampleSize = computeBitmapSampleSize(
            options, minSideLength,
            size.newWidth * size.newHeight
        )
        options.inInputShareable = true;
        options.inPurgeable = true;
        return BitmapFactory.decodeStream(this, null, options)
    } catch (e: java.lang.Exception) {
        return null
    } finally {
        tryClose()
    }
}

/**
 * 把资源转换为bitmap，并设置大小
 */
fun Int.bitmap(width: Int? = null, height: Int? = null): Bitmap? {
    try {
        if (width == null && height == null) return BitmapFactory.decodeResource(
            APP.INSTANCE.resources,
            this
        )
        // 计算缩放后的bitmap大小
        val size = calculateBitmapScaledSize(width, height) ?: return null
        // 计算图片缩放比例
        val minSideLength = size.newWidth.coerceAtMost(size.newHeight)
        val options = BitmapFactory.Options()
        options.inSampleSize = computeBitmapSampleSize(
            options, minSideLength,
            size.newWidth * size.newHeight
        )
        options.inInputShareable = true;
        options.inPurgeable = true;
        return BitmapFactory.decodeResource(APP.INSTANCE.resources, this, options)
    } catch (e: java.lang.Exception) {
        return null
    }
}

private fun computeBitmapSampleSize(
    options: BitmapFactory.Options?,
    minSideLength: Int, maxNumOfPixels: Int
): Int {
    val initialSize = computeBitmapInitialSampleSize(
        options!!, minSideLength,
        maxNumOfPixels
    )
    var roundedSize: Int
    if (initialSize <= 8) {
        roundedSize = 1
        while (roundedSize < initialSize) {
            roundedSize = roundedSize shl 1
        }
    } else {
        roundedSize = (initialSize + 7) / 8 * 8
    }
    return roundedSize
}

private fun computeBitmapInitialSampleSize(
    options: BitmapFactory.Options,
    minSideLength: Int, maxNumOfPixels: Int
): Int {
    val w = options.outWidth.toDouble()
    val h = options.outHeight.toDouble()
    val lowerBound = if (maxNumOfPixels == -1) 1 else ceil(
        sqrt(w * h / maxNumOfPixels)
    ).toInt()
    val upperBound =
        if (minSideLength == -1) 128 else floor(w / minSideLength).coerceAtMost(floor(h / minSideLength))
            .toInt()
    if (upperBound < lowerBound) {
        // return the larger one when there is no overlapping zone.
        return lowerBound
    }
    return if (maxNumOfPixels == -1 && minSideLength == -1) {
        1
    } else if (minSideLength == -1) {
        lowerBound
    } else {
        upperBound
    }
}


// 把图片资源转换为bitmap，并设置大小
fun Int.bitmap(context: Context?, newWidth: Int? = null, newHeight: Int? = null): Bitmap {
    return BitmapFactory.decodeResource(context?.resources, this).zoomImg(newWidth, newHeight)
}

// 设置bitmap大小
fun Bitmap.zoomImg(newWidth: Int? = null, newHeight: Int? = null): Bitmap { //获得图片的宽高
    if (newWidth == null && newHeight == null) {
        return this;
    }
    var scaleWidth: Float = 1f;
    var scaleHeight: Float = 1f;
    newHeight?.apply {
        scaleHeight = newHeight.toFloat() / height
        if (newWidth == null) {
            scaleWidth = scaleHeight
        }
    }
    newWidth?.apply {
        scaleWidth = newWidth.toFloat() / width
        if (newHeight == null) {
            scaleHeight = scaleWidth
        }
    }
    //取得想要缩放的matrix参数
    val matrix = Matrix()
    matrix.postScale(scaleWidth, scaleHeight)
    //得到新的图片
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/**
 * bitmap转base64
 */
fun Bitmap.asBase64(): String? {
    var result: String? = null
    var byteArrayOutputStream: ByteArrayOutputStream? = null
    try {
        byteArrayOutputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        byteArrayOutputStream.flush()
        byteArrayOutputStream.close()
        result = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            byteArrayOutputStream?.flush()
            byteArrayOutputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return result
}

/**
 * 文件路径转base64
 */
fun String.asBase64(): String? {
    var file = File(this)
    if (!file.exists()) {
        return null
    }
    return file.asBase64()
}

/**
 * 文件转base64
 */
fun File.asBase64(): String? {
    var result: String? = null
    if (!exists()) {
        return null
    }
    var inputStream: FileInputStream? = null
    try {
        inputStream = FileInputStream(this)
        var bytes = ByteArray(inputStream.available())
        var length = inputStream.read(bytes)
        result = Base64.encodeToString(bytes, 0, length, Base64.DEFAULT)
    } catch (e: java.lang.Exception) {
        e.printStackTrace();
    } finally {
        try {
            inputStream?.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace();
        }
    }
    return result
}

/**
 * 通过Picasso设置图片
 */
fun ImageView.setImageViaPicasso(
    image: Any?,
    placeholderDrawable: Drawable? = null,
    errorDrawable: Drawable? = null,
    size: Float? = null,
    w: Float? = null,
    h: Float? = null,
    centerInside: Boolean? = null, //默认false
    cache: Boolean? = null, // 默认true
    noFade: Boolean? = null, //默认为false
    asBackground: Boolean? = null, //默认为false
    highQualityBitmap: Boolean? = null //高质量bitmap，默认为false
) {

    if (image == null) {
        if (asBackground == true) {
            this.background = null
        } else {
            this.setImageDrawable(null)
        }
        return
    }
    val picasso = Picasso.get().apply {
        isLoggingEnabled = true
    }
    var requestCreator = when (image) {
        is Int -> picasso.load(image)
        is String -> {
            if (image.isInternetResources)
                picasso.load(image)
            else
                picasso.load(File(image))
        }
        is Uri -> picasso.load(image)
        is File -> picasso.load(image)
        else -> return
    }
    // 设置缓存机制
    if (cache == false) {
        requestCreator.memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
    }
    // 设置图片质量
    requestCreator.config(if (highQualityBitmap == true) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565)
    // 设置加载中的图片
    placeholderDrawable?.let {
        requestCreator.placeholder(it)
    }
    // 设置加载错误的图片
    errorDrawable?.let {
        requestCreator.error(it)
    }
    // 设置图片大小
    val width = w ?: size ?: 0f
    val height = h ?: size ?: 0f
    if (width != 0f || height != 0f) {
        requestCreator.resize(width.toInt(), height.toInt())
        requestCreator.onlyScaleDown()
        if (centerInside == true) {
            // 自适应全部显示
            requestCreator.centerInside()
        } else {
            // 自适应填充满
            requestCreator.centerCrop()
        }
    }
    // 是否显示动画
    if (noFade == true) {
        requestCreator.noFade()
    }
    if (asBackground == true) {
        // 设置为背景
        requestCreator.into(object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }

            override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                background = BitmapDrawable(context.resources, bitmap)
            }

        })
    } else {
        // 设置为src
        requestCreator.into(this)
    }
}

/**
 * 通过Glide设置图片
 */
fun ImageView.setImageViaGlide(
    image: Any?,
    placeholderDrawable: Drawable? = null,
    errorDrawable: Drawable? = null,
    size: Float? = null,
    w: Float? = null,
    h: Float? = null,
    centerInside: Boolean? = null, //默认false
    cache: Boolean? = null, // 默认true
    noFade: Boolean? = null, //默认为false
    asBackground: Boolean? = null, //默认为false
    highQualityBitmap: Boolean? = null, //高质量bitmap，默认为false
    asGif: Boolean? = null //是否为gif，默认为false
) {
    if (image == null) {
        if (asBackground == true) {
            this.background = null
        } else {
            this.setImageDrawable(null)
        }
        return
    }
    val glide =
        Glide.with(this)
    var requestBuilder: RequestBuilder<*> = when (image) {
        is Int -> if (asGif == true) glide.asGif().load(image) else glide.load(image)
        is String -> {
            val checkAsGif = asGif ?: image.endsWith(".gif", true)
            if (image.isInternetResources) {
                if (checkAsGif) glide.asGif().load(image) else glide.load(image)
            } else {
                if (checkAsGif) glide.asGif().load(File(image)) else glide.load(File(image))
            }
        }
        is Uri -> if (asGif == true) glide.asGif().load(image) else glide.load(image)
        is File -> {
            val checkAsGif = asGif ?: image.absolutePath.endsWith(".gif", true)
            if (checkAsGif == true) glide.asGif().load(image) else glide.load(image)
        }
        is GlideUrl -> if (asGif == true) glide.asGif().load(image) else glide.load(image)
        else -> return
    }
//    requestBuilder.priority(Priority.LOW)
//    requestBuilder =requestBuilder.thumbnail(0.1f)
    // 设置缓存机制
    if (cache == false) {
        requestBuilder =
            requestBuilder.diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
    }
    // 设置图片质量
    requestBuilder =
        requestBuilder.format(if (highQualityBitmap == true) DecodeFormat.PREFER_ARGB_8888 else DecodeFormat.PREFER_RGB_565)
    // 设置加载中的图片
    placeholderDrawable?.let {
        requestBuilder = requestBuilder.placeholder(it)
    }
    // 设置加载错误的图片
    errorDrawable?.let {
        requestBuilder = requestBuilder.error(it)
    }
    // 设置图片大小
    val width = w ?: size ?: 0f
    val height = h ?: size ?: 0f
    if (width != 0f || height != 0f) {
        requestBuilder = requestBuilder.override(width.toInt(), height.toInt())
        requestBuilder = if (centerInside == true) {
            // 自适应全部显示
            requestBuilder.centerInside()
        } else {
            // 自适应填充满
            requestBuilder.centerCrop()
        }
    }
    // 是否显示动画
    if (noFade == true) {
        requestBuilder = requestBuilder.dontAnimate()
    }
    if (asBackground == true) {
        // 设置为背景
        if (asGif == true)
            (requestBuilder as? RequestBuilder<GifDrawable>)?.into(object :
                CustomTarget<GifDrawable>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    transition: Transition<in GifDrawable>?
                ) {
                    this@setImageViaGlide.background = resource
                }

            })
        else
            (requestBuilder as? RequestBuilder<Drawable>)?.into(object : CustomTarget<Drawable>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    this@setImageViaGlide.background = resource
                }
            })
    } else {
        requestBuilder.into(this)
    }
}


/**
 * 通过Glide设置图片
 */
fun Any.loadDrawable(
    image: Any?, // 图片资源（Int、String、Uri、File、GlideUrl）
    cache: Boolean? = null, // 默认true
    highQualityBitmap: Boolean? = null, //高质量bitmap，默认为false
    asGif: Boolean? = null, //是否为gif，默认为false
    onFailed: ((Exception?) -> Unit)? = null,
    onSuccess: (Drawable) -> Unit
) {

    val glide = when (this) {
        is View -> Glide.with(this)
        is Context -> Glide.with(this)
        else -> {
            onFailed?.invoke(java.lang.Exception("Need to be accessed through view or context"))
            return
        }
    }
    var requestBuilder: RequestBuilder<*> = when (image) {
        is Int -> if (asGif == true) glide.asGif().load(image) else glide.load(image)
        is String -> {
            val checkAsGif = asGif ?: image.endsWith(".gif", true)
            if (image.isInternetResources) {
                if (checkAsGif) glide.asGif().load(image) else glide.load(image)
            } else {
                if (checkAsGif) glide.asGif().load(File(image)) else glide.load(File(image))
            }
        }
        is Uri -> if (asGif == true) glide.asGif().load(image) else glide.load(image)
        is File -> {
            val checkAsGif = asGif ?: image.absolutePath.endsWith(".gif", true)
            if (checkAsGif == true) glide.asGif().load(image) else glide.load(image)
        }
        is GlideUrl -> if (asGif == true) glide.asGif().load(image) else glide.load(image)
        else -> return
    }
    // 设置缓存机制
    if (cache == false) {
        requestBuilder =
            requestBuilder.diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
    }
    // 设置图片质量
    requestBuilder =
        requestBuilder.format(if (highQualityBitmap == true) DecodeFormat.PREFER_ARGB_8888 else DecodeFormat.PREFER_RGB_565)
    if (asGif == true)
        (requestBuilder as? RequestBuilder<GifDrawable>)?.into(object :
            CustomTarget<GifDrawable>() {
            override fun onLoadCleared(placeholder: Drawable?) {
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                onFailed?.invoke(null)
            }

            override fun onResourceReady(
                resource: GifDrawable,
                transition: Transition<in GifDrawable>?
            ) {
                onSuccess.invoke(resource)
            }

        })
    else
        (requestBuilder as? RequestBuilder<Drawable>)?.into(object : CustomTarget<Drawable>() {
            override fun onLoadCleared(placeholder: Drawable?) {
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                onFailed?.invoke(null)
            }

            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                onSuccess.invoke(resource)
            }
        })
}

fun Int.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80,
    isOriginal: Boolean = false,
    saveFilePath: String? = null
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality,
    isOriginal,
    saveFilePath
)

fun String.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80,
    isOriginal: Boolean = false,
    saveFilePath: String? = null
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality,
    isOriginal,
    saveFilePath
)

fun File.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80,
    isOriginal: Boolean = false,
    saveFilePath: String? = null
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality,
    isOriginal,
    saveFilePath
)

fun Uri.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80,
    isOriginal: Boolean = false,
    saveFilePath: String? = null
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality,
    isOriginal,
    saveFilePath
)

fun Bitmap.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80,
    isOriginal: Boolean = false,
    saveFilePath: String? = null
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality,
    isOriginal,
    saveFilePath
)

fun saveImageAsLocalOrNull(
    image: Any?,
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80,
    isOriginal: Boolean = false,
    saveFilePath: String? = null
): File? = try {
    saveImageAsLocal(
        image,
        width,
        height,
        centerInside,
        highQualityBitmap,
        compressQuality,
        isOriginal,
        saveFilePath
    )
} catch (e: java.lang.Exception) {
    null
}

fun saveImageAsLocal(
    image: Any?,
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80,
    isOriginal: Boolean = false,
    saveFilePath: String? = null
): File {
    if (image == null) {
        throw java.lang.Exception("iamge is null")
    }
    val filePath = if (saveFilePath.isNullOrBlank()) getRandomPhotoCacheFilePath() else saveFilePath
    if (isOriginal || (compressQuality == 100 && highQualityBitmap)) {
        val bitmap = when (image) {
            is Bitmap -> image
            is Int -> image.bitmap()
            is String -> image.bitmap()
            is Uri -> image.bitmap()
            is File -> image.bitmap()
            else -> null
        } ?: throw java.lang.Exception("iamge as to bitmap error")
        val localFile = File(filePath)
        val localFileOutputStream = FileOutputStream(localFile)
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, localFileOutputStream)
            return localFile
        } catch (e: java.lang.Exception) {
            throw e
        } finally {
            localFileOutputStream.tryClose()
        }
    }
    try {
        val newWidth: Int
        val newHeight: Int
        val bitmap: Bitmap? =
            if (image is Bitmap) {
                newWidth = image.width
                newHeight = image.height
                image
            } else {
                var requestBuilder =
                    Glide.with(APP.INSTANCE).asBitmap()
                when (image) {
                    is Int -> requestBuilder.load(image)
                    is String -> if (image.isInternetResources) requestBuilder.load(image) else requestBuilder.load(
                        File(image)
                    )
                    is Uri -> requestBuilder.load(image)
                    is File -> requestBuilder.load(image)
                    else -> throw java.lang.Exception("iamge type error")
                }
                if (width != 0f && height != 0f) {
                    newWidth = width.toInt()
                    newHeight = height.toInt()
                } else {
                    val size = when (image) {
                        is Int -> image.calculateBitmapScaledSize(width.toInt(), height.toInt())
                        is String -> image.calculateBitmapScaledSize(width.toInt(), height.toInt())
                        is Uri -> image.calculateBitmapScaledSize(width.toInt(), height.toInt())
                        is File -> image.calculateBitmapScaledSize(width.toInt(), height.toInt())
                        else -> null
                    } ?: throw java.lang.Exception("iamge calculate scaled size error")
                    newWidth = size.newWidth
                    newHeight = size.newHeight
                }
                requestBuilder = requestBuilder.override(newWidth, newHeight)
                requestBuilder = if (centerInside) {
                    requestBuilder.centerInside()
                } else {
                    requestBuilder.centerCrop()
                }
                requestBuilder =
                    requestBuilder.format(if (highQualityBitmap) DecodeFormat.PREFER_ARGB_8888 else DecodeFormat.PREFER_RGB_565)
                // 让Glide在当前线程同步加载
                requestBuilder.submit().get()
            }
        // 开始压缩
        return Compress.with(APP.INSTANCE, bitmap)
            .setQuality(compressQuality)
            .setTargetDir(FileUtil.getFileDirectory(filePath))
            .setCacheNameFactory {
                FileUtil.getFileName(filePath)
            }
            .strategy(Strategies.compressor())
            .setMaxWidth(newWidth.toFloat())
            .setMaxHeight(newHeight.toFloat())
            .get()
    } catch (e: java.lang.Exception) {
        throw e
    }
}

// 旋转bitmap
fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

// yuv420sp视频帧转bitmap
fun ByteArray.yuv420spToBitmap(width: Int, height: Int): Bitmap {
    val image = YuvImage(this, ImageFormat.NV21, width, height, null)
    val stream = ByteArrayOutputStream()
    image.compressToJpeg(Rect(0, 0, width, height), 100, stream)
    val bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
    stream.tryClose()
    return bitmap
}

/***********************************打开app**************************************/
fun Context.openAppByPath(
    path: String,
    mimeType: String = path.mimeType,
    authority: String? = null
) {
    if (path.isInternetResources) {
        openAppByUri(Uri.parse(path), mimeType)
    } else if (authority != null) {
        openAppByFile(File(path), mimeType, authority)
    }
}

fun Context.openAppByUri(uri: Uri, mimeType: String) {
    var intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, mimeType)
    if (intent.resolveActivity(packageManager) == null) {
        Toast.makeText(
            this,
            R.string.text_not_find_open_app_by_mime_type.string,
            Toast.LENGTH_LONG
        )
            .show()
        return
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.openAppByFile(
    file: File,
    mimeType: String = file.absolutePath.mimeType,
    authority: String
) {
    if (!file.exists()) {
        return
    }
    var intent = Intent(Intent.ACTION_VIEW)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // android N以上必须获取Uri权限，否则无法正常使用
        var uri = FileProvider.getUriForFile(this, authority, file)
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } else {
        // anddroid N以下可以直接设置Uri
        var uri = file.toUri()
        intent.setDataAndType(uri, mimeType)
    }
    if (intent.resolveActivity(packageManager) == null) {
        Toast.makeText(
            this,
            R.string.text_not_find_open_app_by_mime_type.string,
            Toast.LENGTH_LONG
        )
            .show()
        return
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

/***********************************app操作**************************************/

// 重启APP
fun Activity.restartApp(isFinish: Boolean = true) {
    val packageManager: PackageManager = getPackageManager()
    val intent = packageManager.getLaunchIntentForPackage(getPackageName())
    val mainIntent = Intent.makeRestartActivityTask(intent?.component)
    startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
    if (isFinish) finish()
}

// 重启APP
fun Fragment.restartApp(isFinish: Boolean = true) = activity?.restartApp(isFinish)

// 打开悬浮窗设置页面
fun Activity.goDrawOverlaysSettings(onFailure: ((e: Exception) -> Unit)? = null) {
    try {
        val sdkInt = Build.VERSION.SDK_INT
        if (sdkInt >= Build.VERSION_CODES.O) { //8.0以上
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, 100)
        } else if (sdkInt >= Build.VERSION_CODES.M) { //6.0-8.0
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 100)
        } else { //4.4-6.0以下
            //无需处理了
            SettingsCompat.manageDrawOverlays(this)
        }
    } catch (e: Exception) {
        onFailure?.invoke(e)
    }
}

// 是否拥有悬浮窗权限
val canDrawOverlays: Boolean get() = SettingsCompat.canDrawOverlays(APP.INSTANCE)

// 返回到桌面
fun Activity.moveTaskToLauncher() {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    intent.addCategory(Intent.CATEGORY_HOME)
    startActivity(intent)
}

// 返回到桌面
fun Fragment.moveTaskToLauncher() {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    intent.addCategory(Intent.CATEGORY_HOME)
    startActivity(intent)
}

/***********************************其他**************************************/

/**
 * 导航目的地标签
 */
val Fragment.navigationDestinationLabel: String?
    get() {
        findNavController().currentDestination?.label?.let {
            val title = StringBuffer()
            val fillInPattern = Pattern.compile("\\{(.+?)\\}")
            val matcher = fillInPattern.matcher(it)
            while (matcher.find()) {
                val argName = matcher.group(1)
                if (arguments != null && arguments!!.containsKey(argName)) {
                    matcher.appendReplacement(title, "")
                    title.append(arguments!![argName].toString())
                } else {
                    return null
                }
            }
            matcher.appendTail(title)
            return title.toString()
        }
        return null
    }

/**
 * 设置ToolBar为模拟ActionBar效果，并设置Navigation返回按钮和事件
 */
fun Fragment.setActionBarAsToolbar(
    toolbar: View,
    displayShowTitleEnabled: Boolean = false,
    title: String? = null,
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true,
    menuResId: Int? = null,
    menuItemClickListener: ((MenuItem) -> Unit)? = null
) = (toolbar as? Toolbar)?.run {
    setActionBar(
        this,
        displayShowTitleEnabled,
        title,
        isTopLevelDestination,
        isPaddingStatusBarHeight,
        menuResId,
        menuItemClickListener
    )
}

/**
 * 设置ToolBar为模拟ActionBar效果，并设置Navigation返回按钮和事件
 */
fun Fragment.setActionBar(
    toolbar: Toolbar,
    displayShowTitleEnabled: Boolean = false,
    title: String? = null,
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true
) {
    toolbar.applyCompatActionBar(
        title = if (displayShowTitleEnabled) navigationDestinationLabel ?: title
        else title,
        isTopLevelDestination = isTopLevelDestination,
        isPaddingStatusBarHeight = isPaddingStatusBarHeight
    ) {
        activity?.onBackPressed()
    }
}

/**
 * 设置ToolBar为模拟ActionBar效果
 */
fun Toolbar.applyCompatActionBar(
    title: String? = null, // 注意，如果使用自定义布局时，请设置tag为：@string/view_tag_custom_toolbar_title_text_view
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true,
    onBackPressedCallback: (() -> Unit)? = null
) {
    if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
        // 设置系统状态栏
        if (isPaddingStatusBarHeight) {
            setPaddingStatusBarHeight(true, false)
        }
        // 自适应高度时，设置Toolbar高度为actionBar默认高度
        layoutParams = layoutParams.apply {
            height = context.getActionBarHeight() + paddingTop + paddingBottom
        }
    } else {
        // 非自适应高度时，不再重新设置高度；但设置系统状态栏自动扩展高度
        if (isPaddingStatusBarHeight) {
            setPaddingStatusBarHeight(true, true)
        }
    }
    fastTitle = title
    if (!isTopLevelDestination) {
        navigationIcon = DrawerArrowDrawable(context).apply {
            progress = 1f
        }
        setNavigationContentDescription(androidx.navigation.ui.R.string.nav_app_bar_navigate_up_description)
        setNavigationOnClickListener {
            onBackPressedCallback?.invoke()
        }
    } else {
        navigationIcon = null
    }
}

/**
 * 快速设置/获取标题（支持自定义title布局）
 * 注意，如果使用自定义布局时，请设置tag为：@string/view_tag_custom_toolbar_title_text_view
 */
var Toolbar.fastTitle: String?
    set(value) {
        val tvCustomTitle =
            findViewWithTag<TextView>(R.string.view_tag_custom_toolbar_title_text_view.string)
        if (tvCustomTitle != null) tvCustomTitle.text = value
        else this.title = value
    }
    get() {
        val tvCustomTitle =
            findViewWithTag<TextView>(R.string.view_tag_custom_toolbar_title_text_view.string)
        return if (tvCustomTitle != null) tvCustomTitle.text?.toString()
        else this.title?.toString()
    }


/**
 * 设置ToolBar为模拟ActionBar效果，并设置Navigation返回按钮和事件
 */
fun Fragment.setActionBar(
    toolbar: Toolbar,
    displayShowTitleEnabled: Boolean = false,
    title: String? = null,
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true,
    menuResId: Int? = TOOLBAR_NO_ACTION,
    onMenuItemClick: ((MenuItem) -> Unit)? = null
) {
    setActionBar(
        toolbar,
        displayShowTitleEnabled,
        title,
        isTopLevelDestination,
        isPaddingStatusBarHeight
    )
    toolbar.setMenu(menuResId, onMenuItemClick)
}

/**
 * 设置ToolBar为模拟ActionBar效果，并设置Navigation返回按钮和事件
 */
fun Fragment.setActionBar(
    toolbar: Toolbar,
    displayShowTitleEnabled: Boolean = false,
    title: String? = null,
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true,
    hasMenu: LiveData<Boolean>,
    onInflateMenu: () -> Int,
    onMenuItemClick: ((MenuItem) -> Unit)? = null
) {
    setActionBar(
        toolbar,
        displayShowTitleEnabled,
        title,
        isTopLevelDestination,
        isPaddingStatusBarHeight
    )
    toolbar.setMenu(this.viewLifecycleOwner, hasMenu, onInflateMenu, onMenuItemClick)
}


/**
 * 设置ToolBar为模拟ActionBar效果，并设置返回按钮和事件（Activity）
 */
fun ComponentActivity.setActionBarAsToolbar(
    toolbar: View,
    showTitleResId: Int? = null,
    showTitle: String? = null,
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true,
    isTransparentStatusBar: Boolean = true,
    menuResId: Int? = null,
    menuItemClickListener: ((MenuItem) -> Unit)? = null
) = (toolbar as? Toolbar)?.run {
    setActionBar(
        this,
        showTitleResId,
        showTitle,
        isTopLevelDestination,
        isPaddingStatusBarHeight,
        isTransparentStatusBar,
        menuResId,
        menuItemClickListener
    )
}

/**
 * 设置ToolBar为模拟ActionBar效果，并设置返回按钮和事件（Activity）
 */
fun ComponentActivity.setActionBar(
    toolbar: Toolbar,
    showTitleResId: Int? = null,
    showTitle: String? = null,
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true,
    isTransparentStatusBar: Boolean = true
) {
    if (isTransparentStatusBar) {
        setupTransparentStatusBar()
    }
    toolbar.applyCompatActionBar(
        title = showTitleResId?.string ?: showTitle,
        isTopLevelDestination = isTopLevelDestination,
        isPaddingStatusBarHeight = isPaddingStatusBarHeight
    ) {
        onBackPressed()
    }
//    if (toolbar.layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
//        // 设置系统状态栏
//        if (isPaddingStatusBarHeight) {
//            toolbar.setPaddingStatusBarHeight(true, false)
//        }
//        // 自适应高度时，设置Toolbar高度为actionBar默认高度
//        toolbar.layoutParams = toolbar.layoutParams.apply {
//            height = getActionBarHeight() + toolbar.paddingTop + toolbar.paddingBottom
//        }
//    } else {
//        // 非自适应高度时，不再重新设置高度；同时在设置系统状态栏，自动扩展高度
//        if (isPaddingStatusBarHeight) {
//            toolbar.setPaddingStatusBarHeight(true, true)
//        }
//    }
//    if (showTitleResId != null) {
//        toolbar.setTitle(showTitleResId)
//    } else {
//        toolbar.title = showTitle
//    }
//    if (!isTopLevelDestination) {
//        toolbar.navigationIcon = DrawerArrowDrawable(toolbar.context).apply {
//            progress = 1f
//        }
//        toolbar.setNavigationContentDescription(androidx.navigation.ui.R.string.nav_app_bar_navigate_up_description)
//        toolbar.setNavigationOnClickListener {
//            onBackPressed()
//        }
//    } else {
//        toolbar.navigationIcon = null
//    }
}

/**
 * 设置ToolBar为模拟ActionBar效果，并设置返回按钮和事件（Activity）
 */
fun ComponentActivity.setActionBar(
    toolbar: Toolbar,
    showTitleResId: Int? = null,
    showTitle: String? = null,
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true,
    isTransparentStatusBar: Boolean = true,
    menuResId: Int? = TOOLBAR_NO_ACTION,
    onMenuItemClick: ((MenuItem) -> Unit)? = null
) {
    setActionBar(
        toolbar,
        showTitleResId,
        showTitle,
        isTopLevelDestination,
        isPaddingStatusBarHeight,
        isTransparentStatusBar
    )
    toolbar.setMenu(menuResId, onMenuItemClick)
}

/**
 * 设置ToolBar为模拟ActionBar效果，并设置返回按钮和事件（Activity）
 */
fun ComponentActivity.setActionBar(
    toolbar: Toolbar,
    showTitleResId: Int? = null,
    showTitle: String? = null,
    isTopLevelDestination: Boolean = false,
    isPaddingStatusBarHeight: Boolean = true,
    isTransparentStatusBar: Boolean = true,
    hasMenu: LiveData<Boolean>,
    onInflateMenu: () -> Int,
    onMenuItemClick: ((MenuItem) -> Unit)? = null
) {
    setActionBar(
        toolbar,
        showTitleResId,
        showTitle,
        isTopLevelDestination,
        isPaddingStatusBarHeight,
        isTransparentStatusBar
    )
    toolbar.setMenu(this, hasMenu, onInflateMenu, onMenuItemClick)
}

/**
 * 为Toolbar设置菜单
 */
fun Toolbar.setMenu(
    owner: LifecycleOwner,
    hasMenu: LiveData<Boolean>,
    onInflateMenu: () -> Int,
    onMenuItemClick: ((MenuItem) -> Unit)? = null
) {
    hasMenu.observe(owner) {
        if (!it) clearMenu()
        else setMenu(onInflateMenu())
    }
    setMenu(onMenuItemClick = onMenuItemClick)
}

/**
 * 设置菜单
 * menuResId为-1时，不对菜单做更改
 * 若menuItemClickListener为空，则使用上一个设置的menuItemClickListener
 */
fun Toolbar.setMenu(
    menuResId: Int? = TOOLBAR_NO_ACTION,
    onMenuItemClick: ((MenuItem) -> Unit)? = null
): Menu {
    if (menuResId == null) {
        clearMenu()
    } else if (menuResId != TOOLBAR_NO_ACTION) {
        clearMenu()
        inflateMenu(menuResId)
    }
    if (onMenuItemClick != null) {
        setOnMenuItemClickListener {
            onMenuItemClick(it)
            true
        }
    }
    return menu
}

val TOOLBAR_NO_ACTION: Int = -1

/**
 * 清除存在的菜单
 */
fun Toolbar.clearMenu() = menu.clear()

/**
 * 监听返回键
 */
fun Fragment.setupOnBackPressed(onBackPressedCallback: () -> Unit) =
    setupOnBackPressedDelayedNavigateUp {
        onBackPressedCallback()
        null
    }

/**
 * 监听返回键，如果返回值非空，则休眠该值（毫秒）后自动向上导航
 */
fun Fragment.setupOnBackPressedDelayedNavigateUp(onBackPressedCallback: () -> Long?) {
    requireActivity().onBackPressedDispatcher.addCallback(this,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val delayMillis = onBackPressedCallback.invoke()
                if (delayMillis == null) {
                } else if (delayMillis > 0) {
                    Handler().postDelayed({
                        findNavController().navigateUp()
                    }, delayMillis)
                } else {
                    findNavController().navigateUp()
                }
            }
        })
}

/**
 * 设置显示隐藏动画
 */
fun View.setAnimationVisibility(animation: Animation, visibility: Int) {
    if (this.visibility != visibility) {
        startAnimation(animation)
        this.visibility = visibility
    }
}


/**
 * 判断颜色是否为亮色
 */
val Int.isLightColor get() = ColorUtils.calculateLuminance(this) >= 0.5
val Int.isLightColorOfResource get() = ColorUtils.calculateLuminance(this.color) >= 0.5

/**
 * 获取当前版本名
 */
val Context.versionName
    get() = packageManager?.getPackageInfo(
        APP.CONTEXT.packageName, 0
    )?.versionName

/**
 * 获取当前版本号
 */
val Context.versionCode: String
    get() = PackageInfoCompat.getLongVersionCode(
        packageManager.getPackageInfo(
            packageName,
            0
        )
    ).toString()


val Fragment.appCompatActivity: AppCompatActivity? get() = activity as? AppCompatActivity

/**
 * 复制日历对象
 */
fun Calendar.copy(): Calendar {
    val copyTime = time
    return Calendar.getInstance().apply {
        time = copyTime
    }
}

/**
 * 是否为网络资源
 */
val String.isInternetResources: Boolean
    get() = startsWith(
        "http://",
        true
    ) || startsWith("https://", true)


/**
 * 测量后回调
 */
inline fun <T : View> T.afterMeasure(crossinline onAfterMeasureCallback: T.() -> Unit) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if (measuredWidth > 0 && measuredHeight > 0) {
                removeOnLayoutChangeListener(this)
                onAfterMeasureCallback()
            }
        }

    })
}

/**
 * 设置列表滚动时自动隐藏软键盘
 */
fun Fragment.setupAutoHideSoftInput(
    rvView: RecyclerView,
    lifecycleOwner: LifecycleOwner = viewLifecycleOwner
) = requireActivity().setupAutoHideSoftInput(rvView, lifecycleOwner)

/**
 * 设置列表滚动时自动隐藏软键盘
 */
fun Activity.setupAutoHideSoftInput(
    rvView: RecyclerView,
    lifecycleOwner: LifecycleOwner
) {
    rvView.setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) hideSoftInputFromWindow()
    }
    rvView.isFocusable = false
    rvView.isFocusableInTouchMode = false
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            lifecycleOwner.lifecycle.removeObserver(this)
            rvView.onFocusChangeListener = null
        }
    })
}

/**
 * 从父控件中删除自己
 */
fun View.removeSelfFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}


/**
 * TextView自定义a标签（URLSpan）点击事件处理
 */
fun TextView.handleUrlClicks(onClicked: ((String) -> Unit)? = null) {
    //create span builder and replaces current text with it
    text = text.handleUrlClicks(onClicked)
    //make sure movement method is set
    movementMethod = LinkMovementMethod.getInstance()
}

/**
 * 为CharSequence的a标签（URLSpan）添加自定义点击事件处理
 */
fun CharSequence.handleUrlClicks(onClicked: ((String) -> Unit)? = null): CharSequence =
    SpannableStringBuilder.valueOf(this).apply {
        //search for all URL spans and replace all spans with our own clickable spans
        getSpans(0, length, URLSpan::class.java).forEach {
            //add new clickable span at the same position
            setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onClicked?.invoke(it.url)
                    }
                },
                getSpanStart(it),
                getSpanEnd(it),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            //remove old URLSpan
            removeSpan(it)
        }
    }

/**
 * 复制到粘贴板
 */
fun String.copyToClipboard() {
    (APP.INSTANCE.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
        ClipData.newPlainText(
            "text",
            this
        )
    )
}

/**
 * 通过包名检查是否有安装APP
 */
val String.isInstallAppByPackageName: Boolean
    get() = APP.INSTANCE.packageManager.getInstalledPackages(
        0
    ).firstOrNull { it.packageName == this } != null