package com.example.arcns.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.afollestad.materialdialogs.list.listItems
import com.arcns.core.file.cacheDirPath
import com.arcns.core.file.getCurrentTimeMillisFileName
import com.arcns.core.network.*
import com.arcns.core.util.setActionBarAsToolbar
import com.arcns.core.util.EventObserver
import com.arcns.core.util.autoCleared
import com.arcns.core.util.showDialog
import com.example.arcns.R
import com.example.arcns.databinding.FragmentDownloadBinding
import com.example.arcns.databinding.FragmentUploadBinding
import com.example.arcns.viewmodel.*
import kotlinx.android.synthetic.main.fragment_download.*
import kotlinx.android.synthetic.main.fragment_empty.toolbar

/**
 *
 */
class FragmentUpload : Fragment() {
    private var binding by autoCleared<FragmentUploadBinding>()
    private val viewModel by viewModels<ViewModelUpload>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private lateinit var uploadManager: UploadManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUploadBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@FragmentUpload
            viewModel = this@FragmentUpload.viewModel
        }
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarAsToolbar(toolbar)
        setupResult()
    }

    private fun setupResult() {
        viewModel.toast.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        })
        uploadManager = UploadManager(viewLifecycleOwner, viewModel.uploadManagerData)
//        downLoadManager = DownLoadManager(viewModel.downloadManagerData)
//        downloadManager = DownloadManager()
        uploadManager.managerData.notificationOptions = DownloadNotificationOptions(
            smallIcon = R.drawable.ic_download,
            autoCancelOnState = arrayListOf(TaskState.Wait, TaskState.Success)
//            defaultIsOngoing = false

        )
//        viewModel.eventTaskClick.observe(viewLifecycleOwner, EventObserver { task ->
//            showDialog {
//                listItems(
//                    items = listOf("暂停", "取消", "重新下载", "断点续传", "删除")
//                ) { _, index, _ ->
//                    when (index) {
//                        0 -> task.pause()
//                        1 -> task.cancel()
//                        2 -> viewModel.downloadManagerData.download(task.apply {
//                            isBreakpointResume = false
//                        })
//                        3 -> viewModel.downloadManagerData.download(task.apply {
//                            isBreakpointResume = true
//                        })
//                        4 -> viewModel.downloadManagerData.removeTask(task.apply {
//
//                        })
//                    }
//                }
//            }
//        })
    }

//    override fun onDestroyView() {
//        downloadManager.releaseManagerData()
//        super.onDestroyView()
//    }
}
