package com.arcns.media.selector

import android.provider.MediaStore
import androidx.lifecycle.*
import com.arcns.core.APP
import com.arcns.core.file.FileUtil
import com.arcns.core.file.getRandomCacheFilePath
import com.arcns.core.util.Event
import com.arcns.core.util.saveImageAsLocal
import com.arcns.core.util.string
import kotlinx.coroutines.*

class MediaSelectorViewModel : ViewModel() {

    // 正在加载或保存
    private var _loadIng = MutableLiveData<Boolean>()
    var loadIng: LiveData<Boolean> = _loadIng

    // 弹出提示
    private var _toast = MutableLiveData<Event<String>>()
    var toast: LiveData<Event<String>> = _toast

    // 所有媒体文件列表
    private var _allMedias = MutableLiveData<List<EMedia>>()
    var allMedias: LiveData<List<EMedia>> = _allMedias
    val allMediasSize: Int get() = allMedias.value?.size ?: 0

    // 当前选中的媒体文件
    private var _selectedMedias = MutableLiveData<ArrayList<EMedia>>()
    var selectedMedias: LiveData<ArrayList<EMedia>> = _selectedMedias
    val selectedMediasSize: Int get() = selectedMedias.value?.size ?: 0

    // 选择器结果
    private var _resultSelectedMedias = MutableLiveData<Event<ArrayList<EMedia>?>>()
    var resultSelectedMedias: LiveData<Event<ArrayList<EMedia>?>> = _resultSelectedMedias

    // 当前预览的媒体文件
    private var _previewMedias = MutableLiveData<List<EMedia>>()
    var previewMedias: LiveData<List<EMedia>> = _previewMedias
    val previewMediasSize: Int get() = previewMedias.value?.size ?: 0

    // 详情是否为预览模式
    private var _detailsIsPreview = MutableLiveData<Boolean>()
    var detailsIsPreview: LiveData<Boolean> = _detailsIsPreview

    // 详情是否为全屏模式
    private var _detailsIsFullScreen = MutableLiveData<Boolean>()
    var detailsIsFullScreen: LiveData<Boolean> = _detailsIsFullScreen

    // 是否仅为预览模式
    private var _isOnlyDetails = MutableLiveData<Boolean>()
    var isOnlyPreview: LiveData<Boolean> = _isOnlyDetails

    // 返回详情列表
    val detailsMedias: LiveData<List<EMedia>> = Transformations.switchMap(detailsIsPreview) {
        if (it)
            previewMedias
        else
            allMedias
    }
    val detailsMediasSize: Int get() = detailsMedias?.value?.size ?: 0

    // 详情标题（建议字段）
    val detailsTitleText: String
        get() = R.string.media_selector_details_title.string(
            currentMediaPosition + 1, detailsMediasSize
        )

    // 最大可选数量（默认为1）
    private var _selectedMaxSize = MutableLiveData<Int>()
    var selectedMaxSize: LiveData<Int> = _selectedMaxSize
    val selectedMaxSizeValue: Int get() = selectedMaxSize.value ?: 1

    // 选择器模式（拥有勾选框，在列表点击可进入预览）
    private var _isSelector = MutableLiveData<Boolean>().apply {
        value = true
    }
    var isSelector: LiveData<Boolean> = _isSelector

    // 当前的媒体文件
    private var _currentMedia = MutableLiveData<EMedia>()
    var currentMedia: LiveData<EMedia> = _currentMedia

    // 根据下标设置当前媒体文件
    fun setCurrentMediaByPosition(position: Int) {
        if (detailsIsPreview.value == true) {
            _currentMedia.value = previewMedias.value?.get(position)
        } else {
            _currentMedia.value = allMedias.value?.get(position)
        }
    }

    // 获取当前媒体文件的下标
    val currentMediaPosition: Int
        get() = if (detailsIsPreview.value == true) {
            previewMedias.value?.indexOf(currentMedia.value) ?: 0
        } else {
            allMedias.value?.indexOf(currentMedia.value) ?: 0
        }


    // 点击媒体文件事件
    private var _eventClickMedia = MutableLiveData<Event<EMedia>>()
    var eventClickMedia: LiveData<Event<EMedia>> = _eventClickMedia

    // 点击预览事件（点击左下角预览按钮）
    private var _eventClickPreview = MutableLiveData<Event<Unit>>()
    var eventClickPreview: LiveData<Event<Unit>> = _eventClickPreview

    // 完成媒体文件选择事件
    private var _eventCompleteSelectedMedias = MutableLiveData<Event<Unit>>()
    var eventCompleteSelectedMedias: LiveData<Event<Unit>> = _eventCompleteSelectedMedias

    // 完成媒体文件选择时转存策略
    private var _saveAsOption = MutableLiveData<EMediaSaveAsOption>()
    var saveAsOption: LiveData<EMediaSaveAsOption> = _saveAsOption

