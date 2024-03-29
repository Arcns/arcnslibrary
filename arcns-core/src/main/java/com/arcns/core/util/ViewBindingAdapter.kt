package com.arcns.core.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcns.core.APP
import com.arcns.core.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.textfield.TextInputLayout
import java.io.File


// 设置TextView Drawable的大小
@BindingAdapter(
    value = [
        "customDrawableLeftAny",
        "customDrawableLeftRid",
        "customDrawableLeft",
        "customDrawableLeftSize",
        "customDrawableLeftWidth",
        "customDrawableLeftHeight",
        "customDrawableLeftTint",
        "customDrawableTopAny",
        "customDrawableTopRid",
        "customDrawableTop",
        "customDrawableTopSize",
        "customDrawableTopWidth",
        "customDrawableTopHeight",
        "customDrawableTopTint",
        "customDrawableRightAny",
        "customDrawableRightRid",
        "customDrawableRight",
        "customDrawableRightSize",
        "customDrawableRightWidth",
        "customDrawableRightHeight",
        "customDrawableRightTint",
        "customDrawableBottomAny",
        "customDrawableBottomRid",
        "customDrawableBottom",
        "customDrawableBottomSize",
        "customDrawableBottomWidth",
        "customDrawableBottomHeight",
        "customDrawableBottomTint"],
    requireAll = false
)
fun customDrawable(
    textView: TextView,
    drawableLeftAny: Any? = null,
    drawableLeftRid: Int? = null,
    drawableLeft: Drawable? = null,
    drawableLeftSize: Float? = null,
    drawableLeftWidth: Float? = null,
    drawableLeftHeight: Float? = null,
    drawableLeftTint: Int? = null,
    drawableTopAny: Any? = null,
    drawableTopRid: Int? = null,
    drawableTop: Drawable? = null,
    drawableTopSize: Float? = null,
    drawableTopWidth: Float? = null,
    drawableTopHeight: Float? = null,
    drawableTopTint: Int? = null,
    drawableRightAny: Any? = null,
    drawableRightRid: Int? = null,
    drawableRight: Drawable? = null,
    drawableRightSize: Float? = null,
    drawableRightWidth: Float? = null,
    drawableRightHeight: Float? = null,
    drawableRightTint: Int? = null,
    drawableBottomAny: Any? = null,
    drawableBottomRid: Int? = null,
    drawableBottom: Drawable? = null,
    drawableBottomSize: Float? = null,
    drawableBottomWidth: Float? = null,
    drawableBottomHeight: Float? = null,
    drawableBottomTint: Int? = null
) {
    var left = try {
        drawableLeftRid?.drawable ?: drawableLeft ?: drawableLeftAny?.drawable
    } catch (e: Exception) {
        null
    }
    var top = try {
        drawableTopRid?.drawable ?: drawableTop ?: drawableTopAny?.drawable
    } catch (e: Exception) {
        null
    }
    var right = try {
        drawableRightRid?.drawable ?: drawableRight ?: drawableRightAny?.drawable
    } catch (e: Exception) {
        null
    }
    var bottom = try {
        drawableBottomRid?.drawable ?: drawableBottom ?: drawableBottomAny?.drawable
    } catch (e: Exception) {
        null
    }
    // 检查是否有需要加载的资源
    if (left == null || top == null || right == null || bottom == null) {
        val checkWaitLoad: (Drawable?, Any?) -> Boolean = { drawable, any ->
            drawable == null && ((any is String && any.isGlideStringResources) || any is Uri || any is File || any is GlideUrl)
        }
        var leftIsWaitLoad = checkWaitLoad(left, drawableLeftAny)
        var topIsWaitLoad = checkWaitLoad(top, drawableTopAny)
        var rightIsWaitLoad = checkWaitLoad(right, drawableRightAny)
        var bottomIsWaitLoad = checkWaitLoad(bottom, drawableBottomAny)
        if (leftIsWaitLoad || topIsWaitLoad || rightIsWaitLoad || bottomIsWaitLoad) {
            // 检查是否均加载完成
            val checkLoaded: () -> Unit = {
                if (!leftIsWaitLoad && !topIsWaitLoad && !rightIsWaitLoad && !bottomIsWaitLoad)
                    customDrawable(
                        textView = textView,
                        drawableLeft = left,
                        drawableLeftSize = drawableLeftSize,
                        drawableLeftWidth = drawableLeftWidth,
                        drawableLeftHeight = drawableLeftHeight,
                        drawableLeftTint = drawableLeftTint,
                        drawableTop = top,
                        drawableTopSize = drawableTopSize,
                        drawableTopWidth = drawableTopWidth,
                        drawableTopHeight = drawableTopHeight,
                        drawableTopTint = drawableTopTint,
                        drawableRight = right,
                        drawableRightSize = drawableRightSize,
                        drawableRightWidth = drawableRightWidth,
                        drawableRightHeight = drawableRightHeight,
                        drawableRightTint = drawableRightTint,
                        drawableBottom = bottom,
                        drawableBottomSize = drawableBottomSize,
                        drawableBottomWidth = drawableBottomWidth,
                        drawableBottomHeight = drawableBottomHeight,
                        drawableBottomTint = drawableBottomTint
                    )
            }
            // 开始加载资源
            textView.loadDrawable(drawableLeftAny, onFailed = {
                leftIsWaitLoad = false
                checkLoaded()
            }) {
                left = it
                leftIsWaitLoad = false
                checkLoaded()
            }
            textView.loadDrawable(drawableTopAny, onFailed = {
                topIsWaitLoad = false
                checkLoaded()
            }) {
                top = it
                topIsWaitLoad = false
                checkLoaded()
            }
            textView.loadDrawable(drawableRightAny, onFailed = {
                rightIsWaitLoad = false
                checkLoaded()
            }) {
                right = it
                rightIsWaitLoad = false
                checkLoaded()
            }
            textView.loadDrawable(drawableBottomAny, onFailed = {
                bottomIsWaitLoad = false
                checkLoaded()
            }) {
                bottom = it
                bottomIsWaitLoad = false
                checkLoaded()
            }
            // 完成加载网络资源后再进行设置
            return
        }
    }
    // 资源格式化并加载到控件中
    val formatDrawable: (Drawable?, Int?, Float?, Float?, Float?) -> Drawable? =
        { curDrawable, curTint, curSize, curWidth, curHeight ->
            var newDrawable: Drawable? = curDrawable
            newDrawable?.apply {
                curTint?.let {
                    newDrawable = DrawableCompat.wrap(this).mutate()
                    DrawableCompat.setTint(this, it)
                }
                if (curSize != null) {
                    setBounds(0, 0, curSize.toInt(), curSize.toInt())
                } else if (curWidth != null && curHeight != null) {
                    setBounds(0, 0, curWidth.toInt(), curHeight.toInt())
                }
                //        var left = 0
//        var top = 0
//        var right = drawableRightSize?.toInt() ?: drawableRightWidth?.toInt() ?: return@apply
//        var bottom = drawableRightSize?.toInt() ?: drawableRightHeight?.toInt() ?: return@apply
//        // 修复andorid5.1及以下版本 rightDrawable错位的问题
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
//            left = -textView.paddingRight
//            top = (textView.height - bottom) / 2
//            right -= textView.paddingRight
//            bottom += top
//        }
//        setBounds(left, top, right, bottom)
            }
        }
    textView.setCompoundDrawables(
        formatDrawable(
            left,
            drawableLeftTint,
            drawableLeftSize,
            drawableLeftWidth,
            drawableLeftHeight
        ),
        formatDrawable(
            top,
            drawableTopTint,
            drawableTopSize,
            drawableTopWidth,
            drawableTopHeight
        ),
        formatDrawable(
            right,
            drawableRightTint,
            drawableRightSize,
            drawableRightWidth,
            drawableRightHeight
        ),
        formatDrawable(
            bottom,
            drawableBottomTint,
            drawableBottomSize,
            drawableBottomWidth,
            drawableBottomHeight
        )
    )
}

