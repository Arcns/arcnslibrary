package com.example.arcns.util

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.arcns.core.util.showDialog
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.yanzhenjie.permission.AndPermission


/**
 * 结束SmartRefreshLayout刷新动画
 */
fun SmartRefreshLayout.finish(onHeaderCallback: (() -> Unit)? = null) {
    if (state.isHeader) {
        finishRefresh()
        onHeaderCallback?.invoke()
    } else if (state.isFooter) {
        finishLoadMore()
    }
}

fun Fragment.openPermission(
    permissions: Array<String>,
    deniedMsg: String,
    onGranted: () -> Unit
) {
    AndPermission.with(this)
        .runtime()
        .permission(permissions)
        .onGranted {
            onGranted()
        }.onDenied {
            handleAlwaysDeniedPermission(it, deniedMsg)
        }.start()
}


fun Fragment.handleAlwaysDeniedPermission(deniedPermissions: List<String>, deniedMsg: String) {
    if (AndPermission.hasAlwaysDeniedPermission(
            context,
            deniedPermissions
        )
    ) {
        requireActivity().showDialog {
            message(text = deniedMsg)
            positiveButton(text = "去设置") {
                // 拒绝不再询问后跳转设置应用详情页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context?.packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            negativeButton(text = "取消")
        }

    } else {
        Toast.makeText(
            context,
            deniedMsg,
            Toast.LENGTH_LONG
        ).show()
    }
}