package com.example.arcns.adapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcns.core.network.DownloadTask
import com.arcns.core.network.UploadTask
import com.example.arcns.viewmodel.ViewModelDownload
import com.example.arcns.viewmodel.ViewModelUpload


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



@BindingAdapter(
    value = [
        "bindUploadViewModel",
        "bindUpload"
    ],
    requireAll = true
)
fun RecyclerView.bindUpload(viewModel: ViewModelUpload, data: List<UploadTask>?) {
    if (adapter == null) {
        adapter = AdapterUpload(viewModel)
    }
    (adapter as? AdapterUpload)?.submitList(data ?: listOf())
}