//
//// 设置EditText Drawable的大小
//@BindingAdapter(
//    value = [
//        "customDrawableLeft",
//        "customDrawableLeftSize",
//        "customDrawableLeftWidth",
//        "customDrawableLeftHeight",
//        "customDrawableTop",
//        "customDrawableTopSize",
//        "customDrawableTopWidth",
//        "customDrawableTopHeight",
//        "customDrawableRight",
//        "customDrawableRightSize",
//        "customDrawableRightWidth",
//        "customDrawableRightHeight",
//        "customDrawableBottom",
//        "customDrawableBottomSize",
//        "customDrawableBottomWidth",
//        "customDrawableBottomHeight"],
//    requireAll = false
//)
//fun customDrawable(
//    editText: EditText,
//    drawableLeft: Drawable?,
//    drawableLeftSize: Float?,
//    drawableLeftWidth: Float?,
//    drawableLeftHeight: Float?,
//    drawableTop: Drawable?,
//    drawableTopSize: Float?,
//    drawableTopWidth: Float?,
//    drawableTopHeight: Float?,
//    drawableRight: Drawable?,
//    drawableRightSize: Float?,
//    drawableRightWidth: Float?,
//    drawableRightHeight: Float?,
//    drawableBottom: Drawable?,
//    drawableBottomSize: Float?,
//    drawableBottomWidth: Float?,
//    drawableBottomHeight: Float?
//) {
//    drawableLeft?.apply {
//        if (drawableLeftSize != null) {
//            setBounds(0, 0, drawableLeftSize.toInt(), drawableLeftSize.toInt())
//        } else if (drawableLeftWidth != null && drawableLeftHeight != null) {
//            setBounds(0, 0, drawableLeftWidth.toInt(), drawableLeftHeight.toInt())
//        }
//    }
//    drawableTop?.apply {
//        if (drawableTopSize != null) {
//            setBounds(0, 0, drawableTopSize.toInt(), drawableTopSize.toInt())
//        } else if (drawableTopWidth != null && drawableTopHeight != null) {
//            setBounds(0, 0, drawableTopWidth.toInt(), drawableTopHeight.toInt())
//        }
//    }
//    drawableRight?.apply {
//        if (drawableRightSize != null) {
//            setBounds(0, 0, drawableRightSize.toInt(), drawableRightSize.toInt())
//        } else if (drawableRightWidth != null && drawableRightHeight != null) {
//            setBounds(0, 0, drawableRightWidth.toInt(), drawableRightHeight.toInt())
//        }
//    }
//    drawableBottom?.apply {
//        if (drawableBottomSize != null) {
//            setBounds(0, 0, drawableBottomSize.toInt(), drawableBottomSize.toInt())
//        } else if (drawableBottomWidth != null && drawableBottomHeight != null) {
//            setBounds(0, 0, drawableBottomWidth.toInt(), drawableBottomHeight.toInt())
//        }
//    }
//    editText.setCompoundDrawables(drawableLeft, drawableTop, drawableRight, drawableBottom)
//}


