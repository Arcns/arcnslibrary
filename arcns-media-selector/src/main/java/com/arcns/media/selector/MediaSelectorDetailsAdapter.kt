package com.arcns.media.selector

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
import com.arcns.core.file.mimeType
import com.arcns.core.util.openAppByPath
import com.arcns.core.util.openAppByUri
import com.arcns.media.selector.databinding.MediaSelectorLayoutRecyclerviewItemDetailsBinding
import com.arcns.media.selector.databinding.MediaSelectorLayoutRecyclerviewItemDetailsNoPreviewBinding


class MediaSelectorDetailsAdapter(val viewModel: MediaSelectorViewModel) :
    ListAdapter<EMedia, RecyclerView.ViewHolder>(
        diffCallback
    ) {

    val HAS_PREVIEW = 1
    val NO_PREVIEW = 2

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isImage || getItem(position).isVideo) HAS_PREVIEW else NO_PREVIEW


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder = when (viewType) {
        HAS_PREVIEW -> MediaSelectorDetailsViewHolder(
            MediaSelectorLayoutRecyclerviewItemDetailsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ).apply {
                viewModel = this@MediaSelectorDetailsAdapter.viewModel
            }
        )
        else -> MediaSelectorDetailsNoPreviewViewHolder(
            MediaSelectorLayoutRecyclerviewItemDetailsNoPreviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ).apply {
                viewModel = this@MediaSelectorDetailsAdapter.viewModel
            }
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MediaSelectorDetailsViewHolder -> holder.bindTo(getItem(position))
            is MediaSelectorDetailsNoPreviewViewHolder -> holder.bindTo(getItem(position))
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

class MediaSelectorDetailsNoPreviewViewHolder(
    var binding: MediaSelectorLayoutRecyclerviewItemDetailsNoPreviewBinding
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
    currentMedia: EMedia?,
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
@BindingAdapter(
    value = [
        "bindFileClickOpenApp",
        "bindFileClickOpenAppViewModel"
    ],
    requireAll = true
)
fun bindFileClickOpenApp(
    view: View,
    currentMedia: EMedia,
    viewModel: MediaSelectorViewModel
) {
    view.setOnClickListener {
        if (viewModel.onDetailsFileClickOpenApp?.invoke(currentMedia) != true) {
            when (currentMedia.value) {
                is Uri -> view.context.openAppByUri(
                    currentMedia.value,
                    currentMedia.mimeType ?: currentMedia.value.mimeType
                )
                is String -> view.context.openAppByPath(
                    currentMedia.value,
                    currentMedia.mimeType ?: currentMedia.value.mimeType,
                    APP.fileProviderAuthority ?: return@setOnClickListener
                )
            }
        }
    }
}