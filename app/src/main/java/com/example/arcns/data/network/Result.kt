package com.example.arcns.data.network


sealed class Result<out R> {

    data class Success<out T>(val data: T,var lag:Long? = null) : Result<T>()
    data class Error(
        val message: String? = null,
        val exception: Exception? = null,
        var isGlobalError: Boolean = false,
        var errCode:String? = null
    ) :
        Result<Nothing>()

    object Loading : Result<Nothing>()


}



val Result<*>.errorMessage: String
    get() = if (this is Result.Error) message
        ?: "数据请求发生错误，请稍候重试" else ""

val Result<*>.succeeded
    get() = this is Result.Success

val Result<*>.hasNonGlobalError
    get() = this is Result.Error && !isGlobalError