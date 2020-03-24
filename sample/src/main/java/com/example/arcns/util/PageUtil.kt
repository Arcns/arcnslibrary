package com.example.arcns.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcns.core.util.Event
import com.example.arcns.data.network.*

class PageUtil(
    var initPage:Int = 1
) {
    // 当前页数
    private var _currentPage = MutableLiveData<Int>()
    // 加载失败
    private var _refreshFailed = MutableLiveData<Event<String>>()
    var refreshFailed: LiveData<Event<String>> = _refreshFailed

    fun getCurrentPage(): Int = _currentPage.value ?: initPage
    fun updateCurrentPage(refresh: Boolean) {
        if (refresh) {
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
            return newData
        } else {
            return mergePageData(originalData, newData)
        }
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
    fun <T, R> handleError(
        refresh: Boolean,
        originalData: List<T>?,
        result: Result<R>
    ): List<T>? {
        cancelLastUpdateCurrentPage()
        _refreshFailed.value = Event(result.errorMessage)
        return if (refresh) null else originalData
    }

    // 处理错误
    fun <T> handleError(
        result: Result<T>
    ) {
        cancelLastUpdateCurrentPage()
        _refreshFailed.value = Event(result.errorMessage)
    }

    // 取消上一次更新的页数
    fun cancelLastUpdateCurrentPage(){
        if (getCurrentPage() >= initPage) {
            _currentPage.value = getCurrentPage() - 1
        }
    }
}