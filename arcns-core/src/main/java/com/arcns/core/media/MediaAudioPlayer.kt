package com.arcns.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import com.arcns.core.APP
import java.util.*

/**
 * 音频播放器（基于MediaPlayer）
 */
class MediaAudioPlayer : MediaAudioBasePlayer(),
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener {
    private var mMediaPlayer: MediaPlayer? = null
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
                APP.INSTANCE.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    override fun start(): Boolean {
        try {
            if (isReadyPlay) {
                mMediaPlayer?.start() //播放
            } else {
                mMediaPlayer?.prepareAsync() // 加载并播放
            }
            isPause = false
            startHandlerUpdateTimerTask()
            submitHandlerCallback(MediaAudoPlayerHandlerType.Play, currentPosition)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    // 暂停
    override fun pause(): Boolean {
        if (mMediaPlayer == null || remainingDuration < 500) return false
        isPause = true
        stopHandlerUpdateTimerTask()
        mMediaPlayer?.pause()
        submitHandlerCallback(MediaAudoPlayerHandlerType.Pause)
        return true
    }

    // 停止
    override fun stop() {
        if (!isReadyPlay) {
            return
        }
        isReadyPlay = false
        stopHandlerUpdateTimerTask()
        mMediaPlayer?.pause()
        mMediaPlayer?.stop()
    }


    /**
     * 跳转（保持原有的播放状态，也就是此时如果未播放，则跳转后也会保持未播放的状态）
     */
    override fun seekTo(msec: Int) {
        mMediaPlayer?.seekTo(msec)
    }

    /**
     * 跳转并播放
     */
    override fun seekToAndPlay(msec: Int) {
        seekTo(msec)
        start()
    }

    // 释放
    override fun release() {
        stop()
        mMediaPlayer?.release()
        mMediaPlayer = null
    }

    /**
     * 销毁
     */
    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 处理更新定时器回调（运行时定时回调）
     */
    override fun onHandlerUpdateTimerTaskCallback() {
        if (mMediaPlayer?.isPlaying != true) return
        submitHandlerCallback(MediaAudoPlayerHandlerType.Update, currentPosition)
    }

    /**
     * 播放完播放准备
     */
    override fun onPrepared(mp: MediaPlayer) {
        submitHandlerCallback(MediaAudoPlayerHandlerType.Prepared, mMediaPlayer?.duration)
        mp.start()
        isReadyPlay = true
    }

    // 播放完成回调
    override fun onCompletion(mp: MediaPlayer) {
        if (!isReadyPlay) {
            return
        }
        stop()
        submitHandlerCallback(MediaAudoPlayerHandlerType.Complete)
    }

    /**
     * 播放错误回调
     */
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        stop()
        submitHandlerCallback(MediaAudoPlayerHandlerType.Error)
        return false
    }

}

