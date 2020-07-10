package com.example.arcns.adapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcns.core.network.DownloadTask
import com.example.arcns.viewmodel.ViewModelDownload


@BindingAdapter(
    value = [
        "bindDownloadViewModel",
        "bindDownload"
    ],
    requireAll = true
)
fun RecyclerView.bindDownload(viewModel: ViewModelDownload, data: List<DownloadTask>?) {
    if (adapter == null) {
        adapter = AdapterDownload(viewModel)
    }
    (adapter as? AdapterDownload)?.submitList(data ?: listOf())
}
