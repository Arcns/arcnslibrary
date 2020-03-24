package com.example.arcns

import androidx.multidex.MultiDexApplication
import com.arcns.core.APP
import com.scwang.smartrefresh.header.MaterialHeader
import com.scwang.smartrefresh.layout.SmartRefreshLayout

class ZkxtApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        APP.INSTANCE = this
        setupSmartRefreshLayout()
    }

    //设置全局的列表刷新Header Footer构建器
    private fun setupSmartRefreshLayout() {
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
            MaterialHeader(context)
        }
//        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout ->
//            ClassicsFooter(context)
//        }
    }
}