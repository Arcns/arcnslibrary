package com.arcns.media.audio

import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Message
import androidx.lifecycle.*
import com.arcns.core.APP
import com.arcns.core.util.Event
import com.arcns.core.util.LOG
import com.arcns.core.file.getRandomAudioCacheFilePath
import com.arcns.core.util.fastValue
import com.czt.mp3recorder.MP3Recorder
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat

enum class MediaAudioRecorderState {
    Ready, Disable, Recording, RecordingPause, RecordingFinish, RecordingFailed
}

enum class MediaAudioPlayerState {
    None, Ready, Playing, PlayingPause, PlayingFailed
}

class MediaAudioRecorderPlayerViewModel : ViewModel() {

    // 加载中
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // 定时器相关
    private var timerDeferred: Deferred<*>? = null
    private var timerSeconds = MutableLiveData<Long>() // 单位；秒
    var timerToString: LiveData<String> = Transformations.map(timerSeconds) { seconds ->
        (seconds / 60).let {
            if (it < 10) "0$it" else "$it"
        } + ":" + (seconds % 60).let {
            if (it < 10) "0$it" else "$it"
        }
    }

    // 事件
    private val _eventClose = MutableLiveData<Event<Unit>>()
    val eventClose: LiveData<Event<Unit>> = _eventClose
    private val _eventWaveLineAnim = MutableLiveData<Event<Boolean>>()
    val eventWaveLineAnim: LiveData<Event<Boolean>> = _eventWaveLineAnim
    private val _eventRecorderCompleted = MutableLiveData<Event<String>>()
    val eventRecorderCompleted: LiveData<Event<String>> = _eventRecorderCompleted

    // 录音相关
    private var maxRecorderSeconds = MutableLiveData<Long>() //单位：秒
    private var _recorderState = MutableLiveData<MediaAudioRecorderState>().apply {
        value = MediaAudioRecorderState.Ready
    }
    var recorderState: LiveData<MediaAudioRecorderState> = _recorderState
    private var recorder: MP3Recorder? = null
    private var recorderFilePath = MutableLiveData<String>()

    // 播放时长
    private val playerDuration = MutableLiveData<Long>() //单位毫秒
    val playerDurationToString: LiveData<String> = Transformations.map(playerDuration) {
        "/ " + SimpleDateFormat("mm:ss").format(it)
    }
    private val playerCurrent = MutableLiveData<Long>() //单位毫秒
    val playerCurrentToString: LiveData<String> = Transformations.map(playerCurrent) {
        SimpleDateFormat("mm:ss").format(it)
    }

    // 播放相关
    private var _playerState = MutableLiveData<MediaAudioPlayerState>().apply {
        value = MediaAudioPlayerState.None
    }
    var playerState: LiveData<MediaAudioPlayerState> = _playerState
    private var audioPlayer: MediaAudioPlayer? = null
    private var playerPath = MutableLiveData<String>()

    // 显示的时间
    var showTimeToString = Transformations.switchMap(playerState) {
        if (playerState.value == MediaAudioPlayerState.None) {
            return@switchMap timerToString
        } else {
            return@switchMap playerCurrentToString
        }
    }

    /**
     * 初始化录音机
     */
    fun setupRecorder(isDefaultAutoRecorder: Boolean = false, maxRecorderSeconds: Long = 3600) {
        _recorderState.value = MediaAudioRecorderState.Ready
        _playerState.value = MediaAudioPlayerState.None
        timerSeconds.value = 0
        playerCurrent.value = 0
        playerDuration.value = 0
        this.maxRecorderSeconds.value = maxRecorderSeconds
        if (isDefaultAutoRecorder) {
            startRecorder()
        }
    }

    /**
     * 初始化播放器
     */
    fun setupPlayer(isDefaultAutoPlayer: Boolean = false, path: String) {
        _recorderState.value = MediaAudioRecorderState.Disable
        _playerState.value = MediaAudioPlayerState.Ready
        timerSeconds.value = 0
        playerCurrent.value = 0
        playerDuration.value = 0
        setPlayer(path)
        if (isDefaultAutoPlayer) {
            startPlayer()
        }
    }

