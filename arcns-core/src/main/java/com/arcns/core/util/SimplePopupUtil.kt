package com.arcns.core.util

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.afollestad.materialdialogs.MaterialDialog
import com.arcns.core.APP
import com.arcns.core.R

class SimplePopupUtil(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val data: SimplePopupData,
    val navController: NavController? = null,
    val onCreateMaterialDialo: ((SimplePopupDialog) -> MaterialDialog?)? = null,
    val onCustomMaterialDialo: (MaterialDialog.() -> Unit)? = null,
    var onDialogClickCallback: OnSimplePopupDialogButtonClickCallback? = null
) {

    constructor(
        fragment: Fragment,
        data: SimplePopupData,
        navController: NavController? = null,
        onCreateMaterialDialo: ((SimplePopupDialog) -> MaterialDialog?)? = null,
        onCustomMaterialDialo: (MaterialDialog.() -> Unit)? = null,
        onDialogClickCallback: OnSimplePopupDialogButtonClickCallback? = null
    ) : this(
        fragment.requireContext(),
        fragment.viewLifecycleOwner,
        data,
        navController,
        onCreateMaterialDialo,
        onCustomMaterialDialo,
        onDialogClickCallback
    )

    constructor(
        activity: AppCompatActivity,
        data: SimplePopupData,
        navController: NavController? = null,
        onCreateMaterialDialo: ((SimplePopupDialog) -> MaterialDialog?)? = null,
        onCustomMaterialDialo: (MaterialDialog.() -> Unit)? = null,
        onDialogClickCallback: OnSimplePopupDialogButtonClickCallback? = null
    ) : this(
        activity,
        activity,
        data,
        navController,
        onCreateMaterialDialo,
        onCustomMaterialDialo,
        onDialogClickCallback
    )

    private var toast: Toast? = null

    init {
        data.toast.observe(lifecycleOwner, EventObserver {
            toast?.cancel()
            toast = Toast.makeText(context, it, Toast.LENGTH_LONG)
            toast?.show()
        })
        data.dialog.observe(lifecycleOwner, EventObserver(true) {
            onCreateMaterialDialo?.invoke(it)?.show {
                setSimplePopupDialog(it.copy())
            } ?: context.showDialog {
                setSimplePopupDialog(it.copy())
            }
        })
        navController?.addOnDestinationChangedListener { _, _, _ ->
            if (data.isDestinationChangedAutoStop) data.stopLoading()
        }
    }

    private fun MaterialDialog.setSimplePopupDialog(
        simplePopupDialog: SimplePopupDialog
    ) {
        simplePopupDialog.title?.run { title(text = this) }
        message(text = simplePopupDialog.content)
        if (simplePopupDialog.positiveButton != null) {
            positiveButton(text = simplePopupDialog.positiveButton?.text) { _ ->
                dismiss()
                simplePopupDialog.positiveButton?.onClickCallback?.invoke(simplePopupDialog.positiveButton?.clickKey)
                onDialogClick(simplePopupDialog.positiveButton?.clickKey)
            }
        } else {
            positiveButton(res = R.string.simple_popup_dialog_confirm_button)
        }
        if (simplePopupDialog.negativeButton != null) {
            negativeButton(text = simplePopupDialog.negativeButton?.text) { _ ->
                dismiss()
                simplePopupDialog.negativeButton?.onClickCallback?.invoke(simplePopupDialog.negativeButton?.clickKey)
                onDialogClick(simplePopupDialog.negativeButton?.clickKey)
            }
        }
        if (simplePopupDialog.neutralButton != null) {
            neutralButton(text = simplePopupDialog.neutralButton?.text) { _ ->
                dismiss()
                simplePopupDialog.neutralButton?.onClickCallback?.invoke(simplePopupDialog.neutralButton?.clickKey)
                onDialogClick(simplePopupDialog.neutralButton?.clickKey)
            }
        }
        if (simplePopupDialog.isForcedDialog) {
            cancelable(false)
            cancelOnTouchOutside(false)
            noAutoDismiss()
        }
        onCustomMaterialDialo?.invoke(this)
    }


    private fun onDialogClick(clickKey: String?) {
        onDialogClickCallback?.invoke(clickKey)
        data.onEventDialogClick(clickKey ?: return)
    }
}


