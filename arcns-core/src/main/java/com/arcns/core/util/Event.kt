package com.arcns.core.util

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.lang.Exception

open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    fun getContentIfNotHandled(getContentCallback: (T) -> Unit) {
        if (!hasBeenHandled) {
            hasBeenHandled = true
            getContentCallback(content)
        }
    }

    fun peekContent(): T = content
}

class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>) {
        event.getContentIfNotHandled {
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