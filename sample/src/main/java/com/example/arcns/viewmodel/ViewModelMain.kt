package com.example.arcns.viewmodel

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arcns.core.APP
import com.arcns.core.util.Event
import com.arcns.core.util.InjectSuperViewModel
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import com.example.arcns.data.network.NetworkDataSource


class ViewModelMain : ViewModel() {


    // 网络接口
    var networkDataSource: NetworkDataSource = NetworkDataSource()

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 当前状态
    private var _state = MutableLiveData<String>()
    var state: LiveData<String> = _state

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    //
    var drawableLeft = MutableLiveData<String>().apply {
        value = "http://pic.baike.soso.com/p/20110730/bki-20110730162914-1013093665.jpg"
    }

    @InjectSuperViewModel
    lateinit var superViewModel:ViewModelActivityMain

    // 打开蓝牙和权限
    private var _eventOpenBluetoothAndPermission = MutableLiveData<Event<Boolean>>()
    var eventOpenBluetoothAndPermission: LiveData<Event<Boolean>> = _eventOpenBluetoothAndPermission

    init {
        setupBluetooth()
    }

    private fun setupBluetooth() {
        BleManager.getInstance().apply {
            init(APP.INSTANCE)
            enableLog(true)
            setReConnectCount(1, 5000)
            operateTimeout = 5000
        }
//        startBluetooth()
    }

    fun startBluetooth() {
        superViewModel.simplePopupData.startLoading(isDisableTouch = false,loadingDescription = "加载中。。")

        // 设备不支持蓝牙功能
//        if (!BleManager.getInstance().isSupportBle) {
//            _state.value = "当前设备不支持蓝牙功能，无法使用本应用功能"
//            _toast.value = Event("当前设备不支持蓝牙功能，无法使用本应用功能")
//            return
//        }
//        // 打开蓝牙和权限
//        val isBlueEnable = BleManager.getInstance().isBlueEnable
//        if (!isBlueEnable) {
//            _state.value = "当前手机蓝牙处于关闭状态，请开启蓝牙"
//        } else {
//            _state.value = "请授予【位置】权限，用于搜索附近的蓝牙设备"
//        }
//        _eventOpenBluetoothAndPermission.value = Event(isBlueEnable)
    }

    // 打开蓝牙
    fun enableBluetooth() {
        if (BleManager.getInstance().isBlueEnable) {
            scanBluetoothDevice()
            return
        }
        _state.value = "正在开启蓝牙.."
        BleManager.getInstance().enableBluetooth()
    }

    // 扫描蓝牙设备
    fun scanBluetoothDevice() {
        _state.value = "蓝牙已开启，正在搜索附近的设备.."
        // 开始扫描
        BleManager.getInstance().initScanRule(
            BleScanRuleConfig.Builder()
//            .setServiceUuids(serviceUuids) // 只扫描指定的服务的设备，可选
//            .setDeviceName(true, names) // 只扫描指定广播名的设备，可选
//            .setDeviceMac(mac) // 只扫描指定mac的设备，可选
//            .setAutoConnect(false) // 连接时的autoConnect参数，可选，默认false
//            .setScanTimeOut(10000) // 扫描超时时间，可选，默认10秒
                .build()
        )
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                // 返回本次扫描动作是否开启成功
                if (!success) {
                    _state.value = "扫描失败，请尝试重新连接"
                }
            }

            override fun onLeScan(bleDevice: BleDevice) {
                // 扫描过程中所有被扫描到的结果回调，同一个设备可能会出现多次
            }

            override fun onScanning(bleDevice: BleDevice) {
                // 返回扫描过程中的所有过滤后的结果回调，同一个设备只会出现一次
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
                // 扫描结束，返回本次扫描时段内所有被扫描且过滤后的设备集合

            }
        })
    }

    // 连接蓝牙设备
    fun connectBluetoothDevice(bleDevice: BleDevice) {
        _state.value = "正在连接设备.."
        BleManager.getInstance().connect(bleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                // 开始进行连接
            }

            override fun onConnectFail(bleDevice: BleDevice?, exception: BleException?) {
                // 连接失败
                _state.value = "连接失败，请尝试重新连接"
            }

            override fun onDisConnected(
                isActiveDisConnected: Boolean,
                bleDevice: BleDevice,
                gatt: BluetoothGatt,
                status: Int
            ) {
                // 连接断开
                _state.value = "连接断开，请尝试重新连接"
            }

            override fun onConnectSuccess(
                bleDevice: BleDevice,
                bluetoothGatt: BluetoothGatt,
                status: Int
            ) {
                // 连接成功并发现服务
                val serviceList: List<BluetoothGattService> =
                    bluetoothGatt.getServices()
                // 获取这个蓝牙设备所拥有的Service和Characteristic（特征）
                for (service in serviceList) {
                    val uuid_service = service.uuid
                    val characteristicList =
                        service.characteristics
                    for (characteristic in characteristicList) {
                        val uuid_chara = characteristic.uuid
                    }
                }
            }
        })
    }

    fun sendCommandToBluetoothDevice() {
//        BleManager.getInstance().write(
//            bleDevice,
//            uuid_service,
//            uuid_characteristic_write,
//            data,
//            object : BleWriteCallback() {
//                override fun onWriteSuccess(
//                    current: Int,
//                    total: Int,
//                    justWrite: ByteArray
//                ) { // 发送数据到设备成功（分包发送的情况下，可以通过方法中返回的参数可以查看发送进度）
//                }
//
//                override fun onWriteFailure(exception: BleException) { // 发送数据到设备失败
//                }
//            })
    }
}