    /**
     * 点击中间按钮
     */
    fun onClickCenter() {
        when (recorderState.value) {
            // 录制中、录制暂停时，停止录制
            MediaAudioRecorderState.Recording, MediaAudioRecorderState.RecordingPause -> stopRecorder()
            // 准备录制、录制错误时，开始录制
            MediaAudioRecorderState.Ready, MediaAudioRecorderState.RecordingFailed -> startRecorder()
            else -> when (playerState.value) {
                // 暂停播放时，继续播放
                MediaAudioPlayerState.PlayingPause -> continuePlayer()
                // 准备播放、播放错误时，开始播放
                MediaAudioPlayerState.Ready, MediaAudioPlayerState.PlayingFailed -> startPlayer()
                // 播放中时，暂停播放
                MediaAudioPlayerState.Playing -> pausePlayer()
                else -> Unit
            }
        }
    }

    /**
     * 点击右边按钮
     */
    fun onClickRight() {
        when (recorderState.value) {
            // 录制中时，暂停录制
            MediaAudioRecorderState.Recording -> pauseRecorder()
            // 暂停录制时，继续录制
            MediaAudioRecorderState.RecordingPause -> continueRecorder()
            // 完成录制时，提交
            MediaAudioRecorderState.RecordingFinish -> completedRecorder()
            else -> Unit
        }
    }

    /**
     * 点击左边按钮
     */
    fun onClickLeft() {
        onClose()
    }

    /**
     * 关闭
     */
    fun onClose() {
        stopRecorder()
        stopPlayer()
        _eventClose.value = Event(Unit)
    }

    /**
     * 开始定时器
     */
    fun startTimer(startTimerSeconds: Long? = 0) {
        timerDeferred?.cancel()
        timerDeferred = viewModelScope.async(Dispatchers.IO) {
            while (true) {
                delay(1000)
                val current = (timerSeconds.value ?: 0) + 1
                timerSeconds.postValue(current)
                if (current >= maxRecorderSeconds.value ?: 0) {
                    // 超过最大时长停止录制
                    viewModelScope.launch {
                        stopRecorder()
                    }
                    return@async
                }
            }
        }
        timerSeconds.value = startTimerSeconds ?: 0
    }

    /**
     * 停止定时器
     */
    fun stopTimer() {
        timerDeferred?.cancel()
        timerDeferred = null
    }


    /**
     * 开始录音
     */
    fun startRecorder() {
        if (timerDeferred != null ||
            recorder != null ||
            (recorderState.value != MediaAudioRecorderState.Ready && recorderState.value != MediaAudioRecorderState.RecordingFailed)
        ) {
            return
        }
        _recorderState.value = MediaAudioRecorderState.Recording
        try {
            // 开始录制
            recorderFilePath.value = getRandomAudioCacheFilePath()
            recorder = MP3Recorder(File(recorderFilePath.value))
            recorder?.start()
            // 启动定时器
            startTimer()
            // 启动动画
            _eventWaveLineAnim.value = Event(true)
        } catch (e: Exception) {
            LOG("录制发生错误")
            stopRecorder(MediaAudioRecorderState.RecordingFailed)
        }
    }


    /**
     * 暂停录音
     */
    fun pauseRecorder() {
        recorder?.isPause = true
        _recorderState.value = MediaAudioRecorderState.RecordingPause
        stopTimer()
        // 停止动画
        _eventWaveLineAnim.value = Event(false)
    }

    /**
     * 继续录音
     */
    fun continueRecorder() {
        recorder?.isPause = false
        _recorderState.value = MediaAudioRecorderState.Recording
        startTimer(timerSeconds.value)
        // 启动动画
        _eventWaveLineAnim.value = Event(true)
    }

