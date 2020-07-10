package com.example.arcns.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcns.core.network.DownLoadTask
import com.example.arcns.databinding.LayoutRecyclerviewItemDownloadBinding
import com.example.arcns.viewmodel.ViewModelDownload

class AdapterDownload(val viewModel: ViewModelDownload) :
    RecyclerView.Adapter<AdapterDownloadViewHolder>() {
    private var data: List<DownLoadTask> = arrayListOf()

    init {
        // 表示此适配器将发布唯一值(getItemId)作为数据集中项目的键
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return data.get(position).itemId!!
    }

    fun submitList(data: List<DownLoadTask>?) {
        this.data = data ?: arrayListOf()
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterDownloadViewHolder =
        AdapterDownloadViewHolder(
            LayoutRecyclerviewItemDownloadBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ).apply {
                viewModel = this@AdapterDownload.viewModel
            }
        )


    override fun onBindViewHolder(holder: AdapterDownloadViewHolder, position: Int) =
        holder.bindTo(data.get(position))

    override fun getItemCount(): Int = data.size
}

class AdapterDownloadViewHolder(
    var binding: LayoutRecyclerviewItemDownloadBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(item: DownLoadTask) {
        binding.item = item
    }
}