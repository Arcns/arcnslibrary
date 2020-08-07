package com.arcns.core.media

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import java.util.*

/**
 * 音频播放器
 */
class MediaAudioPlayer(
    context: Context,
    private val progressCallbackRate: Long = 10, // 进度回调频率
    private val onHandlerCallback: ((Int, Int?) -> Unit)? = null // 播放进度、状态更新等的处理回调
) :
    OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener,
    OnErrorListener {
    private var mMediaPlayer: MediaPlayer? = null
    private var mTimer: Timer? = null
    private var mTimerTask: TimerTask? = null
    private var mAudioManager: AudioManager? = null
    fun play() {
        mAudioManager?.mode = AudioManager.MODE_NORMAL
        if (mMediaPlayer != null) mMediaPlayer?.start()
    }

    /**
     * 播放本地或网络地址的音频
     */
    fun playUrl(url: String?): Boolean {
        try {
            mAudioManager?.mode = AudioManager.MODE_NORMAL
            mMediaPlayer?.reset()
            mMediaPlayer?.setDataSource(url) // 设置数据源
            mMediaPlayer?.prepareAsync() // prepare自动播放
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        setupPlayTimerTask()
        return true
    }

    /**
     * 播放数据源的音频
     */
    fun playBySetDataSource(setDataSource: (MediaPlayer?) -> Unit): Boolean {
        try {
            mAudioManager?.mode = AudioManager.MODE_NORMAL
            mMediaPlayer?.reset()
            setDataSource(mMediaPlayer)// 设置数据源
            mMediaPlayer?.prepareAsync() // prepare自动播放
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        setupPlayTimerTask()
        return true
    }

    private fun setupPlayTimerTask() {
        mTimer = Timer()
        mTimerTask = object : TimerTask() {
            override fun run() {
                if (mMediaPlayer?.isPlaying != true) return
                onHandlerCallback?.invoke(HANDLER_CUR_TIME, mMediaPlayer?.currentPosition)
            }
        }
        mTimer?.schedule(mTimerTask, 0, progressCallbackRate)
    }

    // 暂停
    fun pause() {
        if (mMediaPlayer != null) mMediaPlayer?.pause()
    }

    // 停止
    fun stop() {
        if (mTimer != null) {
            mTimer?.cancel()
        }
        if (mMediaPlayer != null) {
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }

    /**
     * 跳转
     */
    fun seekTo(time: Int) {
        if (mMediaPlayer != null) {
            mMediaPlayer?.seekTo(time)
            mMediaPlayer?.start()
        }
    }

    // 播放准备
    override fun onPrepared(mp: MediaPlayer) {
        onHandlerCallback?.invoke(HANDLER_PREPARED, mMediaPlayer?.duration)
        mp.start()
    }

    // 播放完成
    override fun onCompletion(mp: MediaPlayer) {
        onHandlerCallback?.invoke(HANDLER_COMPLETE, null)
    }

    /**
     * 缓冲更新
     */
    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {}
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        onHandlerCallback?.invoke(HANDLER_ERROR, null)
        return false
    }

    val duration: Long
        get() = mMediaPlayer?.duration?.toLong() ?: 0

    companion object {
        const val HANDLER_CUR_TIME = 1 //当前播放状态时间
        const val HANDLER_PREPARED = 2 //装备好了
        const val HANDLER_COMPLETE = 0 //完成
        const val HANDLER_ERROR = -28 //错误

        /**
         * 获取当前播放时长
         *
         * @return 本地播放时长
         */
        fun getDurationLocation(
            context: Context?,
            uri: Uri?
        ): Long {
            val player =
                create(context, uri)
            return player?.duration?.toLong() ?: 0
        }
    }

    /**
     * 音频播放器
     *
     * @param context 上下文
     * @param handler 音频状态handler
     */
    init {
        try {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC) // 设置媒体流类型
            mMediaPlayer?.setOnBufferingUpdateListener(this)
            mMediaPlayer?.setOnPreparedListener(this)
            mMediaPlayer?.setOnCompletionListener(this)
            mMediaPlayer?.setOnErrorListener(this)
            mAudioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}