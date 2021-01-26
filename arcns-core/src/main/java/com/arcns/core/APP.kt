package com.arcns.core

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.arcns.core.util.LocaleUtil

/**
 *
 */
class APP {
    companion object {
        /**
         * APP单例（必须初始化）
         */
        lateinit var INSTANCE: Application

        /**
         * APP单例支持转换
         */
        fun <T : Application> getInstance(): T? = INSTANCE as? T

        /**
         * APP内容提供者的Authority（默认为包名+.fileprovider）
         * 注意使用前，必须先在manifests中声明
         *  <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
        </provider>
         * @xml/file_paths:
         * <paths>
        <external-files-path
        name="external-files-path"
        path="/." />
        <cache-path
        name="cache"
        path="." />
        </paths>
         */
        var fileProviderAuthority: String? = null
            set(value) {
                field = value
            }
            get() = field ?: "${INSTANCE.packageName}.fileprovider"

        /**
         * 返回语言工具（按需初始化）
         */
        var localeUtil: LocaleUtil? = null

        /**
         * 封装上下文（如果有需要可以自行覆写该方法）
         */
        var wrapContext: (() -> Context) = {
            localeUtil?.appLocaleContext ?: INSTANCE
        }

        /**
         * 返回封装好的上下文（注意如果有使用语言工具时，使用多语言资源时必须调用APP.CONTEXT而不是APP.INSTANCE）
         */
        val CONTEXT: Context get() = wrapContext()


        /**
         * 主线程
         */
        val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

        /**
         * 在主线程中运行
         */
        fun runOnUiThread(r: (() -> Unit)) {
            mainHandler.post {
                r.invoke()
            }
        }
    }
}


