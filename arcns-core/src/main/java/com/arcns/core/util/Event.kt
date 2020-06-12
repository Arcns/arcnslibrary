package com.arcns.core.util

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

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

inline var <T> MutableLiveData<Event<T>>.eventValue: T
    set(v) {
        value = Event(v)
    }
    get() = throw Exception()
