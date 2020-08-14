package com.arcns.core.media

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.util.*

abstract class MediaAudioBasePlayer {
    // 处理更新定时器
    private var mHandlerUpdateTimer: Timer? = null
    private var mHandlerUpdateTimerTask: TimerTask? = null

    // 处理回调
    private var mHandlerUpdateRate: Int = 10 // 进度回调频率
    private var mHandlerCallback: ((MediaAudoPlayerHandlerType, Int?) -> Unit)? =
        null // 播放进度、状态更新等的处理回调


    /**
     * 设置生命周期感知
     */
    open fun setLifecycleOwner(lifecycleOwner: LifecycleOwner): MediaAudioBasePlayer {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                lifecycleOwner.lifecycle.removeObserver(this)
                this@MediaAudioBasePlayer.onDestroy()
            }
        })
        return this
    }

    /**
     * 开始播放
     */
    abstract fun start(): Boolean

    /**
     * 暂停
     */
    abstract fun pause(): Boolean

    /**
     * 停止
     */
    abstract fun stop()

    /**
     * 跳转
     */
    abstract fun seekTo(msec: Int)


    /**
     * 跳转并播放
     */
    abstract fun seekToAndPlay(msec: Int)

    /**
     * 释放资源
     */
    abstract fun release()

    /**
     * 销毁
     */
    protected open fun onDestroy() {
        release()
        mHandlerCallback = null
    }


    /**
     * 设置处理回调
     */
    fun setHandlerCallback(
        updateRate: Int = 10,
        callback: ((MediaAudoPlayerHandlerType, Int?) -> Unit)?
    ) {
        mHandlerUpdateRate = updateRate
        mHandlerCallback = callback
    }

    /**
     * 提交处理回调
     */
    protected fun submitHandlerCallback(type: MediaAudoPlayerHandlerType, data: Int? = null) {
        mHandlerCallback?.invoke(type, data)
    }

    /**
     * 启动处理更新定时器
     */
    protected fun startHandlerUpdateTimerTask() {
        if (mHandlerUpdateRate <= 0 || mHandlerUpdateTimer != null) return
        mHandlerUpdateTimer = Timer()
        mHandlerUpdateTimerTask = object : TimerTask() {
            override fun run() {
                onHandlerUpdateTimerTaskCallback()
            }
        }
        mHandlerUpdateTimer?.schedule(mHandlerUpdateTimerTask, 0, mHandlerUpdateRate.toLong())
    }

    /**
     * 停止处理更新定时器
     */
    protected fun stopHandlerUpdateTimerTask() {
        if (mHandlerUpdateTimer == null) return
        mHandlerUpdateTimer?.cancel()
        mHandlerUpdateTimer = null
    }

    /**
     * 处理更新定时器回调（运行时定时回调）
     */
    protected abstract fun onHandlerUpdateTimerTaskCallback()
}


/**
 * 播放处理类型
 */
enum class MediaAudoPlayerHandlerType {
    // 播放进度更新
    Update,

    // 播放准备完成
    Prepared,

    // 播放暂停
    Pause,

    // 开始/继续播放
    Play,

    // 播放完成
    Complete,

    // 播放错误
    Error
}