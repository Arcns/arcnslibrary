package com.example.arcns.ui

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.arcns.core.app.NotificationOptions
import com.arcns.core.file.FileUtil
import com.arcns.core.file.cacheDirPath
import com.arcns.core.file.getCurrentDateTimeFileName
import com.arcns.core.network.*
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException


/**
 *
 */
class FragmentMain : Fragment() {
    private var binding by autoCleared<FragmentMainBinding>()
    private val viewModel by viewModels<ViewModelMain>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
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
        val uploadManager = UploadManager()
        var task: UploadTask? = null
        btnUploadTest.setOnClickListener {

//            val fileName1 = "test.mp4"
            val fileName1 = getCurrentDateTimeFileName(".mp4")
            val file1 = File(cacheDirPath + File.separator + fileName1)
            if (!file1.exists()) {
                FileUtil.copyFile(requireActivity().assets.open("test.mp4"), cacheDirPath, fileName1)
                LOG("UploadTest source1 copy ok " + file1.length())
            } else {
                LOG("UploadTest source1 file exists " + file1.length())
            }

            val fileName2 = "test.jpg"
            val file2 = File(cacheDirPath + File.separator + fileName2)
            if (!file2.exists()) {
                FileUtil.copyFile(requireActivity().assets.open(fileName2), cacheDirPath, fileName2)
                LOG("UploadTest source2 copy ok " + file2.length())
            } else {
                LOG("UploadTest source2 file exists " + file2.length())
            }

            task = UploadTask(
                url = "https://api.imgur.com/3/upload",
                parameters = arrayListOf(
                    UploadTaskFileParameter(
                        name = "video",
                        fileName = "test.mp4",
                        filePath = file1.absolutePath
                    )
                ),
                onCustomRequest = { task, requestBuilder ->
                    requestBuilder.addHeader("Authorization", "Client-ID {{16070e7eb7aa4d6}}")
                },
                notificationOptions = UploadNotificationOptions(
                    smallIcon = R.drawable.ic_download
                )
            )
            uploadManager.upLoad(task!!)

            return@setOnClickListener

            val client = OkHttpClient().newBuilder()
                .build()
            val mediaType = "text/plain".toMediaTypeOrNull()
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    fileName2,
                    file2.asRequestBody()
                )
//                .addFormDataPart(
//                    "video",
//                    fileName1,
//                    file1.asRequestBody()
//                )
                .build()
            val request = Request.Builder()
                .url("https://api.imgur.com/3/upload")
                .method("POST", body)
                .addHeader("Authorization", "Client-ID {{16070e7eb7aa4d6}}")
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LOG("UploadTest error " + e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    LOG("UploadTest " + response.isSuccessful + "  " + response.body?.string())
                }

            })

        }
        btnUploadTest2.setOnClickListener {
            task?.pause()
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