package com.arcns.core.app

import android.content.Context
import android.os.PowerManager
import com.arcns.core.APP

/**
 * CPU唤醒工具（避免锁屏时冻结）
 */
class WakeLockUtil {

    // CPU唤醒标志
    private var foregroundServiceWakeLock: PowerManager.WakeLock? = null

    /**
     * 保持CPU唤醒状态
     * 需要添加<uses-permission android:name="android.permission.WAKE_LOCK"/>权限
     */
    fun acquireWakeLock() {
        val foregroundServiceWakeLockTag = "app:foregroundServiceWakeLockTag"
        foregroundServiceWakeLock =
            (APP.INSTANCE.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                foregroundServiceWakeLockTag
            )
        foregroundServiceWakeLock?.acquire()
    }

    /**
     * 释放CPU唤醒状态
     */
    fun releaseWakeLock() {
        try {
            foregroundServiceWakeLock?.release()
        } catch (e: Exception) {
        }
    }
}