    // 完成按钮文本（建议字段）
    val completeButtonText: String
        get() =
            if (selectedMediasSize > 0)
                R.string.media_selector_complete_view_text_and_number.string(
                    selectedMediasSize,
                    selectedMaxSizeValue
                )
            else R.string.media_selector_complete_view_text.string


    // 预览按钮文本（建议字段）
    var previeweText: LiveData<String> = Transformations.map(_selectedMedias) {
        if (it != null && it.size > 0) {
            R.string.media_selector_media_bottom_bar_previewe_text_and_number.string(it.size)
        } else {
            R.string.media_selector_media_bottom_bar_previewe_text.string
        }
    }

    // 媒体库查询内容
    var mediaStoreMediaQuerys: List<EMediaQuery>? = null

    // 详情页中，点击打开文件时的回调事件，此变量为空或返回false时使用默认的打开方式（打开对应的app），返回true时表示由使用者自行使用打开方式
    var onDetailsFileClickOpenApp: ((item: EMedia) -> Boolean)? = null

    // 协程加载
    var setupFromMediaStoreDeferred: Deferred<Unit>? = null


    // 配置默认导航（如果要使用默认布局文件，则必须配置该参数）
    var defaultNavigationConfig: MediaSelectorNavigationConfig? = null
    var defaultNavigationTitleConfig: MediaSelectorNavigationTitleConfig? = null

