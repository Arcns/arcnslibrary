package com.arcns.core.app

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.arcns.core.APP
import com.arcns.core.R
import com.arcns.core.util.LOG
import com.arcns.core.util.addShareData
import com.arcns.core.util.getDisposableShareData
import com.arcns.core.util.string
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

/**
 * 前台服务
 */
class ForegroundService<T> : Service() {

    // 服务内容
    var serviceContent: T? = null
        private set

    // 前台服务配置
    var options: ForegroundServiceOptions<T>? = null
        private set

    // CPU唤醒工具（避免锁屏时冻结）
    private var wakeLockUtil = WakeLockUtil()

    // 通讯
    private var foregroundServiceBinder =
        ForegroundServiceBinder(this)

    override fun onBind(intent: Intent?): IBinder? = foregroundServiceBinder

    // 通知相关
    var notificationBuilder: NotificationCompat.Builder? = null
        private set

    //
    override fun onCreate() {
        super.onCreate()
        options = getDisposableShareData<ForegroundServiceOptions<T>>(
            KEY_OPTIONS
        )?.apply {
            // 通知栏配置
            notificationOptions?.run {
                notificationBuilder = createBuilder() ?: return@run
                startForeground(notificationID, notificationBuilder?.build())
            }
            // 创建服务内容
            serviceContent = onCreateServiceContent(this@ForegroundService)
            // 保持CPU唤醒状态
            if (isAcquireWakeLock) {
                wakeLockUtil.acquireWakeLock()
            }
        }
    }

    /**
     * 更新通知栏
     */
    fun updateNotification(notificationOptions: NotificationOptions) {
        notificationBuilder?.update(notificationOptions)
        startForeground(
            options?.notificationOptions?.notificationID ?: return,
            notificationBuilder?.build() ?: return
        )
    }


    // 销毁
    override fun onDestroy() {
        // 释放CPU唤醒状态
        wakeLockUtil.releaseWakeLock()
        // 停止前台服务通知
        stopForeground(true)
        // 调用销毁回调
        options?.onDestroyServiceContent?.invoke(serviceContent)
        super.onDestroy()
    }


    companion object {
        const val KEY_OPTIONS = "KEY_OPTIONS"

        // 前台服务默认配置
        private var defualtOptionsMap = HashMap<KClass<*>, ForegroundServiceOptions<*>>()

        // 设置前台服务默认配置
        fun setDefaultOptions(
            kClassKey: KClass<*>,
            options: ForegroundServiceOptions<*>
        ) {
            defualtOptionsMap.put(kClassKey, options)
        }

        // 删除前台服务默认配置
        fun removeDefaultOptions(kClassKey: KClass<*>) = defualtOptionsMap.remove(kClassKey)

        // 返回前台服务默认配置
        fun <T : Any> getDefaultOptions(kClassKey: KClass<T>): ForegroundServiceOptions<T>? {
            return defualtOptionsMap.get(kClassKey) as? ForegroundServiceOptions<T>
        }

        /**
         * 开始服务（创建一个前台服务）
         */
        inline fun <reified T : Any> startService(
            serviceConnection: ForegroundServiceConnection<T>? = null, // 服务连接器
            options: ForegroundServiceOptions<T>? = getDefaultOptions( // 前台服务配置
                T::class
            )
        ) {
            if (options != null) {
                // 保存到共享池中，方便在服务创建后取出
                addShareData(KEY_OPTIONS, options)
            }
            // 开启服务
            var intent = Intent(APP.INSTANCE, ForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                APP.INSTANCE.startForegroundService(intent)
            } else {
                APP.INSTANCE.startService(intent)
            }
            serviceConnection?.run {
                APP.INSTANCE.bindService(intent, this, BIND_AUTO_CREATE)
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
    //服务内容
    val serviceContent: T? get() = service.serviceContent

    /**
     * 停止服务
     */
    fun stopService(): Boolean {
        service.stopSelf()
        return true
    }
}

/**
 * 前台服务连接器
 */
abstract class ForegroundServiceConnection<T> : ServiceConnection {
    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
        val foregroundServiceBinder =
            serviceBinder as? ForegroundServiceBinder<T> ?: return // 服务通讯
        onServiceConnected(
            binder = foregroundServiceBinder, // 服务通讯
            serviceContent = serviceBinder.serviceContent //服务内容
        )
        // 通讯完成后，解除绑定
        APP.INSTANCE.unbindService(this)
    }

    abstract fun onServiceConnected(binder: ForegroundServiceBinder<T>, serviceContent: T?)
}

/**
 * 前台服务配置
 */
open class ForegroundServiceOptions<T>(
    var onCreateServiceContent: OnCreateForegroundServiceContent<T>, // 创建服务内容的回调
    var onDestroyServiceContent: OnDestroyForegroundServiceContent<T>? = null, // 销毁服务内容的回调
    var notificationOptions: ForegroundServiceNotificationOptions? = null, // 前台服务通知的配置
    var isAcquireWakeLock: Boolean = false //是否保持CPU唤醒
)

/**
 * 前台服务通知配置
 */
open class ForegroundServiceNotificationOptions : NotificationOptions {
    constructor(
        channelId: String = UUID.randomUUID().toString(),
        channelName: String = R.string.text_foreground_service_notification_default_channel_name.string,
        notificationID: Int = ((Int.MAX_VALUE / 2)..Int.MAX_VALUE).random(),
        contentTitle: String = R.string.text_foreground_service_notification_default_content_title.string,
        contentText: String = R.string.text_foreground_service_notification_default_content_text.string,
        contentIntent: PendingIntent? = null,
        smallIcon: Int,
        largeIcon: Bitmap? = null,
        defaults: Int? = Notification.DEFAULT_ALL, //默认通知选项
        priority: Int? = NotificationCompat.PRIORITY_MAX, // 通知优先级
        progress: NotificationProgressOptions? = null,//进度
        isOngoing: Boolean? = null,// 是否禁用滑动删除
        // 创建自定义NotificationChannel代替默认
        onCreateNotificationChannel: (() -> NotificationChannel)? = null,
        // 设置NotificationCompatBuilder
        onSettingNotificationCompatBuilder: ((NotificationCompat.Builder) -> Unit)? = null
    ) : super(
        channelId,
        channelName,
        notificationID,
        contentTitle,
        contentText,
        contentIntent,
        smallIcon,
        largeIcon,
        defaults,
        priority,
        progress,
        isOngoing,
        onCreateNotificationChannel,
        onSettingNotificationCompatBuilder
    )
}