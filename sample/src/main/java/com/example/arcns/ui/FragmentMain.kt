package com.example.arcns.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.arcns.core.APP
import com.arcns.core.file.FileUtil
import com.arcns.core.file.cacheDirPath
import com.arcns.core.media.selector.*
import com.arcns.core.util.*
import com.arcns.media.audio.MediaAudioRecorderPlayerUtil
import com.example.arcns.R
import com.example.arcns.databinding.FragmentMainBinding
import com.example.arcns.util.openPermission
import com.example.arcns.viewmodel.ViewModelActivityMain
import com.example.arcns.viewmodel.ViewModelMain
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.fragment_empty.toolbar
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File

/**
 *
 */
class FragmentMain : Fragment() {
    private var binding by autoCleared<FragmentMainBinding>()
    private val viewModel by viewModelsAndInjectSuper<ViewModelMain>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelImageSelector by activityViewModels<MediaSelectorViewModel>()
    private lateinit var audioRecorderPlayerUtil: MediaAudioRecorderPlayerUtil

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@FragmentMain
            viewModel = this@FragmentMain.viewModel
        }
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarAsToolbar(toolbar, isTopLevelDestination = true)

        setupResult()

        setupOnBackPressed {
            // 设置返回时退到后台
//            requireActivity().finish()
            requireActivity().moveTaskToBack(true)
        }

//        Handler().postDelayed({
//            activity?.finish()
//        },2000)


//        val clz = ViewModelMain::class
//        clz.declaredMemberProperties.forEach {
//            val superViewModel = it.findAnnotation<InjectSuperViewModel>()
//            if (superViewModel != null && it is KMutableProperty<*>) {
//                it.setter.call(viewModel, viewModelActivityMain)
//                LOG("viewModelActivityMain:" + viewModel.superViewModel)
//            }
//        }

        LOG("viewModelActivityMain:" + viewModel.superViewModel)
        viewModel.superViewModel.test = 99

    }

    private fun setupResult() {
        audioRecorderPlayerUtil = MediaAudioRecorderPlayerUtil(activity = requireActivity())
        viewModel.toast.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        })
        viewModel.eventOpenBluetoothAndPermission.observe(
            viewLifecycleOwner,
            EventObserver { isBlueEnable ->
                if (isBlueEnable) {
                    openBluetoothAndPermission()
                    return@EventObserver
                }
                requireActivity().showDialog {
                    message(text = "使用本应用需要开启蓝牙功能，当前手机蓝牙处于关闭状态，是否立即开启蓝牙？")
                    positiveButton(text = "立即开启蓝牙") {
                        openBluetoothAndPermission()
                    }
                    negativeButton(text = "取消")
                }
            })
        viewModelActivityMain.eventBluetoothState.observe(viewLifecycleOwner, EventObserver {
            viewModel.startBluetooth()
        })
        //
        btnGoMapGaode.setOnClickListener {
            findNavController().navigate(FragmentMainDirections.actionFragmentMainToFragmentMapGaode())
        }
        btnGoMapBaidu.setOnClickListener {
            findNavController().navigate(FragmentMainDirections.actionFragmentMainToFragmentMapBaidu())
        }
        btnAudioTest.setOnClickListener {
            AndPermission.with(this)
                .runtime()
                .permission(Permission.RECORD_AUDIO)
                .onGranted {
                    audioRecorderPlayerUtil.openRecorder(
                        isDefaultAutoRecorder = true,
                        completed = EventObserver {
                        })
                }.onDenied {
                    Toast.makeText(
                        context,
                        "录音功能需要使用麦克风权限，请您允许应用使用相应权限",
                        Toast.LENGTH_LONG
                    ).show()
                }.start()
        }
        btnDownloadTest.setOnClickListener {
            findNavController().navigate(FragmentMainDirections.actionFragmentMainToFragmentDownload())
        }
        btnUploadTest.setOnClickListener {
            findNavController().navigate(FragmentMainDirections.actionFragmentMainToFragmentUpload())
        }
        btnMediaSelector1.setOnClickListener {
            AndPermission.with(activity)
                .runtime()
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .onGranted {
                    context ?: return@onGranted
                    viewModelImageSelector.setupMediaSelector(
//                        saveAsOption = viewModel.imageSaveAsOption
//                    ,
//                    setupFromMediaStoreMediaQuerys = arrayOf(
//                        EMediaQuery(
//                            queryContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                        ),
//                        EMediaQuery(
//                            queryContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//                            querySelection = "${MediaStore.Files.FileColumns.SIZE}<145049664"
//                        )
//                    )
                    )
//                    NavMainDirections.actionGlobalNavigation()
                    navigationDefaultMediaSelector(
                        MediaSelectorNavigationConfig(
                            R.id.action_global_mediaSelectorFragment,
                            R.id.action_global_mediaSelectorDetailsFragment
                        ),
                        MediaSelectorNavigationTitleConfig(
                            title = "选择图片",
                            isCenter = true
                        )
                    )
                }.onDenied {
                    Toast.makeText(
                        context ?: return@onDenied,
                        "无权限",
                        Toast.LENGTH_LONG
                    ).show()
                }.start()
        }
        btnMediaSelector2.setOnClickListener {
            val fileName2 = "test.jpg"
            val file2 = File(cacheDirPath + File.separator + fileName2)
            if (!file2.exists()) {
                FileUtil.copyFile(APP.INSTANCE.assets.open(fileName2), cacheDirPath, fileName2)
            }
            viewModelImageSelector.setupMediaSelector(
                isSelector = false,
                isSetupFromMediaStore = false,
                initMedias = listOf(file2.absolutePath),
                currentMedia = file2.absolutePath,
                isOnlyPreview = true
            )
            navigationDefaultMediaSelectorDetails(
                R.id.action_global_mediaSelectorDetailsFragment,
                navigationTitleConfig = MediaSelectorNavigationTitleConfig(
                    isCenter = true
                )
            )
        }
    }

    private fun openBluetoothAndPermission() {
        var permissions = arrayOf(
            Permission.ACCESS_FINE_LOCATION,
            Permission.ACCESS_COARSE_LOCATION
        )
        if (AndPermission.hasPermissions(
                context,
                permissions
            )
        ) {
            viewModel.enableBluetooth()
            return
        }
        requireActivity().showDialog {
            message(text = "使用本应用需要您授予【位置】权限，用于搜索附近的蓝牙设备。如果您不同意授予以上权限，将导致本应用的功能无法正常使用。")
            positiveButton(text = "去授予权限") {
                openPermission(permissions, "权限获取失败，请授予本应用使用【位置】权限，用于搜索附近的蓝牙设备") {
                    viewModel.enableBluetooth()
                }
            }
            negativeButton(text = "取消")
        }
    }


}