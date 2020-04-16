package com.arcns.core.media.audio

import androidx.lifecycle.*
import com.arcns.core.util.LOG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaAudioRecorderPlayerViewModel : ViewModel() {

    var recordSeconds = MutableLiveData<Long>().apply { value = 0 }
    var recordSecondsToString: LiveData<String> = Transformations.map(recordSeconds) { seconds ->
        (seconds / 60).let {
            if (it < 10) "0$it" else "$it"
        } + ":" + (seconds % 60).let {
            if (it < 10) "0$it" else "$it"
        }
    }


    fun startRecord() {
        recordSeconds.value = 0
        viewModelScope.async(Dispatchers.IO) {
            while (true) {
                delay(1000)
                recordSeconds.postValue((recordSeconds.value ?: 0) + 1)
            }
        }
    }

}