class SimplePopupData(
    var defaultDisableTouchMarginTop: Float = (APP.INSTANCE.getStatusBarHeight() + APP.INSTANCE.getActionBarHeight()).toFloat(),
    var defaultIsEnableDisabledBackground: Boolean? = null
) {

    // 正在加载
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng
    val isLoadIng: Boolean get() = loadIng.value ?: false

    // 禁用触摸
    private var _isDisableTouch = MutableLiveData<Boolean>()
    var isDisableTouch: LiveData<Boolean> = _isDisableTouch

    // 启用背景
    private var _isEnableDisabledBackground = MutableLiveData<Boolean>()
    var isEnableDisabledBackground: LiveData<Boolean> = _isEnableDisabledBackground

    // 禁用触摸的上边距，该区域可以触摸，用于留出返回按钮
    private var _disableTouchMarginTop = MutableLiveData<Float>()
    var disableTouchMarginTop: LiveData<Float> = _disableTouchMarginTop

    // 加载框下的说明文字
    private var _loadingDescription = MutableLiveData<String>()
    var loadingDescription: LiveData<String> = _loadingDescription

    // 目的地更新时自动关闭加载框
    var isDestinationChangedAutoStop: Boolean = true


    /**
     * 开始加载
     */
    fun startLoading(
        isDisableTouch: Boolean = true,// 禁用触摸
        disableTouchMarginTop: Int? = null, // 禁用触摸时，顶部不禁用的高度
        isEnableDisabledBackground: Boolean = defaultIsEnableDisabledBackground
            ?: isDisableTouch, // 启用背景
        isDestinationChangedAutoStop: Boolean = true, // 目的地跳转时自动关闭搜索框
        loadingDescription: String? = null // 加载框下的说明文字
    ) {
        _loadIng.fastValue = true
        _isDisableTouch.fastValue = isDisableTouch
        _isEnableDisabledBackground.fastValue = isEnableDisabledBackground
        _disableTouchMarginTop.fastValue =
            disableTouchMarginTop?.toFloat() ?: defaultDisableTouchMarginTop
        this.isDestinationChangedAutoStop = isDestinationChangedAutoStop
        updateLoadingDescription(loadingDescription)
    }

    /**
     * 更新加载框下的说明文字
     */
    fun updateLoadingDescription(loadingDescription: String? = null) {
        this._loadingDescription.value =
            if (loadingDescription.isNullOrBlank()) null else loadingDescription
    }

    /**
     * 结束加载
     */
    fun stopLoading() {
        _loadIng.fastValue = false
        _isDisableTouch.fastValue = false
    }

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast
    fun showToast(message: String) {
        _toast.fastValue = Event(message)
    }

    fun showToast(messageRes: Int) {
        _toast.fastValue = Event(messageRes.string)
    }

    // 简单弹窗提示
    var _dialog = MutableLiveData<Event<SimplePopupDialog>>()
    var dialog: LiveData<Event<SimplePopupDialog>> = _dialog
    private var _eventOnDialogClick = MutableLiveData<Event<String>>()
    var eventOnDialogClick: LiveData<Event<String>> = _eventOnDialogClick

    // 简单弹窗提示
    fun showDialog(title: String? = null, message: String) {
        _dialog.fastValue = Event(
            SimplePopupDialog(
                title = title,
                content = message
            )
        )
    }

    // 简单弹窗提示
    fun showDialog(dialog: SimplePopupDialog) {
        _dialog.fastValue = Event(dialog)
    }

    // 简单弹窗提示
    fun onEventDialogClick(key: String) {
        _eventOnDialogClick.fastValue = Event(key)
    }

}

data class SimplePopupDialog(
    var title: String? = null,
    var content: String,
    var positiveButton: SimplePopupDialogButton? = null,
    var negativeButton: SimplePopupDialogButton? = null,
    var neutralButton: SimplePopupDialogButton? = null,
    var isForcedDialog: Boolean = false
)

data class SimplePopupDialogButton(
    var text: String,
    var clickKey: String? = text,
    var onClickCallback: OnSimplePopupDialogButtonClickCallback? = null
)

typealias OnSimplePopupDialogButtonClickCallback = (clickKey: String?) -> Unit