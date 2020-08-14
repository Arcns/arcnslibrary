package com.arcns.core.media

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

abstract class MediaAudioBasePlayer {

    /**
     * 设置生命周期感知
     */
    open fun setLifecycleOwner(lifecycleOwner: LifecycleOwner): MediaAudioBasePlayer {
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
     * 暂停
     */
    abstract fun pause()

    /**
     * 停止
     */
    abstract fun stop()

    /**
     * 跳转并播放
     */
    abstract fun seekToAndPlay()

    /**
     * 释放资源
     */
    abstract fun release()
}