/**
 * 把状态栏的高度填充到控件的内边距中
 */
@BindingAdapter("paddingStatusBarHeight")
fun paddingStatusBarHeight(view: View, notRepeat: Boolean) {
    view.setPaddingStatusBarHeight(notRepeat)
}

/**
 * 把状态栏的高度填充到控件的外边距中
 */
@BindingAdapter("marginStatusBarHeight")
fun marginStatusBarHeight(view: View, margin: Boolean) {
    if (margin) {
        view.setMarginStatusBarHeight()
    }
}

/**
 * 按屏幕大小的一定比例设置宽度
 */
@BindingAdapter("widthSetToAverageScreen")
fun widthSetToAverageScreen(view: View, average: Float) {
    val outMetrics = DisplayMetrics()
    (view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)?.defaultDisplay
        .getMetrics(
            outMetrics
        )
    view.layoutParams = view.layoutParams.apply {
        width = (outMetrics.widthPixels.toFloat() / average).toInt()
    }
}

/**
 * 设置自适应标题，允许在标题hint和内容hint使用不同的两个字符串
 */
@BindingAdapter(
    value = [
        "setAdaptiveHintTitle",
        "setAdaptiveHintContent",
        "setAdaptiveHintValue"
    ],
    requireAll = false
)
fun TextInputLayout.setAdaptiveHint(
    hintTitle: String,
    hintContent: String,
    hintValue: String? = null
) {
    hint =
        if (hintValue.isNullOrEmpty()) hintContent
        else hintTitle
    editText?.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
        hint = if (hasFocus or !editText?.text.isNullOrEmpty()) hintTitle else hintContent
    }
}

