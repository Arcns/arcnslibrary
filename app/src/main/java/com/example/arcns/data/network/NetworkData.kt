package com.example.arcns.data.network

data class NetworkData<T>(
    var message: String? = null,
    var data: T? = null,
    var success: Boolean = false,
    var errCode:String? = null
){
    var lag:Long? = null
}