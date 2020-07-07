package com.arcns.core.app

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * 唤醒APP广播
 */
class WakeAppReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION = "com.arcns.core.app.WakeAppReceiver"
        private const val KEY_WAKE_APP_PACKAGE_NAME = "KEY_WAKE_APP_PACKAGE_NAME"

        /**
         * 创建唤醒广播意图
         */
        fun newIntent(context: Context, packageName: String = context.packageName): Intent = Intent(
            // 明确Receiver，否则在安卓10及以上无法接收到广播
            context, WakeAppReceiver::class.java
        ).apply {
            action = WakeAppReceiver.ACTION
            putExtra(KEY_WAKE_APP_PACKAGE_NAME, packageName)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.wakeApp(intent?.getStringExtra(KEY_WAKE_APP_PACKAGE_NAME) ?: context.packageName)
    }
}

/**
 * 唤醒APP
 */
fun Context.wakeApp(packageName: String): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    // 尝试通过任务列表唤醒
    if (this.packageName.equals(packageName, true)) {
        activityManager?.getRunningTasks(100)?.forEach {
            if (it.topActivity?.packageName?.equals(packageName) == true) {
                activityManager.moveTaskToFront(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) it.taskId else it.id,
                    0
                )
                return true
            }
        }
    }
    // 无法通过任务列表唤醒时，通过打开包对应app进行唤醒
    packageManager.getLaunchIntentForPackage(packageName)?.run {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
        return true
    }
    return false
}