/**
 * 加载图片，支持String（path），Int（res）、Uri，file
 */
@BindingAdapter(
    value = [
        "setImage",
        "setImagePlaceholderDrawable",
        "setImageErrorDrawable",
        "setImageSize",
        "setImageWidth",
        "setImageHeight",
        "setImageCenterInside",
        "setImageCache",
        "setImageNoFade",
        "setImageAsBackground",
        "setImageHighQualityBitmap",
        "setImageAsGif"
    ],
    requireAll = false
)
fun ImageView.setImage(
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
    setImageViaGlide(
        image,
        placeholderDrawable,
        errorDrawable,
        size,
        w,
        h,
        centerInside,
        cache,
        noFade,
        asBackground,
        highQualityBitmap,
        asGif
    )
}


/**
 * 动画显示及隐藏
 */
@BindingAdapter(
    value = [
        "visibilityAnimShow",
        "visibilityAnimHide",
        "visibilityAnimState"
    ]
)
fun setAnimationVisibility(
    view: View,
    visibilityAnimShow: Animation,
    visibilityAnimHide: Animation,
    visibilityAnimState: Int?
) {
    if (visibilityAnimState == null || visibilityAnimState == View.GONE) {
        view.visibility = View.GONE
        return
    }
    view.setAnimationVisibility(
        if (visibilityAnimState == View.VISIBLE) visibilityAnimShow else visibilityAnimHide,
        visibilityAnimState
    )
}

/**
 * 使用软键盘动态调整界面大小能够透明状态栏，并且不会造成状态栏的布局改变
 */
@BindingAdapter("setCompatibleSoftInputAdjustResize")
fun setCompatibleSoftInputAdjustResize(view: View, compatible: Boolean) {
    if (compatible) {
        view.fitsSystemWindows = true
        ViewCompat.setOnApplyWindowInsetsListener(view) { view, insets ->
            insets.replaceSystemWindowInsets(0, 0, 0, insets.systemWindowInsetBottom).apply {
                ViewCompat.onApplyWindowInsets(view, this)
            }
        }
    }
}

/**
 * 动态设置约束者布局
 */
