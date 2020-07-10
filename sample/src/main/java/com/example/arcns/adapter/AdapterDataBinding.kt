package com.example.arcns.adapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcns.core.network.DownLoadTask
import com.arcns.core.util.LOG
import com.example.arcns.viewmodel.ViewModelDownload


@BindingAdapter(
    value = [
        "bindDownloadViewModel",
        "bindDownload"
    ],
    requireAll = true
)
fun RecyclerView.bindDownload(viewModel: ViewModelDownload, data: List<DownLoadTask>?) {
    if (adapter == null) {
        adapter = AdapterDownload(viewModel)
    }
    LOG("RecyclerView.bindDownload")
    (adapter as? AdapterDownload)?.submitList(data ?: listOf())
}
