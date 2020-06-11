package com.arcns.core.app

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.arcns.core.util.addShareData
import com.arcns.core.util.getDisposableShareData
import kotlin.reflect.KClass

/**
 * 前台服务
 */
class ForegroundService<T> : Service() {

    // 服务内容
    var serviceContent: T? = null

    // 前台服务配置
    var options: ForegroundServiceOptions<T>? = null

    // CPU唤醒工具（避免锁屏时冻结）
    private var wakeLockUtil = WakeLockUtil()

    // 通讯
    private var foregroundServiceBinder =
        ForegroundServiceBinder(this)

    override fun onBind(intent: Intent?): IBinder? = foregroundServiceBinder

    //
    override fun onCreate() {
        super.onCreate()
        options = getDisposableShareData<ForegroundServiceOptions<T>>(
            KEY_OPTIONS
        )?.apply {
            // 通知栏配置
            notificationOptions?.run {
                startForeground(notificationID, createNotification(this) ?: return@run)
            }
            // 服务内容
            serviceContent = onCreateServiceContent(this@ForegroundService)
            // 保持CPU唤醒状态
            if (isAcquireWakeLock) {
                wakeLockUtil.acquireWakeLock()
            }
        }
    }


    // 销毁
    override fun onDestroy() {
        wakeLockUtil.releaseWakeLock()
        options?.onDestroyServiceContent?.invoke(serviceContent)
        super.onDestroy()
    }


    companion object {
        const val KEY_OPTIONS = "KEY_OPTIONS"

        // 默认值
        private var defualtOptionsMap = HashMap<KClass<*>, ForegroundServiceOptions<*>>()
        fun setDefaultOptions(
            kClassKey: KClass<*>,
            options: ForegroundServiceOptions<*>
        ) {
            defualtOptionsMap.put(kClassKey, options)
        }

        fun <T : Any> getDefaultOptions(kClassKey: KClass<T>): ForegroundServiceOptions<T>? {
            return defualtOptionsMap.get(kClassKey) as? ForegroundServiceOptions<T>
        }

        // 开始服务
        inline fun <reified T : Any> startService(
            context: Context?,
            serviceConnection: ForegroundServiceConnection<T>? = null,
            options: ForegroundServiceOptions<T>? = getDefaultOptions(
                T::class
            )
        ) {
            if (options != null) {
                addShareData(KEY_OPTIONS, options)
            }
            // 开启服务
            var intent = Intent(context, ForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(intent)
            } else {
                context?.startService(intent)
            }
            serviceConnection?.run {
                context?.bindService(intent, this, BIND_AUTO_CREATE)
            }
        }
    }
}

/**
 * 新建服务内容
 */
typealias OnCreateForegroundServiceContent<T> = (Context) -> T
/**
 * 销毁服务内容
 */
typealias OnDestroyForegroundServiceContent<T> = (T?) -> Unit

/**
 * 前台服务通讯
 */
class ForegroundServiceBinder<T>(val service: ForegroundService<T>) : Binder() {
    val serviceContent: T? get() = service.serviceContent
}

/**
 * 前台服务连接器
 */
abstract class ForegroundServiceConnection<T> : ServiceConnection {
    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
        onServiceConnected(
            binder = serviceBinder as? ForegroundServiceBinder<T> ?: return,
            serviceContent = serviceBinder.serviceContent
        )
    }

    abstract fun onServiceConnected(binder: ForegroundServiceBinder<T>, serviceContent: T?)
}

/**
 * 前台服务配置
 */
data class ForegroundServiceOptions<T>(
    var onCreateServiceContent: OnCreateForegroundServiceContent<T>,
    var onDestroyServiceContent: OnDestroyForegroundServiceContent<T>? = null,
    var notificationOptions: NotificationOptions? = null,
    var isAcquireWakeLock: Boolean = false
)
