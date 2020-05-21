package com.arcns.core.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * 共享数据池
 */
class ShareDataPool private constructor() {

    private val datas = HashMap<Any, Any>()

    /**
     * 添加数据到共享池（KEY为Value::class）
     */
    inline fun <reified T : Any> add(value: T) = add(T::class, value)

    /**
     * 添加数据到共享池（KEY为Value::class）
     */
    inline fun <reified T : Any> add(lifecycleOwner: LifecycleOwner, value: T) =
        add(lifecycleOwner, T::class, value)

    /**
     * 添加数据到共享池
     */
    fun add(lifecycleOwner: LifecycleOwner, key: Any, value: Any) {
        add(key, value)
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                remove(key)
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        })
    }

    /**
     * 添加数据到共享池
     */
    fun add(key: Any, value: Any) {
        datas[key] = value
    }

    /**
     * 从共享池删除数据
     */
    fun remove(key: Any) = datas.remove(key)

    /**
     * 从共享池删除数据
     */
    fun removeShareDataByValue(value: Any) {
        run remove@{
            datas.forEach {
                if (it.value == value) {
                    remove(it.key)
                    return@remove
                }
            }
        }
    }

    /**
     * 从共享池清空所有数据
     */
    fun clear() = datas.clear()

    /**
     * 从共享池获取数据（KEY为左侧对象::class，类型自动转换为左侧对象的类型）
     */
    inline fun <reified T : Any> get(): T? = get(T::class) as? T

    /**
     * 从共享池获取数据（类型自动转换为左侧对象的类型）
     */
    inline fun <reified T : Any> get(key: Any): T? = getShareDataByKey(key) as? T

    /**
     * 从共享池获取数据
     */
    fun getShareDataByKey(key: Any): Any? = datas[key]

    //
    companion object {
        /**
         * 单例对象
         */
        val instance: ShareDataPool by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ShareDataPool()
        }
    }
}

/**
 * 共享池委托
 */
class ShareDataGetValueProxy<T : Any>(private val key: Any, val valueKClass: KClass<T>) :
    ReadOnlyProperty<Any, T?> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return ShareDataPool.instance.getShareDataByKey(key) as? T?
    }
}

/**
 * 从共享池获取数据
 */
inline fun <reified T : Any> getShareData(key: Any = T::class): T? = ShareDataPool.instance.get(key)

/**
 * 添加数据到共享池（KEY为Value::class）
 */
inline fun <reified T : Any> addShareData(lifecycleOwner: LifecycleOwner, value: T) =
    ShareDataPool.instance.add(lifecycleOwner, value)

/**
 * 添加数据到共享池（KEY为Value::class）
 */
inline fun <reified T : Any> addShareData(value: T) = ShareDataPool.instance.add(value)

/**
 * 添加数据到共享池
 */
inline fun addShareData(lifecycleOwner: LifecycleOwner, key: Any, value: Any) =
    ShareDataPool.instance.add(lifecycleOwner, key, value)

/**
 * 添加数据到共享池
 */
inline fun addShareData(key: Any, value: Any) = ShareDataPool.instance.add(key, value)

/**
 * 从共享池删除数据
 */
inline fun removeShareData(key: Any) = ShareDataPool.instance.remove(key)

/**
 * 从共享池删除数据
 */
inline fun removeShareDataByValue(value: Any) = ShareDataPool.instance.removeShareDataByValue(value)

/**
 * 通过委托获取共享数据
 */
inline fun <reified T : Any> shareData(key: Any = T::class) =
    ShareDataGetValueProxy(key = key, valueKClass = T::class)

