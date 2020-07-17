package com.example.arcns.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arcns.core.network.DownloadTask
import com.arcns.core.network.UploadTask
import com.example.arcns.databinding.LayoutRecyclerviewItemDownloadBinding
import com.example.arcns.databinding.LayoutRecyclerviewItemUploadBinding
import com.example.arcns.viewmodel.ViewModelDownload
import com.example.arcns.viewmodel.ViewModelUpload

class AdapterUpload(val viewModel: ViewModelUpload) :
    RecyclerView.Adapter<AdapterUploadViewHolder>() {
    private var data: List<UploadTask> = arrayListOf()

    init {
        // 表示此适配器将发布唯一值(getItemId)作为数据集中项目的键
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return data.get(position).itemId!!
    }

    fun submitList(data: List<UploadTask>?) {
        this.data = data ?: arrayListOf()
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterUploadViewHolder =
        AdapterUploadViewHolder(
            LayoutRecyclerviewItemUploadBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ).apply {
                viewModel = this@AdapterUpload.viewModel
            }
        )


    override fun onBindViewHolder(holder: AdapterUploadViewHolder, position: Int) =
        holder.bindTo(data.get(position))

    override fun getItemCount(): Int = data.size
}

class AdapterUploadViewHolder(
    var binding: LayoutRecyclerviewItemUploadBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(item: UploadTask) {
        binding.item = item
    }
}