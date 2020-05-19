package com.arcns.core.util

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 使对象能够在Fragment销毁（onDestroyView）时自动设置为NULL
 * 可解决DataBinding等对象造成Fragment内存泄漏等问题
 */
class AutoClearedValue<T : Any>(
    val fragment: Fragment,
    val onCreateCallback: (() -> T?)? = null,
    val onClearCallback: ((value: T?) -> Unit)? = null
) :
    ReadWriteProperty<Fragment, T> {
    private var _value: T? = null

    init {
        fragment.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                fragment.viewLifecycleOwnerLiveData.observe(fragment, Observer {
                    it.lifecycle.addObserver(object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
                        fun onCreate() {
                            onCreateCallback?.invoke()?.run {
                                _value = this
                            }
                        }

                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        fun onDestroy() {
                            onClearCallback?.invoke(_value)
                            _value = null
                        }
                    })
                })
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException(
            "should never call auto-cleared-value get when it might not be available"
        )
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        _value = value
    }
}

/**
 * 使对象能够在Activty销毁（onDestroy）时自动设置为NULL
 */
class ActivityAutoClearedValue<T : Any>(
    val activity: ComponentActivity,
    val onCreateCallback: (() -> T?)? = null,
    val onClearCallback: ((value: T?) -> Unit)? = null
) :
    ReadWriteProperty<ComponentActivity, T> {
    private var _value: T? = null

    init {
        activity.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                onCreateCallback?.invoke().run {
                    _value = this
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                onClearCallback?.invoke(_value)
                _value = null
            }
        })
    }

    override fun getValue(thisRef: ComponentActivity, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException(
            "should never call auto-cleared-value get when it might not be available"
        )
    }

    override fun setValue(thisRef: ComponentActivity, property: KProperty<*>, value: T) {
        _value = value
    }
}

/**
 * 使用方法：var obj by autoCleared<Obj>()
 * 例如：var binding by autoCleared<FragmentBinding>()
 */
fun <T : Any> Fragment.autoCleared(
    onCreateCallback: (() -> T?)? = null,
    onClearCallback: ((value: T?) -> Unit)? = null
) =
    AutoClearedValue<T>(this, onCreateCallback, onClearCallback)


/**
 * 使用方法：var obj by autoCleared<Obj>()
 * 例如：var binding by autoCleared<FragmentBinding>()
 */
fun <T : Any> ComponentActivity.autoCleared(
    onCreateCallback: (() -> T?)? = null,
    onClearCallback: ((value: T?) -> Unit)? = null
) =
    ActivityAutoClearedValue<T>(this, onCreateCallback, onClearCallback)