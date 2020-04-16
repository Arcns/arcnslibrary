package com.arcns.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.text.Html
import android.text.Spanned
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.afollestad.materialdialogs.DialogBehavior
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.Companion.DEFAULT_BEHAVIOR
import com.arcns.core.APP
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import me.shouheng.compress.Compress
import me.shouheng.compress.strategy.Strategies
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

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
fun Int.string(value: Any?): String = APP.CONTEXT.getString(this, value)
fun Int.string(value: Any?, value2: Any?): String = APP.CONTEXT.getString(this, value, value2)

// 获取drawable资源
val Int.drawable: Drawable
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return APP.INSTANCE.resources.getDrawable(this, APP.INSTANCE.theme)
        } else {
            return APP.INSTANCE.resources.getDrawable(this)
        }
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


// string序列化为对象
inline fun <reified T> Gson.fromJson(json: String) = fromJson(json, T::class.java)

// string转换为html
val String.html: Spanned
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, 0)
    } else {
        Html.fromHtml(this)
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
    return result;
}

/**
 * 上内边距增加系统状态栏的高度大小
 */
fun View.setPaddingStatusBarHeight() {
    setPadding(
        paddingLeft,
        paddingTop + context.getStatusBarHeight(),
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
        android.util.Log.e("ZKXT_MODE_DEBUG", message)
}

fun String.log() {
    if (MODE_DEBUG)
        android.util.Log.e("ZKXT_MODE_DEBUG", this)
}


/***********************************图片显示、保存与压缩**************************************/


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
        // 设置为src
        requestBuilder.into(this)
    }
}

fun Int.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality
)

fun String.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality
)

fun File.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality
)

fun Uri.saveImageAsLocal(
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80
): File? = saveImageAsLocalOrNull(
    this,
    width,
    height,
    centerInside,
    highQualityBitmap,
    compressQuality
)

fun saveImageAsLocalOrNull(
    image: Any?,
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80
): File? = try {
    saveImageAsLocal(image, width, height, centerInside, highQualityBitmap, compressQuality)
} catch (e: java.lang.Exception) {
    null
}

fun saveImageAsLocal(
    image: Any?,
    width: Float = 0f,
    height: Float = 0f,
    centerInside: Boolean = false,
    highQualityBitmap: Boolean = true,
    compressQuality: Int = 80
): File {
    if (image == null) {
        throw java.lang.Exception("iamge is null")
    }
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
    if (width != 0f || height != 0f) {
        requestBuilder = requestBuilder.override(width.toInt(), height.toInt())
    }
    requestBuilder = if (centerInside) {
        requestBuilder.centerInside()
    } else {
        requestBuilder.centerCrop()
    }
    requestBuilder =
        requestBuilder.format(if (highQualityBitmap) DecodeFormat.PREFER_ARGB_8888 else DecodeFormat.PREFER_RGB_565)
    try {
        // 让Glide在当前线程同步加载
        val bitmap = requestBuilder.submit().get()
        // 开始压缩
        return Compress.with(APP.INSTANCE, bitmap)
            .setQuality(compressQuality)
            .strategy(Strategies.compressor()).get()
    } catch (e: java.lang.Exception) {
        throw e
    }
}

/***********************************打开app**************************************/
fun Context.openAppByPath(path: String, mimeType: String, authority: String? = null) {
    if (path.isInternetResources) {
        openAppByUri(Uri.parse(path), mimeType)
    } else if (authority != null) {
        openAppByFile(File(path), mimeType, authority)
    }
}

fun Context.openAppByUri(uri: Uri, mimeType: String) {
    var intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, mimeType)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.openAppByFile(file: File, mimeType: String, authority: String) {
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
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

/***********************************其他**************************************/

/**
 * 设置ToolBar为ActionBar，并设置NavController
 */
fun Fragment.setActionBarAsToolbar(
    toolbar: View, displayShowTitleEnabled: Boolean = false,
    isTopLevelDestination: Boolean = false
) {
    (toolbar as? Toolbar)?.run {
        setActionBar(this, displayShowTitleEnabled, isTopLevelDestination)
    }
}

/**
 * 设置ToolBar为ActionBar，并设置NavController
 */
fun Fragment.setActionBar(
    toolbar: View,
    displayShowTitleEnabled: Boolean = false,
    isTopLevelDestination: Boolean = false
) {
    appCompatActivity?.apply {
        setSupportActionBar(toolbar as Toolbar)
        if (isTopLevelDestination) {
            // 设置为顶级页面，actionbar不会有后退按钮
            setupActionBarWithNavController(
                findNavController(),
                AppBarConfiguration.Builder(findNavController().currentDestination?.id ?: return)
                    .build()
            )
        } else {
            setupActionBarWithNavController(
                findNavController()
            )
        }
        supportActionBar?.setDisplayShowTitleEnabled(displayShowTitleEnabled)
    }
}


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
 * 获取内容提供者令牌
 */
val Context.fileProviderAuthority: String get() = "$packageName.fileprovider"

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