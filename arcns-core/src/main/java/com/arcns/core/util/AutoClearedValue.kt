package com.arcns.core.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 使对象能够在Fragment销毁（onDestroyView）时自动设置为NULL
 * 可解决DataBinding等对象造成Fragment内存泄漏等问题
 */
class AutoClearedValue<T : Any>(val fragment: Fragment) : ReadWriteProperty<Fragment, T> {
    private var _value: T? = null

    init {
        fragment.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                fragment.viewLifecycleOwnerLiveData.observe(fragment, Observer {
                    it.lifecycle.addObserver(object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        fun onDestroy() {
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
 * 使用方法：var obj by autoCleared<Obj>()
 * 例如：var binding by autoCleared<FragmentBinding>()
 */
fun <T : Any> Fragment.autoCleared() = AutoClearedValue<T>(this)