@BindingAdapter(
    value = [
        "bindConstraintLeftToLeft",
        "bindConstraintLeftToRight",
        "bindConstraintTopToTop",
        "bindConstraintTopToBottom",
        "bindConstraintRightToRight",
        "bindConstraintRightToLeft",
        "bindConstraintBottomToBottom",
        "bindConstraintBottomToTop",
        "bindConstraintClearUnSet"
    ],
    requireAll = false
)
fun bindConstraint(
    view: View,
    constraintLeftToLeft: View?,
    constraintLeftToRight: View?,
    constraintTopToTop: View?,
    constraintTopToBottom: View?,
    constraintRightToRight: View?,
    constraintRightToLeft: View?,
    constraintBottomToBottom: View?,
    constraintBottomToTop: View?,
    constraintClearUnSet: Boolean?
) {
    val clearUnSet = constraintClearUnSet ?: false
    val parent = view.parent as? ConstraintLayout ?: return
    val constraintSet = ConstraintSet()
    constraintSet.clone(parent)
    // left
    if (constraintLeftToLeft != null || constraintLeftToRight != null) {
        val startSide = ConstraintSet.LEFT
        val endID = (constraintLeftToLeft ?: constraintLeftToRight)!!.id
        val endSide =
            if (constraintLeftToLeft != null) ConstraintSet.LEFT else ConstraintSet.RIGHT
        constraintSet.connect(view.id, startSide, endID, endSide)
    } else if (clearUnSet) {
        constraintSet.clear(view.id, ConstraintSet.LEFT)
    }
    // top
    if (constraintTopToTop != null || constraintTopToBottom != null) {
        val startSide = ConstraintSet.TOP
        val endID = (constraintTopToTop ?: constraintTopToBottom)!!.id
        val endSide =
            if (constraintTopToTop != null) ConstraintSet.TOP else ConstraintSet.BOTTOM
        constraintSet.connect(view.id, startSide, endID, endSide)
    } else if (clearUnSet) {
        constraintSet.clear(view.id, ConstraintSet.TOP)
    }
    // right
    if (constraintRightToRight != null || constraintRightToLeft != null) {
        val startSide = ConstraintSet.RIGHT
        val endID = (constraintRightToRight ?: constraintRightToLeft)!!.id
        val endSide =
            if (constraintRightToRight != null) ConstraintSet.RIGHT else ConstraintSet.LEFT
        constraintSet.connect(view.id, startSide, endID, endSide)
    } else if (clearUnSet) {
        constraintSet.clear(view.id, ConstraintSet.RIGHT)
    }
    // bottom
    if (constraintBottomToBottom != null || constraintBottomToTop != null) {
        val startSide = ConstraintSet.BOTTOM
        val endID = (constraintBottomToBottom ?: constraintBottomToTop)!!.id
        val endSide =
            if (constraintBottomToBottom != null) ConstraintSet.BOTTOM else ConstraintSet.TOP
        constraintSet.connect(view.id, startSide, endID, endSide)
    } else if (clearUnSet) {
        constraintSet.clear(view.id, ConstraintSet.BOTTOM)
    }
    constraintSet.applyTo(parent)
}


@BindingAdapter(
    value = [
        "bindPaddingLeft",
        "bindPaddingTop",
        "bindPaddingRight",
        "bindPaddingBottom"
    ],
    requireAll = false
)
fun bindPadding(
    view: View,
    paddingLeft: Float?,
    paddingTop: Float?,
    paddingRigth: Float?,
    paddingBottom: Float?
) {
    view.setPadding(
        paddingLeft?.toInt() ?: view.paddingLeft,
        paddingTop?.toInt() ?: view.paddingTop,
        paddingRigth?.toInt() ?: view.paddingRight,
        paddingBottom?.toInt() ?: view.paddingBottom
    )
}

@BindingAdapter(
    value = [
        "bindMarginLeft",
        "bindMarginTop",
        "bindMarginRight",
        "bindMarginBottom"
    ],
    requireAll = false
)
fun bindMargin(
    view: View,
    marginLeft: Float?,
    marginTop: Float?,
    marginRigth: Float?,
    marginBottom: Float?
) {

    val layoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
    layoutParams.setMargins(
        marginLeft?.toInt() ?: layoutParams.leftMargin,
        marginTop?.toInt() ?: layoutParams.topMargin,
        marginRigth?.toInt() ?: layoutParams.rightMargin,
        marginBottom?.toInt() ?: layoutParams.bottomMargin
    )
    view.layoutParams = layoutParams
}

