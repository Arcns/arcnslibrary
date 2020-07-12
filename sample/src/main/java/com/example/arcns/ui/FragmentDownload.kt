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
import com.arcns.core.network.DownloadTask
import com.arcns.core.network.DownloadManager
import com.arcns.core.network.DownloadNotificationOptions
import com.arcns.core.util.setActionBarAsToolbar
import com.arcns.core.util.EventObserver
import com.arcns.core.util.autoCleared
import com.arcns.core.util.showDialog
import com.example.arcns.R
import com.example.arcns.databinding.FragmentDownloadBinding
import com.example.arcns.viewmodel.*
import kotlinx.android.synthetic.main.fragment_download.*
import kotlinx.android.synthetic.main.fragment_empty.toolbar

/**
 *
 */
class FragmentDownload : Fragment() {
    private var binding by autoCleared<FragmentDownloadBinding>()
    private val viewModel by viewModels<ViewModelDownload>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private lateinit var downloadManager: com.arcns.core.network.DownloadManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDownloadBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@FragmentDownload
            viewModel = this@FragmentDownload.viewModel
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
        downloadManager = DownloadManager(viewLifecycleOwner, viewModel.downloadManagerData)
//        downLoadManager = DownLoadManager(viewModel.downloadManagerData)
//        downloadManager = DownloadManager()
        downloadManager.managerData.notificationOptions = DownloadNotificationOptions(
            smallIcon = R.drawable.ic_download//,
//            defaultIsOngoing = false
        )
        viewModel.eventTaskClick.observe(viewLifecycleOwner, EventObserver { task ->
            showDialog {
                listItems(
                    items = listOf("暂停", "取消", "重新下载", "断点续传", "删除")
                ) { _, index, _ ->
                    when (index) {
                        0 -> task.pause()
                        1 -> task.cancel()
                        2 -> viewModel.downloadManagerData.download(task.apply {
                            isBreakpointResume = false
                        })
                        3 -> viewModel.downloadManagerData.download(task.apply {
                            isBreakpointResume = true
                        })
                        4 -> viewModel.downloadManagerData.removeTask(task.apply {

                        })
                    }
                }
            }
        })
        btnAddTask1.setOnClickListener {
            downloadManager.download(
                DownloadTask(
//                    url = "https://codeload.github.com/afollestad/material-dialogs/zip/3.3.0",
//                url = "https://codeload.github.com/Arcns/arcnslibrary/tar.gz/0.1.20-3",
//                url = "http://rtu.earth123.net:10008/uploads/apk/202007061123414530346.apk",
//                url = "https://dldir1.qq.com/weixin/android/weixin7016android1700_arm64.apk",
//                    url="http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4",
                    url = "https://6c0fee503ddb187fc6bd1ce48124b314.dd.cdntips.com/imtt.dd.qq.com/16891/apk/B63F493587B17E6AD41B8E6844E6CE99.apk?mkey=5f069da3b7ed4490&f=1806&cip=183.237.98.101&proto=https",
                    saveDirPath = cacheDirPath,
                    saveFileSuffix = ".dk",
                    isBreakpointResume = false
                )
            )
        }
    }

//    override fun onDestroyView() {
//        downloadManager.releaseManagerData()
//        super.onDestroyView()
//    }
}

data class EDownloadItem(
    var title: String,
    var type: String
)