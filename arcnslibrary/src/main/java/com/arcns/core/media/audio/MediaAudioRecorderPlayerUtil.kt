package com.arcns.core.media.audio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.arcns.core.R
import com.arcns.core.databinding.MediaAudioRecorderPlayerLayoutDefaultBinding
import com.arcns.core.util.EventObserver
import com.arcns.core.util.showDialog


class MediaAudioRecorderPlayerUtil {

    // 宿主
    private var activity: AppCompatActivity? = null
    private var fragment: Fragment? = null

    // 生命周期
    private val lifecycleOwner: LifecycleOwner? get() = activity ?: fragment
    private val context: Context? get() = activity ?: fragment?.context

    // 播放器弹窗、viewmodel和binding
    var viewModel: MediaAudioRecorderPlayerViewModel
    private var binding: MediaAudioRecorderPlayerLayoutDefaultBinding? = null
    private var dialog: MaterialDialog? = null

    constructor(activity: AppCompatActivity) {
        this.activity = activity
        viewModel = ViewModelProvider(activity).get(MediaAudioRecorderPlayerViewModel::class.java)
        setupEvent()
    }

    constructor(fragment: Fragment) {
        this.fragment = fragment
        viewModel = ViewModelProvider(fragment).get(MediaAudioRecorderPlayerViewModel::class.java)
        setupEvent()
    }

    /**
     * 初始化绑定事件
     */
    private fun setupEvent() {
        viewModel.eventClose.removeObservers(lifecycleOwner ?: return)
        viewModel.eventClose.observe(lifecycleOwner ?: return, EventObserver {
            binding?.waveLineView?.release()
            binding = null
            dialog?.dismiss()
            dialog = null
        })
        viewModel.eventWaveLineAnim.removeObservers(lifecycleOwner ?: return)
        viewModel.eventWaveLineAnim.observe(lifecycleOwner ?: return, EventObserver {
            if (it) binding?.waveLineView?.startAnim()
            else binding?.waveLineView?.stopAnim()
        })
    }

    /**
     * 打开录音机
     */
    fun openRecorder(
        isDefaultAutoRecorder: Boolean = false,
        maxRecorderSeconds: Long = 3600,
        completed: EventObserver<String>
    ) {
        if (dialog != null || binding != null) {
            return
        }
        openDialog()
        viewModel.setupRecorder(isDefaultAutoRecorder, maxRecorderSeconds)
        viewModel.eventRecorderCompleted.removeObservers(lifecycleOwner ?: return)
        viewModel.eventRecorderCompleted.observe(lifecycleOwner ?: return, completed)
    }

    /**
     * 打开播放器
     */
    fun openPlayer(isDefaultAutoPlayer: Boolean = false, path: String) {
        if (dialog != null || binding != null) {
            return
        }
        openDialog()
        viewModel.setupPlayer(isDefaultAutoPlayer, path)
    }

    /**
     * 打开弹出框
     */
    fun openDialog() {
        if (dialog != null || binding != null) {
            return
        }
        binding =
            MediaAudioRecorderPlayerLayoutDefaultBinding.inflate(LayoutInflater.from(context))
                .apply {
                    lifecycleOwner = this@MediaAudioRecorderPlayerUtil.lifecycleOwner
                    viewModel = this@MediaAudioRecorderPlayerUtil.viewModel
                }
        dialog = context?.showDialog {
            customView(view = binding?.root)
            noAutoDismiss()
            cancelable(false)
            cancelOnTouchOutside(false)
        }
    }

    fun closeDialog() {
        viewModel.onClose()
    }
}

/**
 * 设置中间按钮
 */
@BindingAdapter(
    value = ["bindMediaAudioRecorderStateToCenter", "bindMediaAudioPlayerStateToCenter"],
    requireAll = true
)
fun bindMediaAudioStateToCenter(
    view: ImageView,
    recorderState: MediaAudioRecorderState?,
    playerState: MediaAudioPlayerState?
) {
    var stateIcon = when (recorderState) {
        // 录制中、录制暂停时，显示停止录制按钮
        MediaAudioRecorderState.Recording, MediaAudioRecorderState.RecordingPause -> R.drawable.media_audio_recorder_player_ic_finish_record
        // 准备录制、录制错误时，显示开始录制按钮
        MediaAudioRecorderState.Ready, MediaAudioRecorderState.RecordingFailed -> R.drawable.media_audio_recorder_player_ic_start_record
        else -> when (playerState) {
            // 准备播放、播放错误、暂停播放时，显示播放按钮
            MediaAudioPlayerState.Ready, MediaAudioPlayerState.PlayingFailed, MediaAudioPlayerState.PlayingPause -> R.drawable.media_audio_recorder_player_ic_play
            // 播放中时，显示暂停播放按钮
            MediaAudioPlayerState.Playing -> R.drawable.media_audio_recorder_player_ic_pause
            else -> null
        }
    }
    if (stateIcon != null) {
        view.setImageResource(stateIcon)
        view.visibility = View.VISIBLE
    } else {
        view.setImageDrawable(null)
        view.visibility = View.INVISIBLE
    }
}

/**
 * 设置右边按钮
 */
@BindingAdapter(
    value = ["bindMediaAudioRecorderStateToRight", "bindMediaAudioPlayerStateToRight"],
    requireAll = true
)
fun bindMediaAudioStateToRight(
    view: ImageView,
    recorderState: MediaAudioRecorderState?,
    playerState: MediaAudioPlayerState?
) {
    var stateIcon = when (recorderState) {
        // 录制中，显示暂停录制按钮
        MediaAudioRecorderState.Recording -> R.drawable.media_audio_recorder_player_ic_pause
        // 暂停录制时，显示继续录制按钮
        MediaAudioRecorderState.RecordingPause -> R.drawable.media_audio_recorder_player_ic_play
        // 完成录制时，显示提交按钮
        MediaAudioRecorderState.RecordingFinish -> R.drawable.media_audio_recorder_player_ic_submit
        else -> null
    }
    if (stateIcon != null) {
        view.setImageResource(stateIcon)
        view.visibility = View.VISIBLE
    } else {
        view.setImageDrawable(null)
        view.visibility = View.INVISIBLE
    }
}