// 让CoordinatorLayout和AppBarLayout滚动能够自适应，在列表内容未撑满时禁用，在撑满时开启
@BindingAdapter(
    value = [
        "bindAppBarLayoutAdaptiveScroll",
        "bindAppBarLayoutAdaptiveScrollLifecycleOwner"
    ],
    requireAll = false
)
fun RecyclerView.bindAppBarLayoutAdaptiveScroll(
    appBarLayoutChildren: View,
    lifecycleOwner: LifecycleOwner? = null
) {
    // 删除上一个监听，避免重复监听
    (tag as? View.OnLayoutChangeListener)?.run {
        removeOnLayoutChangeListener(this)
        tag = null
    }
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
            tag = this
            val layoutManager = layoutManager as? LinearLayoutManager ?: return
            var itemCount = (adapter as? ListAdapter<*, *>)?.itemCount
                ?: (adapter as? RecyclerView.Adapter)?.itemCount ?: return
            if (getTag(R.string.app_name)?.toString()?.toIntOrNull() == itemCount) {
                return
            }
            setTag(R.string.app_name, itemCount.toString())
            var firstPosition =
                layoutManager.findFirstCompletelyVisibleItemPosition()
            var lastPosition =
                layoutManager.findLastCompletelyVisibleItemPosition()
            if (itemCount == 0 || (firstPosition == 0 && lastPosition + 1 == itemCount)) {
                bindAppBarLayoutScrollEnabled(appBarLayoutChildren, false)
            } else {
                bindAppBarLayoutScrollEnabled(appBarLayoutChildren, true)
            }
        }
    })
    lifecycleOwner?.lifecycle?.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            lifecycleOwner.lifecycle.removeObserver(this)
            // 删除监听，避免泄漏
            (tag as? View.OnLayoutChangeListener)?.run {
                removeOnLayoutChangeListener(this)
                tag = null
            }
        }
    })
}

@BindingAdapter("bindAppBarLayoutScrollEnabled")
fun bindAppBarLayoutScrollEnabled(view: View, enabled: Boolean) {
    (view.layoutParams as? AppBarLayout.LayoutParams)?.run {
        val newScrollFlags = if (enabled) {
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else {
            AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
        }
        if (scrollFlags == newScrollFlags) {
            return@run
        }
        scrollFlags = newScrollFlags
        view.layoutParams = this
    }

}


/**
 * 自动换行 整齐排版
 */
@BindingAdapter(
    value = [
        "setAutoSplitText",
        "setAutoSplitTexIndent",
        "setAutoSplitTexDrawableSize"
    ],
    requireAll = false
)
fun TextView.setAutoSplitText(
    rawText: String?,
    indent: String? = null,
    drawableSize: Float? = null
) {
    post {
        val a = paddingLeft
        val b = paddingRight
        val c = width
        val autoSplitText = if (!rawText.isNullOrEmpty() && c != 0) {
            val tvWidth = c - a - b.toFloat() - (drawableSize?.toInt() ?: 0) //空间可用宽度

            //将缩进处理成空格
            var indentSpace = ""
            var indentWidth = 0f
            if (!TextUtils.isEmpty(indent)) {
                val rawIndentWidth: Float = paint.measureText(indent)
                if (rawIndentWidth < tvWidth) {
                    while (paint.measureText(indentSpace)
                            .also({ indentWidth = it }) < rawIndentWidth
                    ) {
                        indentSpace += " "
                    }
                }
            }

            //将原始文本按行拆分
            val rawTextLines =
                rawText.replace("\r".toRegex(), "").split("\n".toRegex()).toTypedArray()
            val sbNewText = StringBuilder()
            for (rawTextLine in rawTextLines) {
                if (paint.measureText(rawTextLine) <= tvWidth) {
                    //如果行宽度在空间范围之内，就不处理了
                    sbNewText.append(
                        """
                        $rawTextLine
                        
                        """.trimIndent()
                    )
                } else {
                    //否则按字符测量，在超过可用宽度的前一个字符处，手动替换，加上换行，缩进
                    var lineWidth = 0f
                    var i = 0
                    while (i != rawTextLine.length) {
                        val ch = rawTextLine[i]
                        //从手动换行的第二行开始加上缩进
                        if (lineWidth < 0.1f && i != 0) {
                            sbNewText.append(indentSpace)
                            lineWidth += indentWidth
                        }
                        val textWidth: Float = paint.measureText(ch.toString())
                        lineWidth += textWidth
                        if (lineWidth < tvWidth) {
                            sbNewText.append(ch)
                        } else {
                            sbNewText.append("\n")
                            lineWidth = 0f
                            --i
                        }
                        ++i
                    }
                    sbNewText.append("\n")
                }
            }
            //结尾多余的换行去掉
            if (!rawText.endsWith("\n")) {
                sbNewText.deleteCharAt(sbNewText.length - 1)
            }
            sbNewText.toString()
        } else {
            ""
        }
        text = autoSplitText
    }

}