    /**
     * 初始化
     */
    fun setupMediaSelector(
        mediaSelectedMaxSize: Int = 1,
        isSelector: Boolean = true,
        isSetupFromMediaStore: Boolean = true,
        initMedias: List<Any>? = null,
        currentMedia: Any? = null,
        isOnlyPreview: Boolean = false,
        saveAsOption: EMediaSaveAsOption? = null,
        setupFromMediaStoreMediaQuerys: Array<EMediaQuery> = arrayOf(EMediaQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)),
        onDetailsFileClickOpenApp: ((item: EMedia) -> Boolean)? = null
    ) {
//        if (allMedias.value != null) {
//            return
//        }
//        if (loadIng.value == true) {
//            return
//        }
        destroy()
        if (mediaSelectedMaxSize <= 1) {
            _selectedMaxSize.value = 1
        } else {
            _selectedMaxSize.value = mediaSelectedMaxSize
        }
        _isSelector.value = isSelector
        if (initMedias != null && initMedias.size > 0) {
            when (initMedias[0]) {
                is EMedia -> {
                    _allMedias.value = initMedias as? List<EMedia>
                }
                is String -> {
                    _allMedias.value = initMedias.map {
                        EMedia(path = it.toString())
                    }.toList()
                }
            }
        } else if (isSetupFromMediaStore) {
            if (setupFromMediaStoreDeferred != null) {
                setupFromMediaStoreDeferred?.cancel()
                setupFromMediaStoreDeferred = null
            }
            mediaStoreMediaQuerys = setupFromMediaStoreMediaQuerys.toList()
            //初始化媒体库里的文件（注意需要先获取READ_EXTERNAL_STORAGE权限）
            setupFromMediaStoreDeferred = viewModelScope.async(Dispatchers.IO) {
                _loadIng.postValue(true)
                _selectedMedias.postValue(null)
                delay(500)
                _allMedias.postValue(
                    getMediasFromMediaStore(*setupFromMediaStoreMediaQuerys)
                )
                _loadIng.postValue(false)
                setupFromMediaStoreDeferred = null
            }
        }
        var mCurrentMedia = currentMedia
        if (isOnlyPreview && mCurrentMedia == null && !initMedias.isNullOrEmpty()) {
            mCurrentMedia = initMedias[0]
        }
        if (mCurrentMedia != null) {
            when (mCurrentMedia) {
                is EMedia -> {
                    _currentMedia.value = mCurrentMedia
                }
                is String -> {
                    _currentMedia.value = EMedia(path = mCurrentMedia)
                }
            }
        }
        this.onDetailsFileClickOpenApp = onDetailsFileClickOpenApp
        _isOnlyDetails.value = isOnlyPreview
        _detailsIsPreview.value = false
        _detailsIsFullScreen.value = false
        if (saveAsOption != null) {
            _saveAsOption.value = saveAsOption
        } else if (_saveAsOption.value == null) {
            _saveAsOption.value = EMediaSaveAsOption()
        }
    }


    /**
     * 设置当前媒体文件
     */
    fun onSetCurrentMedia(item: EMedia) {
        _currentMedia.value = item
    }

    /**
     * 排序选中列表
     */
    fun onSortingSelectedMedias(fromPosition: Int, toPosition: Int) {
        _selectedMedias.value = _selectedMedias.value?.apply {
            val media = get(fromPosition) ?: return
            remove(media)
            add(toPosition, media)
            // 更新预览列表
            if (detailsIsPreview.value == true) {
                val previewMedias = ArrayList<EMedia>().apply {
                    addAll(_previewMedias.value ?: return)
                }
                this.forEachIndexed { index, media ->
                    previewMedias.remove(media)
                    previewMedias.add(index, media)
                }
                _previewMedias.value = previewMedias
            }
        }
    }

    /**
     * 点击媒体文件
     */
    fun onClickMedia(item: EMedia) {
        _detailsIsFullScreen.value = false
        _detailsIsPreview.value = false
        _currentMedia.value = item
        _eventClickMedia.value = Event(item)
    }

    /**
     * 预览
     */
    fun onPreviewe() {
        _detailsIsFullScreen.value = false
        _detailsIsPreview.value = true
        _previewMedias.value = ArrayList<EMedia>().apply {
            addAll(selectedMedias.value ?: return@apply)
        }
        _currentMedia.value = previewMedias.value?.first()
        _eventClickPreview.value = Event(Unit)
    }

    /**
     * 切换媒体文件详情全屏
     */
    fun onToggleDetailsFullScreen() {
        _detailsIsFullScreen.value = !(_detailsIsFullScreen.value ?: true)
    }

    /**
     * 选中/取消选中媒体文件
     */
    fun onToggleSelectedMedia(item: EMedia): Boolean {
        var medias = _selectedMedias.value ?: ArrayList()
        if (medias.contains(item)) {
            if (loadIng.value == true) {
                // 加载中时停止操作
                return true
            }
            // 取消选中
            medias.remove(item)
            _selectedMedias.value = medias
            _currentMedia.value = item
            return false
        } else {
            if (loadIng.value == true) {
                // 加载中时停止操作
                return false
            }
            if (medias.size + 1 <= selectedMaxSize.value ?: 1) {
                // 选中
                medias.add(item)
                _selectedMedias.value = medias
                _currentMedia.value = item
                return true
            } else {
                // 超过最大可选数量
                _toast.value =
                    Event(R.string.media_selector_max_size_tip.string(selectedMaxSize.value))
                return false
            }
        }
    }

    /**
     * 完成媒体文件选择
     */
    fun onCompleteSelectedMedias() {
        if (loadIng.value == true) {
            // 加载中时停止操作
            return
        }
        viewModelScope.launch {
            _loadIng.value = true
            saveResultSelectedMedias()
            destroy()
            _loadIng.value = false
            _eventCompleteSelectedMedias.value = Event(Unit)
        }
    }

    /**
     * 保存选中媒体文件到结果中
     */
    suspend fun saveResultSelectedMedias() {
        withContext(Dispatchers.IO) {
            saveAsOption.value?.let { option ->
                if (!option.enable) {
                    return@let
                }
                selectedMedias.value?.forEach {
                    try {
                        if (it.isImage && !it.isGif) {
                            // 图片
                            it.saveAsPath = saveImageAsLocal(
                                it.value,
                                option.saveAsWidth,
                                option.saveAsHeight,
                                option.centerInside,
                                option.highQualityBitmap,
                                option.saveAsCompressQuality,
                                option.isOriginal
                            ).absolutePath
                        } else {
                            // 视频或音频或gif
                            var saveFilePath: String? = getRandomCacheFilePath(it.suffix)
                            if (it.uri != null) {
                                if (!FileUtil.saveFileWithUri(APP.INSTANCE, it.uri, saveFilePath)) {
                                    saveFilePath = null
                                }
                            } else if (it.path != null) {
                                saveFilePath = FileUtil.copyFile(
                                    it.path,
                                    FileUtil.getFileDirectory(saveFilePath),
                                    FileUtil.getFileName(saveFilePath)
                                )
                            }
                            if (saveFilePath != null) {
                                it.uri = null
                                it.saveAsPath = saveFilePath
                                it.path = it.saveAsPath
                            } else {
                                throw Exception("media save as error")
                            }
                        }
                        it.uri = null
                        it.path = it.saveAsPath
                        it.saveAsException = null
                    } catch (e: Exception) {
                        it.saveAsException = e
                    }
                }
            }
            _resultSelectedMedias.postValue(Event(selectedMedias.value))
        }
    }

    /**
     * 媒体文件是否选中
     */
    fun isSelectedMedia(item: EMedia?): Boolean = _selectedMedias.value?.contains(item) == true

    /**
     * 返回选中媒体文件的序号
     */
    fun getSelectedMediaIndex(item: EMedia?): String = _selectedMedias.value?.indexOf(item)?.run {
        if (this >= 0) (this + 1).toString() else ""
    } ?: ""

    /**
     * 销毁资源
     */
    fun destroy() {
        _loadIng.value = false
        _saveAsOption.value = null
        _allMedias.value = null
        _selectedMedias.value = null
        onDetailsFileClickOpenApp = null
    }

}