    /**
     * 停止录音
     */
    fun stopRecorder(state: MediaAudioRecorderState = MediaAudioRecorderState.RecordingFinish) {
        val durationSeconds = timerSeconds.value
        stopTimer()
        recorder?.isPause = false
        recorder?.stop()
        recorder = null
        _recorderState.value = state
        if (recorderFilePath.value != null && File(recorderFilePath.value).exists()) {
            setPlayer(recorderFilePath.value, durationSeconds)
        }
        // 停止动画
        _eventWaveLineAnim.value = Event(false)
    }

    /**
     * 完成录音（提交）
     */
    fun completedRecorder() {
        _eventRecorderCompleted.value = Event(recorderFilePath.value ?: return)
        onClose()
    }

    /**
     * 设置播放
     */
    fun setPlayer(path: String?, durationSeconds: Long? = null) {
        if (path == null) {
            return
        }
        playerPath.value = path
        _playerState.value = MediaAudioPlayerState.Ready
        if (durationSeconds != null) {
            // 直接赋值时长
            playerDuration.value = durationSeconds.toLong() * 1000
        } else {
            // 获取真实的播放时长
            viewModelScope.async(Dispatchers.IO) {
                _loading.postValue(true)
                var mediaPlayer = MediaPlayer()
                val duration = try {
                    mediaPlayer.setDataSource(APP.INSTANCE, Uri.parse(path));
                    mediaPlayer.prepare()
                    mediaPlayer.duration
                } catch (e: java.lang.Exception) {
                    0
                } finally {
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                    mediaPlayer.release()
                }
                playerDuration.postValue(duration.toLong())
                _loading.postValue(false)
            }
        }
    }

    /**
     * 开始播放
     */
    fun startPlayer() {
        if (audioPlayer != null) {
            return
        }
        audioPlayer = MediaAudioPlayer(APP.INSTANCE) { type, data ->
            when (type) {
                // 更新时间
                MediaAudioPlayer.HANDLER_CUR_TIME -> {
                    playerCurrent.fastValue = (data ?: 0).toLong()
                }
                // 播放结束
                MediaAudioPlayer.HANDLER_COMPLETE -> finishPlayer()
                // 播放开始
                MediaAudioPlayer.HANDLER_PREPARED -> {
                    playerCurrent.fastValue = 0
                    playerDuration.fastValue = (data ?: 0).toLong()
                }
                // 播放错误
                MediaAudioPlayer.HANDLER_ERROR -> failedPlayer()
            }
        }
        if (audioPlayer?.playBySetDataSource {
                it?.setDataSource(
                    APP.INSTANCE,
                    Uri.parse(playerPath.value)
                )
            } == 100) {
            _playerState.fastValue = MediaAudioPlayerState.Playing
            // 启动动画
            _eventWaveLineAnim.fastValue = Event(true)
        } else {
            _playerState.fastValue = MediaAudioPlayerState.PlayingFailed
        }
    }

    /**
     * 暂停播放
     */
    fun pausePlayer() {
        audioPlayer?.pause()
        _playerState.value = MediaAudioPlayerState.PlayingPause
        // 停止动画
        _eventWaveLineAnim.value = Event(false)
    }

    /**
     * 继续播放
     */
    fun continuePlayer() {
        audioPlayer?.play()
        _playerState.value = MediaAudioPlayerState.Playing
        // 启动动画
        _eventWaveLineAnim.value = Event(true)
    }

    /**
     * 播放结束
     */
    fun finishPlayer() {
        stopPlayer()
        playerCurrent.fastValue = playerDuration.value
        _playerState.fastValue = MediaAudioPlayerState.Ready
    }

    /**
     * 播放错误
     */
    fun failedPlayer() {
        stopPlayer()
        _playerState.fastValue = MediaAudioPlayerState.PlayingFailed
    }

    /**
     * 停止播放
     */
    fun stopPlayer() {
        if (playerState.value == MediaAudioPlayerState.None) {
            return
        }
        audioPlayer?.pause()
        audioPlayer?.stop()
        audioPlayer = null
        _playerState.fastValue = MediaAudioPlayerState.Ready
        // 停止动画
        _eventWaveLineAnim.fastValue = Event(false)
    }
}