package com.example.arcns.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcns.core.util.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ViewModelStartupPage : ViewModel() {

    private var _goLogin = MutableLiveData<Event<String?>>()
    var goLogin: LiveData<Event<String?>> = _goLogin
    private var _goMain = MutableLiveData<Event<Unit>>()
    var goMain: LiveData<Event<Unit>> = _goMain

    init {
        startup()
    }

    private fun startup() {
        viewModelScope.launch {
            delay(1000)
            _goMain.value = Event(Unit)
        }
    }

}