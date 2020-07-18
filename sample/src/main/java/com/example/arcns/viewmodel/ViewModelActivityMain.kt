package com.example.arcns.viewmodel

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcns.core.map.MapTrackRecorder
import com.arcns.core.util.Event
import kotlinx.coroutines.launch

class ViewModelActivityMain : ViewModel() {

    var test:Int = 0

    /**
     * 轨迹记录器
     */
    var mapTrackRecorder = MapTrackRecorder().apply {
        recorderInterval = 1000
    }

    /**
     * 蓝牙状态
     */
    private var _eventBluetoothState = MutableLiveData<Event<Int>>()
    var eventBluetoothState: LiveData<Event<Int>> = _eventBluetoothState
    fun setEventBluetoothState(state: Int) {
        _eventBluetoothState.value = Event(state)
    }

    /**
     * 是否显示首页菜单
     */
    private var _displayMainMenu = MutableLiveData<Int>()
    var displayMainMenu: LiveData<Int> = _displayMainMenu
    fun onMainMenuShow() {
        _displayMainMenu.value = View.VISIBLE
    }

    fun onMainMenuHide(anim: Boolean = true) {
        _displayMainMenu.value =
            if (anim && _displayMainMenu.value == View.VISIBLE) View.INVISIBLE else View.GONE
    }

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast
    fun postToast(value: String) {
        _toast.postValue(Event(value))
    }

    // 检查到新版本
    private var _checkNewVersionUpdate = MutableLiveData<Event<String>>()
    var checkNewVersionUpdate: LiveData<Event<String>> = _checkNewVersionUpdate

    // 启动界面继续运行
    private var _eventStartupPageContinue = MutableLiveData<Event<Unit>>()
    var eventStartupPageContinue: LiveData<Event<Unit>> = _eventStartupPageContinue

    /**
     * 检查APK更新
     */
    private var isCheckUpdate = false
    private var isNewVersionUpdate = false
    fun onCheckUpdate(isShowLoad: Boolean = false) {
        if (isCheckUpdate || isNewVersionUpdate) {
            return
        }
        isCheckUpdate = true
        if (isShowLoad) {
            _loadIng.value = true
        }
        viewModelScope.launch {
//            var versionResult = networkDataSource.updateVersion()
//            if (versionResult.succeeded) {
//                var version = (versionResult as Result.Success).data!!
//                var versionCode = try {
//                    version.version.toDouble()
//                } catch (e: Exception) {
//                    0.0
//                }
//                if (versionCode > APP.INSTANCE.versionCode.toLong()) {
//                    if (!isNewVersionUpdate) {
//                        _checkNewVersionUpdate.value = Event(version.url)
//                    }
//                    isNewVersionUpdate = true
//                } else {
//                    _eventStartupPageContinue.value = Event(Unit)
//                    if (isShowLoad) {
//                        _toast.value = Event("当前已是最新版本")
//                    }
//                }
//            } else {
//                _eventStartupPageContinue.value = Event(Unit)
//                if (isShowLoad) {
//                    _toast.value = Event("检查更新失败")
//                }
//            }
//            if (isShowLoad) {
//                _loadIng.value = false
//            }
//            isCheckUpdate = false
        }
    }
}