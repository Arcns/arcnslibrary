package com.arcns.media.selector

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcns.media.selector.databinding.MediaSelectorLayoutRecyclerviewItemBinding


class MediaSelectorAdapter(val viewModel: MediaSelectorViewModel) :
    ListAdapter<EMedia, MediaSelectorViewHolder>(
        diffCallback
    ) {

    private var selectedBinding = ArrayList<MediaSelectorLayoutRecyclerviewItemBinding>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaSelectorViewHolder =
        MediaSelectorViewHolder(
            MediaSelectorLayoutRecyclerviewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ).apply {
                viewModel = this@MediaSelectorAdapter.viewModel
            }
        )

    override fun onBindViewHolder(holder: MediaSelectorViewHolder, position: Int) =
        holder.bindTo(getItem(position)) { item, isSelected ->
            // 更新记录选中视图的列表
            if (isSelected) {
                if (!selectedBinding.contains(holder.binding)) {
                    selectedBinding.add(holder.binding)
                }
            } else {
                selectedBinding.remove(holder.binding)
                // 更新其他选中的媒体文件
                selectedBinding.forEach {
                    it.item = it.item
                }
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

class MediaSelectorViewHolder(
    var binding: MediaSelectorLayoutRecyclerviewItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(item: EMedia, updateSelectedMedia: (EMedia, Boolean) -> Unit) {
        binding.item = item
        if (binding.viewModel?.isSelectedMedia(item) == true) {
            // 更新记录选中视图的列表（因为当从其他页面返回时，adapter的选中视图列表会重新创建）
            updateSelectedMedia(item, true)
        }
        binding.tvToggleSelectedMedia?.setOnClickListener {
            // 切换选中/取消取消
            val isSelected =
                binding.viewModel?.onToggleSelectedMedia(item) ?: return@setOnClickListener
            // 更新当前媒体文件
            binding.item = item
            // 更新记录选中视图的列表
            updateSelectedMedia(item, isSelected)
        }
    }
}

/**
 * 为媒体文件选择器列表绑定数据
 */
@BindingAdapter(
    value = [
        "bindMediaSelectorData",
        "bindMediaSelectorViewModel"
    ],
    requireAll = true
)
fun bindMediaSelector(
    recyclerView: RecyclerView,
    data: List<EMedia>?,
    viewModel: MediaSelectorViewModel
) {
    if (recyclerView.adapter == null) {
        recyclerView.adapter = MediaSelectorAdapter(viewModel)
    }
    (recyclerView.adapter as? MediaSelectorAdapter)?.submitList(data ?: ArrayList())
}

/**
 * 为媒体文件文件夹选择器绑定数据
 */
@BindingAdapter(
    value = [
        "bindMediaFolderSelectorData",
        "bindMediaFolderSelectorViewModel"
    ],
    requireAll = true
)
fun bindMediaFolderSelector(
    spinner: AppCompatSpinner,
    data: List<String>?,
    viewModel: MediaSelectorViewModel
) {
    spinner.adapter = ArrayAdapter(
        spinner.context,
        R.layout.media_selector_layout_spinner_item_media_folder_selector,
        data ?: return
    ).apply {
        setDropDownViewResource(
            R.layout.media_selector_layout_spinner_item_media_folder_selector_drop_down
        )
    }
    spinner.onItemSelectedListener = object :
        AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
        }
    }
}