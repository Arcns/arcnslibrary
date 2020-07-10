package com.example.arcns.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.arcns.core.app.ForegroundServiceBinder
import com.arcns.core.app.ForegroundServiceConnection
import com.arcns.core.map.MapLocator
import com.arcns.core.map.startMapLocatorService
import com.arcns.core.network.DownLoadManager
import com.arcns.core.network.DownloadNotificationOptions
import com.arcns.core.util.setActionBarAsToolbar
import com.arcns.core.util.EventObserver
import com.arcns.core.util.autoCleared
import com.example.arcns.R
import com.example.arcns.databinding.FragmentDownloadBinding
import com.example.arcns.databinding.FragmentEmptyBinding
import com.example.arcns.viewmodel.*
import kotlinx.android.synthetic.main.fragment_empty.*

/**
 *
 */
class FragmentDownload : Fragment() {
    private var binding by autoCleared<FragmentDownloadBinding>()
    private val viewModel by viewModels<ViewModelDownload>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private lateinit var downLoadManager: DownLoadManager

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
        downLoadManager = DownLoadManager(viewLifecycleOwner, viewModel.downloadManagerData)
        downLoadManager.notificationOptions = DownloadNotificationOptions(
            smallIcon = R.drawable.ic_download,
            defaultIsOngoing = false
        )
    }
}

data class EDownloadItem(
    var title: String,
    var type: String
)