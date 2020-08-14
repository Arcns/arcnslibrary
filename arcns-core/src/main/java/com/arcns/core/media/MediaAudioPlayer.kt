package com.arcns.core.media

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.arcns.core.APP
import java.util.*

/**
 * 音频播放器
 */
class MediaAudioPlayer(
    context: Context,
    private val progressCallbackRate: Long = 10, // 进度回调频率
    private val onHandlerCallback: ((Int, Int?) -> Unit)? = null // 播放进度、状态更新等的处理回调
) :
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener {
    private var mMediaPlayer: MediaPlayer? = null
    private var mTimer: Timer? = null
    private var mTimerTask: TimerTask? = null
    private var mAudioManager: AudioManager? = null

    /**
     * 是否已准备好播放（成功加载源文件）
     */
    var isReadyPlay: Boolean = false
        private set

    /**
     * 播放时长
     */
    val duration: Int
        get() = mMediaPlayer?.duration ?: 0

    /**
     * 返回当前播放位置
     */
    val currentPosition: Int get() = mMediaPlayer?.currentPosition ?: 0

    /**
     * 剩余播放时长
     */
    val remainingDuration: Int get() = duration - currentPosition

    /**
     * 是否正在播放
     */
    val isPlaying: Boolean
        get() = mMediaPlayer?.isPlaying == true

    /**
     * 是否暂停
     */
    var isPause: Boolean = false
        private set

    /**
     * 获取MediaPlayer
     */
    fun getMediaPlayer(): MediaPlayer? = mMediaPlayer

    init {
        try {
            mMediaPlayer = MediaPlayer()
            //设置音频流属性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaPlayer?.setAudioAttributes(
                    // 设置媒体流类型
                    AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build()
                )
            } else {
                mMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC) // 设置媒体流类型
            }
            // 监听播放准备回调
            mMediaPlayer?.setOnPreparedListener(this)
            // 监听播放完成回调
            mMediaPlayer?.setOnCompletionListener(this)
            // 监听播放错误回调
            mMediaPlayer?.setOnErrorListener(this)
            // 获取管理器
            mAudioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置生命周期感知
     */
    fun setLifecycleOwner(lifecycleOwner: LifecycleOwner): MediaAudioPlayer {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                lifecycleOwner.lifecycle.removeObserver(this)
                release()
            }
        })
        return this
    }

    /**
     * 设置播放源（本地或网络地址）
     */
    fun setSource(source: String?) {
        mAudioManager?.mode = AudioManager.MODE_NORMAL
        mMediaPlayer?.reset()
        mMediaPlayer?.setDataSource(source) // 设置数据源
    }

    /**
     * 设置播放源（Uri）
     */
    fun setSource(source: Uri) {
        mAudioManager?.mode = AudioManager.MODE_NORMAL
        mMediaPlayer?.reset()
        mMediaPlayer?.setDataSource(APP.INSTANCE, source) // 设置数据源
    }

    /**
     * 设置播放源（自定义设置）
     */
    fun setSource(setSourceHandler: (MediaPlayer?) -> Unit) {
        mAudioManager?.mode = AudioManager.MODE_NORMAL
        mMediaPlayer?.reset()
        setSourceHandler.invoke(mMediaPlayer)
    }

    /**
     * 设置播放源（本地或网络地址）并播放
     */
    fun setSourceAndPlay(source: String?): Boolean {
        setSource(source)
        return start()
    }

    /**
     * 设置播放源（Uri）并播放
     */
    fun setSourceAndPlay(source: Uri): Boolean {
        setSource(source)
        return start()
    }

    /**
     * 设置播放源（自定义设置）并播放
     */
    fun setSourceAndPlay(setSourceHandler: (MediaPlayer?) -> Unit): Boolean {
        setSource(setSourceHandler)
        return start()
    }

    /**
     * 开始播放
     */
    fun start(): Boolean {
        try {
            if (isReadyPlay) {
                mMediaPlayer?.start() //播放
                if (isPause) {
                    onHandlerCallback?.invoke(HANDLER_PLAY, currentPosition)
                }
            } else {
                mMediaPlayer?.prepareAsync() // 加载并播放
                onHandlerCallback?.invoke(HANDLER_PLAY, currentPosition)
            }
            startPlayTimerTask()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * 初始化播放进度定时器
     */
    private fun startPlayTimerTask() {
        if (mTimer != null) return
        isPause = false
        mTimer = Timer()
        mTimerTask = object : TimerTask() {
            override fun run() {
                if (mMediaPlayer?.isPlaying != true) return
                onHandlerCallback?.invoke(HANDLER_CUR_TIME, currentPosition)
            }
        }
        mTimer?.schedule(mTimerTask, 0, progressCallbackRate)
    }

    // 暂停
    fun pause(): Boolean {
        if (mMediaPlayer == null || remainingDuration < 500) return false
        isPause = true
        mTimer?.cancel()
        mTimer = null
        mMediaPlayer?.pause()
        onHandlerCallback?.invoke(HANDLER_PAUSE, null)
        return true
    }

    // 停止
    fun stop() {
        if (mTimer == null) return
        mTimer?.cancel()
        mTimer = null
        mMediaPlayer?.pause()
        mMediaPlayer?.stop()
        isReadyPlay = false
    }

    // 释放
    fun release() {
        stop()
        mMediaPlayer?.release()
        mMediaPlayer = null
    }

    /**
     * 跳转（保持原有的播放状态，也就是此时如果未播放，则跳转后也会保持未播放的状态）
     */
    fun seekTo(msec: Int) {
        mMediaPlayer?.seekTo(msec)
    }

    /**
     * 跳转并播放
     */
    fun seekToAndPlay(msec: Int) {
        seekTo(msec)
        start()
    }

    /**
     * 播放完播放准备
     */
    override fun onPrepared(mp: MediaPlayer) {
        onHandlerCallback?.invoke(HANDLER_PREPARED, mMediaPlayer?.duration)
        mp.start()
        isReadyPlay = true
    }

    // 播放完成回调
    override fun onCompletion(mp: MediaPlayer) {
        if (mTimer == null) {
            return
        }
        stop()
        onHandlerCallback?.invoke(HANDLER_COMPLETE, null)
    }

    /**
     * 播放错误回调
     */
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        stop()
        onHandlerCallback?.invoke(HANDLER_ERROR, null)
        return false
    }

    // 静态相关
    companion object {
        const val HANDLER_CUR_TIME = 1 //当前播放状态时间
        const val HANDLER_PREPARED = 2 //装备好了
        const val HANDLER_PAUSE = 3 //暂停
        const val HANDLER_PLAY = 4 // 开始播放/继续播放
        const val HANDLER_COMPLETE = 0 //完成
        const val HANDLER_ERROR = -28 //错误
    }


}