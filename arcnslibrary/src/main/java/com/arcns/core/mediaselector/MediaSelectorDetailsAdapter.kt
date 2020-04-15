package com.arcns.core.mediaselector

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.arcns.core.APP
import com.arcns.core.databinding.MediaSelectorLayoutRecyclerviewItemDetailsBinding
import com.arcns.core.util.openAppByFile
import com.arcns.core.util.openAppByUri
import java.io.File


class MediaSelectorDetailsAdapter(val viewModel: MediaSelectorViewModel) :
    ListAdapter<EMedia, RecyclerView.ViewHolder>(
        diffCallback
    ) {

    val IMAGE = 1

    override fun getItemViewType(position: Int): Int {
        return IMAGE
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder = MediaSelectorDetailsViewHolder(
        MediaSelectorLayoutRecyclerviewItemDetailsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).apply {
            viewModel = this@MediaSelectorDetailsAdapter.viewModel
        }
    )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MediaSelectorDetailsViewHolder -> holder.bindTo(getItem(position))
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<EMedia>() {
            override fun areItemsTheSame(oldItem: EMedia, newItem: EMedia): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: EMedia, newItem: EMedia): Boolean =
                oldItem == newItem
        }
    }
}

class MediaSelectorDetailsViewHolder(
    var binding: MediaSelectorLayoutRecyclerviewItemDetailsBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(item: EMedia) {
        binding.item = item
    }
}

/**
 * 为媒体文件选择器列表绑定数据
 */
@BindingAdapter(
    value = [
        "bindMediaSelectorDetailsData",
        "bindMediaSelectorDetailsViewModel"
    ],
    requireAll = true
)
fun bindMediaSelectorDetails(
    viewPager: ViewPager2,
    data: List<EMedia>?,
    viewModel: MediaSelectorViewModel
) {
    var firstInit = false
    if (viewPager.adapter == null) {
        viewPager.adapter = MediaSelectorDetailsAdapter(viewModel)
        firstInit = true
    }
    (viewPager.adapter as? MediaSelectorDetailsAdapter)?.submitList(data ?: ArrayList())
    // 设置当前打开的媒体文件
    if (firstInit && viewModel.currentMediaPosition != 0) {
        viewPager.setCurrentItem(viewModel.currentMediaPosition, false)
    }
    // 监听viewPager滑动
    viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.setCurrentMediaByPosition(position)
        }
    })
}

/**
 * 设置当前的媒体文件
 */
@BindingAdapter(
    value = [
        "bindMediaSelectorDetailsCurrentMedia",
        "bindMediaSelectorDetailsCurrentMediaViewModel"
    ],
    requireAll = true
)
fun bindMediaSelectorDetailsCurrentMedia(
    viewPager: ViewPager2,
    currentMedia: EMedia,
    viewModel: MediaSelectorViewModel
) {
    // 设置当前打开的媒体文件
    if (viewModel.currentMediaPosition != viewPager.currentItem) {
        viewPager.setCurrentItem(viewModel.currentMediaPosition, false)
    }
}

/**
 * 设置点击控件打开相应媒体文件对应的app
 */
@BindingAdapter("bindVideoOrAudioClickOpenApp")
fun bindVideoOrAudioClickOpenApp(
    view: View,
    currentMedia: EMedia
) {
    view.setOnClickListener {
        when (currentMedia.value) {
            is Uri -> view.context.openAppByUri(
                currentMedia.value,
                currentMedia.mimeTypeIfNullGetOfSuffix
            )
            is String -> view.context.openAppByFile(
                File(currentMedia.value),
                currentMedia.mimeTypeIfNullGetOfSuffix,
                APP.fileProviderAuthority ?: return@setOnClickListener
            )
        }
    }
}