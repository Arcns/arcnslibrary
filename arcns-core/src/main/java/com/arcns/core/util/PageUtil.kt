package com.arcns.core.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import okio.Okio

class PageUtil(
    // 初始化页数
    var initPage: Int = 1,
    // 每页数据的数量（用于判断是否已经到最后一页）
    var pageSize: Int = 20
) {
    // 加载失败
    private var _refreshFailed = MutableLiveData<Event<String>>()
    var refreshFailed: LiveData<Event<String>> = _refreshFailed

    // 是否为首次加载数据
    private var _isFirstRefresh = MutableLiveData<Boolean>()
    val isFirstRefresh: Boolean get() = _isFirstRefresh.value ?: true

    // 是否为空数据
    private var _isDataEmpty = MutableLiveData<Boolean>()
    var isDataEmpty: LiveData<Boolean> = _isDataEmpty
    fun updateIsDataEmpty(isDataEmpty: Boolean) {
        _isDataEmpty.value = isDataEmpty
    }

    // 没有更多数据时回调
    private var _noMoreData = MutableLiveData<Boolean>()
    var noMoreData: LiveData<Boolean> = _noMoreData


    // 当前页数
    private var _currentPage = MutableLiveData<Int>()
    fun getCurrentPage(): Int = _currentPage.value ?: initPage
    fun updateCurrentPage(refresh: Boolean) {
        updateIsDataEmpty(false)
        if (refresh) {
            _isFirstRefresh.value = false
            _currentPage.value = initPage
        } else {
            _currentPage.value = getCurrentPage() + 1
        }
    }

    // 处理分页数据
    fun <T> handlePageData(
        refresh: Boolean,
        originalData: List<T>?,
        newData: List<T>?
    ): List<T>? {
        if (refresh) {
            if (newData == null || newData.isEmpty()){
                updateIsDataEmpty(true)
            }
            checkNoMoreData(newData)
            return newData
        } else {
            val data = mergePageData(originalData, newData)
            checkNoMoreData(newData)
            return data
        }
    }

    // 检查是否已经到最后一页
    fun <T> checkNoMoreData(newData: List<T>?){
        _noMoreData.value = newData?.size ?: 0 < pageSize
    }

    // 合并分页数据
    fun <T> mergePageData(originalData: List<T>?, newData: List<T>?): List<T> {
        var datas = ArrayList<T>()
        originalData?.run {
            datas.addAll(this)
        }
        newData?.run {
            datas.addAll(this)
        }
        return datas
    }

    // 处理错误
    fun <T> handleError(
        refresh: Boolean,
        originalData: List<T>?,
        error:String
    ): List<T>? {
        if (refresh){
            updateIsDataEmpty(true)
        }
        cancelLastUpdateCurrentPage()
        _refreshFailed.value = Event(error)
        return if (refresh) null else originalData
    }

    // 处理错误
    fun handleError(
        error:String
    ) {
        cancelLastUpdateCurrentPage()
        if (getCurrentPage() <= initPage){
            updateIsDataEmpty(true)
        }
        _refreshFailed.value = Event(error)
    }

    // 取消上一次更新的页数
    fun cancelLastUpdateCurrentPage() {
        if (getCurrentPage() >= initPage) {
            _currentPage.value = getCurrentPage() - 1
        }
    }
}