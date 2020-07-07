package com.example.arcns

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.arcns.core.APP
import com.arcns.core.app.*
import com.arcns.core.map.*
import com.arcns.core.util.*
import com.arcns.map.gaode.GaodeMapLocator
import com.example.arcns.databinding.ActivityMainBinding
import com.example.arcns.event.EventMainActivity
import com.example.arcns.event.EventMainActivityType
import com.example.arcns.viewmodel.ViewModelActivityMain
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: ViewModelActivityMain by viewModels()
    private lateinit var navController: NavController

    // 蓝牙开关广播接受者
    private val bluetoothIntentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                when (state) {
                    BluetoothAdapter.STATE_ON, BluetoothAdapter.STATE_OFF -> viewModel.setEventBluetoothState(
                        state
                    )
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        navController = findNavController(R.id.navHostFragment)

        setupStatus()
        setupBnvMenu()
        setupActionBar()
        setupResult()
        setupBluetoothBroadcastReceiver()

        LOG("test:" + (6.4555555).keepDecimalPlaces(3))
        LOG("test:" + (6.4555555).keepDecimalPlaces(3, false))
        LOG("test:" + "xf".zeroPadding(4))
        LOG("test:" + "5xf".zeroPadding(4))
        LOG("test:" + "15xf".zeroPadding(4))
        LOG("test:32ac - " + calculatedCRC16ToHex("0X35=ÿ,"))
        LOG("test:2311 - " + calculatedCRC16ToHex("FFFF=msg_,"))
        LOG("test:7657 - " + calculatedCRC16ToHex("TIME= 20200624173828,TEMP= 29.4,"))


        // 定位器服务
        setMapLocatorServiceDefaultOptions(
            ForegroundServiceOptions(
                onCreateServiceContent = {
                    GaodeMapLocator(it).apply {
                        addTrackRecorder(viewModel.mapTrackRecorder)
                        start()
                    }
                },
                notificationOptions = ForegroundServiceNotificationOptions(
                    smallIcon = R.mipmap.ic_launcher
                ),
                onDestroyServiceContent = {
                    LOG("ForegroundService Service Content onDestroy")
                    it?.onDestroy()
                }
            )
        )
        startMapLocatorService(
            serviceConnection = object : ForegroundServiceConnection<MapLocator>() {
                override fun onServiceConnected(
                    binder: ForegroundServiceBinder<MapLocator>,
                    serviceContent: MapLocator?
                ) {
                    LOG("ForegroundService onServiceConnected")
//                    serviceContent?.addTrackRecorder(viewModel.mapTrackRecorder)
//                    serviceContent?.start()
                    Handler().postDelayed({

//                        binder.service.updateNotification {
//                            it.setProgress(100, 50, false)
//                            it.setContentText("test")
//                        }

                        LOG("ForegroundService stopService")
                    }, 5000)
                    Handler().postDelayed({

                        binder.stopService()

                        LOG("ForegroundService stopService")
                    }, 15000)
                }
            }
        )

    }

    private fun setupBluetoothBroadcastReceiver() =
        registerReceiver(bluetoothBroadcastReceiver, bluetoothIntentFilter)

    private fun setupActionBar() {

    }

    /**
     * 配置状态栏透明
     */
    private fun setupStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    /**
     * 配置底部菜单栏
     */
    private fun setupBnvMenu() {
        // 覆盖并自行实现NavigationUI.setupWithNavController的BottomNavigationView选中事件，避免每次点击都重新创建Fragment
//        NavigationUI.setupWithNavController(bnvMenu, navController)
//        bnvMenu.setOnNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.fragmentMain -> navController.popBackStack(R.id.fragmentMain, false)
//                R.id.fragmentUserCenter -> navController.navigate(R.id.fragmentUserCenter)
//            }
//            true;
//        }
//        // 控制ActionBar和BottomNavigationView的隐藏与显示
//        navController.addOnDestinationChangedListener { controller, destination, arguments ->
//            // 检查更新
//            viewModel.onCheckUpdate()
//            // 关闭键盘
//            hideSoftInputFromWindow()
//            when (destination.id) {
//                R.id.fragmentStartupPage -> {
//                    viewModel.onMainMenuHide(false)
//                }
//                R.id.fragmentMain, R.id.fragmentUserCenter -> {
//                    viewModel.onMainMenuShow()
//                }
//                else -> {
//                    viewModel.onMainMenuHide()
//                }
//            }
//        }
    }

    private fun setupResult() {
        viewModel.toast.observe(this, EventObserver {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        })
        // 监测到新版本回调
        viewModel.checkNewVersionUpdate.observe(this,
            EventObserver {
                showDialog {
                    title(text = "检测到新版本")
                    message(text = "检测到新版本，请立即更新！")
                    positiveButton(text = "立即下载更新") { dialog ->
                        Toast.makeText(
                            this@MainActivity,
                            "正在下载..",
                            Toast.LENGTH_LONG
                        ).show()
                        DownloadService.startService(
                            this@MainActivity,
                            DownloadTask(
                                downloadUrl = it,
                                downloadTitle = getString(
                                    R.string.app_name
                                ),
                                downloadDescription = "正在下载新版本",
                                downloadSaveNameSuffix = ".apk",
                                isAutoOpen = true
                            )
                        )
                        dialog.dismiss()
                        showDialog {
                            message(text = "正在下载新版本..")
                            cancelable(false)
                            cancelOnTouchOutside(false)
                            noAutoDismiss()
                        }

                    }
                    // 强制更新
                    cancelable(false)
                    cancelOnTouchOutside(false)
                    noAutoDismiss()
                    negativeButton(text = "关闭应用") {
                        finish()
                    }
                }
            })
    }

    /**
     * 配置返回按键响应
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        // 检查更新
        viewModel.onCheckUpdate()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(bluetoothBroadcastReceiver)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    /**
     * 全局事件监听
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventMainActivity) {
        when (event.type) {
            EventMainActivityType.onDataInterfaceReLogin -> {
            }
            EventMainActivityType.onDataInterfaceUpdateCurrentUser -> {
            }
        }
    }
}
