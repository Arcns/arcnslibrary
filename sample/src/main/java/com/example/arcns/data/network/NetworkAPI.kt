package com.example.arcns.data.network

import retrofit2.Call
import retrofit2.http.*

interface NetworkAPI {


    // 测试
    @POST("collect/test")
    fun test(
    ): Call<NetworkData<Any?>>
}