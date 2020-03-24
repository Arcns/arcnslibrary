package com.arcns.core.util

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.text.Html
import android.text.Spanned
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.DateTimeCallback
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.datetime.dateTimePicker
import com.arcns.core.APP
import com.google.gson.Gson
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
fun Int.string(vararg formatArgs: Any?) = APP.CONTEXT.getString(this, formatArgs)

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

/***********************************bitmap操作**************************************/

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
 * bitmap压缩
 */
fun Bitmap.compression(): Bitmap? {
    val baos = ByteArrayOutputStream()
    //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
    compress(Bitmap.CompressFormat.JPEG, 100, baos)
    var options = 100
    while (baos.toByteArray().size / 1024 > 50 && options > 10) { //循环判断如果压缩后图片是否大于50kb,大于继续压缩
        baos.reset() //重置baos即清空baos
        compress(Bitmap.CompressFormat.JPEG, options, baos) //这里压缩options%，把压缩后的数据存放到baos中
        options -= 10 //每次都减少10
    }
    //把压缩后的数据baos存放到ByteArrayInputStream中
    val isBm = ByteArrayInputStream(baos.toByteArray())
    return BitmapFactory.decodeStream(isBm, null, null)
}

/***********************************系统状态栏操作**************************************/

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
 * 日期时间选择框
 */
fun Activity.showDateTimePicker(
    @StringRes res: Int? = null,
    title: String? = null,
    currentDateTime: Calendar? = null,
    dateTimeCallback: DateTimeCallback = null
) = (this as Context).showDateTimePicker(res, title, currentDateTime, dateTimeCallback)

/**
 * 日期时间选择框
 */
fun View.showDateTimePicker(
    @StringRes res: Int? = null,
    title: String? = null,
    currentDateTime: Calendar? = null,
    dateTimeCallback: DateTimeCallback = null
) = context.showDateTimePicker(res, title, currentDateTime, dateTimeCallback)

/**
 * 日期时间选择框
 */
fun Context.showDateTimePicker(
    @StringRes res: Int? = null,
    title: String? = null,
    currentDateTime: Calendar? = null,
    dateTimeCallback: DateTimeCallback = null
) {
    MaterialDialog(this).show {
        title(res = res, text = title)
        dateTimePicker(
            currentDateTime = if (currentDateTime != null) Calendar.getInstance().apply {
                time = currentDateTime?.time
            } else null,
            show24HoursView = true,
            dateTimeCallback = dateTimeCallback
        )
    }
}

/**
 * 日期选择框
 */
fun Activity.showDatePicker(
    title: String,
    currentDateTime: Calendar? = null,
    dateCallback: DateTimeCallback = null
) = (this as Context).showDatePicker(title, currentDateTime, dateCallback)

/**
 * 日期选择框
 */
fun View.showDatePicker(
    title: String,
    currentDateTime: Calendar? = null,
    dateCallback: DateTimeCallback = null
) = context.showDatePicker(title, currentDateTime, dateCallback)

/**
 * 日期选择框
 */
fun Context.showDatePicker(
    title: String,
    currentDateTime: Calendar? = null,
    dateCallback: DateTimeCallback = null
) {
    MaterialDialog(this).show {
        title(text = title)
        datePicker(
            currentDate = if (currentDateTime != null) Calendar.getInstance().apply {
                time = currentDateTime?.time
            } else null,
            dateCallback = dateCallback
        )
    }
}

/**
 * 弹出框
 */
fun Activity.showDialog(func: MaterialDialog.() -> Unit): MaterialDialog =
    (this as Context).showDialog(func)

/**
 * 弹出框
 */
fun View.showDialog(func: MaterialDialog.() -> Unit): MaterialDialog = context.showDialog(func)

/**
 * 弹出框
 */
fun Context.showDialog(func: MaterialDialog.() -> Unit): MaterialDialog {
    return MaterialDialog(this).show {
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


/***********************************其他**************************************/

/**
 * 设置ToolBar为ActionBar，并设置NavController
 */
fun Fragment.setActionBarAsToolbar(toolbar: View, displayShowTitleEnabled: Boolean = false) {
    (toolbar as? Toolbar)?.run {
        setActionBar(this, displayShowTitleEnabled)
    }
}

/**
 * 设置ToolBar为ActionBar，并设置NavController
 */
fun Fragment.setActionBar(toolbar: View, displayShowTitleEnabled: Boolean = false) {
    (activity as? AppCompatActivity)?.apply {
        setSupportActionBar(toolbar as Toolbar)
        setupActionBarWithNavController(
            findNavController()
        )
        setupActionBarWithNavController(findNavController())
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