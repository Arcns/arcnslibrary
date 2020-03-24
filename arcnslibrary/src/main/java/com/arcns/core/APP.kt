package com.arcns.core

import android.app.Application
import android.content.Context
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
    }
}


