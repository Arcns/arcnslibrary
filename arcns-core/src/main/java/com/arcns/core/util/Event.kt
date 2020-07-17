package com.arcns.core.util

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.lang.Exception

open class Event<out T>(content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write
    private var eventContent: Any? = null

    init {
        eventContent = content
    }

    fun getContentIfNotHandled(isDisposable: Boolean = false, getContentCallback: (T) -> Unit) {
        if (!hasBeenHandled) {
            hasBeenHandled = true
            (eventContent as? T)?.run {
                getContentCallback(this)
            }
            if (isDisposable) eventContent = null
        }
    }

    fun peekContent(): T? = eventContent as? T
}

class EventObserver<T>(
    private val isDisposable: Boolean = false, //是否为一次性数据，如果为一次性数据，则数据会在取出后自动置null
    private val onEventUnhandledContent: (T) -> Unit
) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>) {
        event.getContentIfNotHandled(isDisposable) {
            onEventUnhandledContent(it)
        }
    }
}

fun <T> MutableLiveData<Event<T>>.postEventValue(value: T) = postValue(Event(value))

inline var <T> MutableLiveData<Event<T>>.eventValue: T
    get() = throw Exception()
    set(value) {
        this.value = Event(value)
    }


inline var <T> MutableLiveData<Event<T>>.fastEventValue: T
    get() = throw Exception()
    set(value) {
        fastValue = Event(value)
    }