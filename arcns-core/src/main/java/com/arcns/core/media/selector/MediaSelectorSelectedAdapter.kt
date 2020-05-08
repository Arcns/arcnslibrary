package com.arcns.core.media.selector

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.*
import com.arcns.core.databinding.MediaSelectorLayoutRecyclerviewItemSelectedBinding
import com.arcns.core.databinding.MediaSelectorLayoutRecyclerviewItemSelectedVideoOrAudioBinding
import com.arcns.core.util.LOG


class MediaSelectorSelectedAdapter(val viewModel: MediaSelectorViewModel) :
    ListAdapter<ESelectedMedia, RecyclerView.ViewHolder>(
        diffCallback
    ) {

    private val IMAGE = 1
    private val VIDEO = 2
    private val AUDIO = 3

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).media.isImage) IMAGE else if (getItem(position).media.isVideo) VIDEO else AUDIO

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder =
        if (viewType == IMAGE)
            MediaSelectorSelectedViewHolder(
                MediaSelectorLayoutRecyclerviewItemSelectedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ).apply {
                    viewModel = this@MediaSelectorSelectedAdapter.viewModel
                }
            )
        else
            MediaSelectorSelectedVideoOrAudioViewHolder(
                MediaSelectorLayoutRecyclerviewItemSelectedVideoOrAudioBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ).apply {
                    viewModel = this@MediaSelectorSelectedAdapter.viewModel
                }
            )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (holder) {
            is MediaSelectorSelectedViewHolder -> holder.bindTo(getItem(position))
            is MediaSelectorSelectedVideoOrAudioViewHolder -> holder.bindTo(getItem(position))
            else -> Unit
        }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ESelectedMedia>() {
            override fun areItemsTheSame(
                oldItem: ESelectedMedia,
                newItem: ESelectedMedia
            ): Boolean =
                oldItem.media == newItem.media

            override fun areContentsTheSame(
                oldItem: ESelectedMedia,
                newItem: ESelectedMedia
            ): Boolean =
                oldItem.media == newItem.media && oldItem.isCurrentMedia == newItem.isCurrentMedia
        }
    }
}

class MediaSelectorSelectedViewHolder(
    var binding: MediaSelectorLayoutRecyclerviewItemSelectedBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(item: ESelectedMedia) {
        binding.item = item
    }
}

class MediaSelectorSelectedVideoOrAudioViewHolder(
    var binding: MediaSelectorLayoutRecyclerviewItemSelectedVideoOrAudioBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(item: ESelectedMedia) {
        binding.item = item
    }
}

/**
 * 为媒体文件选择器列表绑定数据
 */
@BindingAdapter(
    value = [
        "bindMediaSelectorDetailsCurrentMedia",
        "bindMediaSelectorSelectedData",
        "bindMediaSelectorSelectedViewModel"
    ],
    requireAll = true
)
fun bindMediaSelectorSelected(
    recyclerView: RecyclerView,
    currentMedia: EMedia,
    data: List<EMedia>?,
    viewModel: MediaSelectorViewModel
) {
    if (recyclerView.adapter == null) {
        recyclerView.adapter = MediaSelectorSelectedAdapter(viewModel)
        // 实现拖动排序
        ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int = makeMovementFlags(
                // 拖动方向
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                // 滑动方向
                0
            )

            override fun isLongPressDragEnabled(): Boolean = true

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                viewModel.onSortingSelectedMedias(
                    fromPosition = viewHolder.adapterPosition,
                    toPosition = target.adapterPosition
                )
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }
        }).attachToRecyclerView(
            recyclerView
        )
    }

    var currentMediaPosition: Int? = null
    (recyclerView.adapter as? MediaSelectorSelectedAdapter)?.submitList(data?.mapIndexed() { position, item ->
        val isCurrentMedia = item == currentMedia
        if (isCurrentMedia) {
            currentMediaPosition = position
        }
        ESelectedMedia(
            media = item,
            isCurrentMedia = isCurrentMedia
        )
    }?.toList() ?: ArrayList())
    // 选中列表跳转到选中媒体文件
    recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            LOG("onGlobalLayout")
            recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            currentMediaPosition?.run {
                val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
                if (this > layoutManager.findLastCompletelyVisibleItemPosition() || this < layoutManager.findFirstCompletelyVisibleItemPosition()) {
                    layoutManager.scrollToPosition(this)
//            layoutManager.stackFromEnd = true
                }
            }
        }

    })

}
