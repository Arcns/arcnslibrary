package com.arcns.core.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.edit
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.arcns.core.APP
import java.util.*

/**
 * 语言工具
 * 使用方法：
 * 一、Appcompat版本 <= 1.1
 * 1、在application oncreate中初始化，并在attachBaseContext、onConfigurationChanged在调用LocaleUtil.setSystemLocaleLanguage设置系统默认语言
 * 2、在activity的attachBaseContext中使用LocaleUtil.wrapLocaleContext封装super的上下文
 * 3、在需要设置语言的地方调用LocaleUtil.showLocaleSelector即可
 * 兼容5.0，6.0等老版本
 * 1、如果设置语言的地方为Fragment则需要在其onAttach中使用LocaleUtil.wrapLocaleContext封装context的上下文
 * 2、在activity的applyOverrideConfiguration中使用Context.applyOverrideConfiguration覆盖super的语言配置
 *
 *
 *
 * 二、Appcompat版本 >= 1.1
 *  1、在application oncreate中初始化，并在attachBaseContext、onConfigurationChanged在调用LocaleUtil.setSystemLocaleLanguage设置系统默认语言
 *  2、在activity的attachBaseContext中调用applyOverrideConfiguration(Configuration())
 *  3、在activity的applyOverrideConfiguration中使用LocaleUtil.updateConfigurationIfSupported()更新语言配置
 *  4、在需要设置语言的地方调用LocaleUtil.showLocaleSelector即可
 * 兼容5.0，6.0等老版本
 *  1、如果设置语言的地方为Fragment则需要在其onAttach中使用LocaleUtil.wrapLocaleContext封装context的上下文
 *
 */
class LocaleUtil(
    /**
     * 支持的语言列表
     */
    var getLocales: (() -> List<ELocale>)
) {
    // 保存数据的配置文件名和参数名
    private val DATA_NAME = "DATA_NAME"
    private val DATA_KEY_LOCALE = "DATA_KEY_LOCALE"

    // 保存当前上下文，避免重复设置语言
    private var currentContext: Context? = null

    // 当前系统语言
    private var currentSystemLocaleLanguage: String? = null

    /**
     * 返回封装了当前语言的APP上下文
     */
    val appLocaleContext: Context
        get() {
            currentContext = (currentContext
                ?: APP.INSTANCE).setLocale(
                getSaveLocaleAutoConvertSystemLocale()
            )
            return currentContext!!
        }

    /**
     * 返回封装好语言的上下文
     */
    fun wrapLocaleContext(context: Context?): Context? =
        context?.setLocale(getSaveLocaleAutoConvertSystemLocale())

    /**
     * 设置系统语言（在Application的attachBaseContext、onConfigurationChanged回调）
     */
    fun setSystemLocaleLanguage(language: String?) {
        currentSystemLocaleLanguage = language
    }

    /**
     * 保存设置好的当前语言
     */
    fun saveLocale(language: String? = null) =
        APP.INSTANCE.getSharedPreferences(DATA_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(
                    DATA_KEY_LOCALE, if (language.isNullOrBlank()) {
                        null
                    } else language
                )
            }

    /**
     * 获取保存的设置语言，注意如果保存的是跟随系统时将返回null
     */
    fun getSaveLocale(context: Context? = APP.INSTANCE): Locale? =
        context?.getSharedPreferences(DATA_NAME, Context.MODE_PRIVATE)
            ?.getString(
                DATA_KEY_LOCALE,
                null
            )?.let {
                Locale(it)
            }

    /**
     * 获取保存的设置语言，并自动转换跟随系统时的语言
     */
    fun getSaveLocaleAutoConvertSystemLocale(context: Context? = APP.INSTANCE): Locale =
        getSaveLocale(context) ?: Locale(currentSystemLocaleLanguage)


    /**
     * 弹出语言选择框
     */
    fun getLocaleSelector(
        activity: Activity,
        title: String? = null,
        autoSave: Boolean = true,
        selection: ((String?) -> Unit)? = null
    ): MaterialDialog.() -> Unit {
        var currentLocales = getLocales()
        // 获取当前选中的语言
        var currentLocale = getSaveLocale()
        var currentIndex = 0
        var currentItems = currentLocales.mapIndexed { index, eLocale ->
            if (eLocale.language?.equals(currentLocale?.language, true) == true) {
                currentIndex = index
            }
            eLocale.displayName
        }.toList()
        // 弹出框内容
        val setupDialog: MaterialDialog.() -> Unit = {
            this.title(text = title)
            this.noAutoDismiss()
            this.listItemsSingleChoice(
                items = currentItems,
                initialSelection = currentIndex
            ) { _, index, _ ->
                if (currentIndex == index) {
                    return@listItemsSingleChoice
                }
                val language = currentLocales[index].language
                if (autoSave) {
                    saveLocale(language)
                }
                if (selection != null) {
                    selection(language)
                } else {
                    activity.recreate()
                }
            }
        }
        return setupDialog
    }

    /**
     * 弹出语言选择框
     */
    fun showLocaleSelector(
        activity: Activity,
        title: String? = null,
        autoSave: Boolean = true,
        selection: ((String?) -> Unit)? = null
    ) {
        activity.showDialog(
            func = getLocaleSelector(
                activity, title, autoSave, selection
            )
        )
    }
}

data class ELocale(
    var language: String? = null,
    var displayName: String
)


/**
 * 设置新的语言
 */
fun Context.setLocale(
    newLocale: Locale
): Context {
    var configuration = resources.configuration
    // 与当前语言对比，避免重复设置
    if (newLocale.language == configuration.localeLanguage) {
        return this
    }
    // 设置语言
    Locale.setDefault(newLocale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        configuration.setLocale(newLocale)
        configuration.setLayoutDirection(newLocale)
    } else {
        configuration.locale = newLocale
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        createConfigurationContext(configuration)
    } else {
        resources.updateConfiguration(configuration, resources.displayMetrics)
        this
    }


}

/**
 * 获取当前配置的语言
 */
val Context.localeLanguage: String? get() = resources?.configuration?.localeLanguage

/**
 * 获取当前配置的语言
 */
val Configuration.localeLanguage: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        locales[0]
    } else {
        locale
    }.language

/**
 * 适用 <= Appcompat 1.1
 * 应用语言配置（使语言配置修改后能够兼容5.0，6.0等老版本）
 */
fun Context.applyOverrideConfiguration(overrideConfiguration: Configuration?): Configuration? {
    if (overrideConfiguration != null) {
        val uiMode = overrideConfiguration.uiMode
        overrideConfiguration.setTo(resources.configuration)
        overrideConfiguration.uiMode = uiMode
    }
    return overrideConfiguration
}

/**
 * 适用 >= Appcompat 1.2
 * 更新语言配置
 */
fun Configuration.updateConfigurationIfSupported(): Configuration? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        if (!locales.isEmpty) return this
    } else {
        if (locale != null) return this
    }
    val newLocale = APP.localeUtil?.getSaveLocaleAutoConvertSystemLocale()
    if (newLocale != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) setLocale(newLocale)
        else locale = newLocale
    }
    return this
}