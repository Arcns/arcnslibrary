package com.arcns.core.media.audio

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.customview.customView
import com.arcns.core.databinding.MediaAudioRecorderPlayerLayoutDefaultBinding
import com.arcns.core.util.LOG
import com.arcns.core.util.showDialog

class MediaAudioRecorderPlayerUtil {
    var viewModel: MediaAudioRecorderPlayerViewModel
    private var activity: AppCompatActivity? = null
    private var fragment: Fragment? = null
    val lifecycleOwner: LifecycleOwner? get() = activity ?: fragment
    val context: Context? get() = activity ?: fragment?.context

    var binding: MediaAudioRecorderPlayerLayoutDefaultBinding? = null

    constructor(activity: AppCompatActivity) {
        this.activity = activity
        viewModel = ViewModelProvider(activity).get(MediaAudioRecorderPlayerViewModel::class.java)
    }

    constructor(fragment: Fragment) {
        this.fragment = fragment
        viewModel = ViewModelProvider(fragment).get(MediaAudioRecorderPlayerViewModel::class.java)
    }


    fun open() {
        binding =
            MediaAudioRecorderPlayerLayoutDefaultBinding.inflate(LayoutInflater.from(context))
                .apply {
                    lifecycleOwner = this@MediaAudioRecorderPlayerUtil.lifecycleOwner
                    viewModel = this@MediaAudioRecorderPlayerUtil.viewModel
                }
        context?.showDialog {
            customView(view = binding?.root)
            noAutoDismiss()
            cancelable(false)
            cancelOnTouchOutside(false)
            onShow {
                binding?.waveLineView?.startAnim()
            }
            onDismiss {
                LOG("销毁")
                binding?.waveLineView?.release()
            }
            binding?.ivLeft?.setOnClickListener {
                dismiss()
            }
        }
    }
}