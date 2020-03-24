package com.example.arcns.data.network

import com.arcns.core.util.LOG
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.example.arcns.util.SharedPreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit

class NetworkDataSource {

    private var networkAPI: NetworkAPI

    private fun getRandomValues(length: Int, onlyNumber: Boolean = false): String {
        val randoms = if (onlyNumber)
            "1234567890"
        else
            "abcdefghijklmnopqrstuvwxyz1234567890"
        var value = ""
        val randomLength = randoms.length
        for (i in 1..length) {
            value += randoms[(Math.random() * randomLength).toInt()]
        }
        return value
    }

    private fun getTimeDiff(lag: Long): Long {
        var time = Date().time / 1000
        var timeDiff = time + lag
        return timeDiff
    }

    private fun getAuthorization(): String? {
        var token = SharedPreferencesUtil.getToken()
        var lag = SharedPreferencesUtil.getTimeDiff()
        if (token.isNullOrBlank()) {
            return null
        }
        var timeDiff = getTimeDiff(lag)
        var value = getRandomValues(38) +
                getRandomValues(10, true) +
                timeDiff.toString() +
                getRandomValues(20, true) +
                getRandomValues(30) +
                token +
                getRandomValues(10)
        LOG(value)
        return value
    }

    init {
        // 添加公共Header
        val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            var request = chain.request().newBuilder().apply {
                // 添加Token
                getAuthorization()?.run {
//                    addHeader(DATA_INTERFACE_HEADER_KEY_AUTHORIZATION, this).build()
                }
            }.build()
            chain.proceed(request)
        }.addInterceptor(HttpLoggingInterceptor(logger = object : HttpLoggingInterceptor.Logger {
            // 拦截接口数据
            override fun log(message: String) {
                LOG("接口数据：$message")
            }
        }).apply {
            level = HttpLoggingInterceptor.Level.BODY
        }).connectTimeout(5, TimeUnit.SECONDS).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(SharedPreferencesUtil.getSystemSetting())
            .client(httpClient)
//            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(
                // 匹配Gson的@Expose注解，同时实现未配置@Expose的属性全部自动匹配
                GsonConverterFactory.create(
                    GsonBuilder().addSerializationExclusionStrategy(object : ExclusionStrategy {
                        override fun shouldSkipClass(clazz: Class<*>?): Boolean = false

                        override fun shouldSkipField(f: FieldAttributes?): Boolean {
                            val expose = f?.getAnnotation(Expose::class.java)
                            return expose != null && !expose.serialize
                        }

                    })
                        .addDeserializationExclusionStrategy(object : ExclusionStrategy {
                            override fun shouldSkipClass(clazz: Class<*>?): Boolean = false

                            override fun shouldSkipField(f: FieldAttributes?): Boolean {
                                val expose =
                                    f?.getAnnotation(Expose::class.java)
                                return expose != null && !expose.deserialize
                            }

                        })
                        .create()
                )
            )
            .build();
        networkAPI = retrofit.create(NetworkAPI::class.java)
    }



    /**
     *
     */
    suspend fun test(
    ): Result<Any?> = networkAPI.test(
    ).executeResult()

    /**
     * 执行并返回结果
     */
    private suspend fun <T> Call<NetworkData<T>>.executeResult(autoLogin: Boolean = true): Result<T?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                var networkData = executeNetworkData(autoLogin)
                if (networkData.success) {
                    Result.Success(networkData.data, networkData.lag)
                } else {
                    Result.Error(
                        message = networkData.message,
                        errCode = networkData.errCode
                    )
                }
            } catch (e: Exception) {
                Result.Error(
                    exception = e
                )
            }
        }


    /**
     * 数据请求
     */
    private suspend fun <T> Call<NetworkData<T>>.executeNetworkData(autoLogin: Boolean = true): NetworkData<T> {
        try {
            LOG("开始加载")
            var response: Response<NetworkData<T>> = this.clone().execute()
            if (!response.isSuccessful) {
                LOG("加载失败：" + response.code())
                return NetworkData<T>(
                    message = "响应错误：" + response.code(),
                    success = false
                )
            }
            var networkData = response.body() as NetworkData<T>

            if (!networkData?.success){
                // 全局错误
                when(networkData.errCode){
                    "401"->{

                    }
                }
            }

            return networkData;
        } catch (e: Exception) {
            if (e is SocketTimeoutException) {
                return NetworkData<T>(
                    message = "网络超时",
                    success = false
                )
            } else {
                return NetworkData<T>(
                    message = "请求错误",
                    success = false
                )
            }
            throw e;
        }
    }

}