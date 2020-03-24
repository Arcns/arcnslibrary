package com.example.arcns.util

import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import com.arcns.core.APP
import com.example.arcns.BuildConfig

class SharedPreferencesUtil {
    companion object {
        val DATA_NAME = "DATA_NAME"
        // 用户名密码
        val DATA_KEY_USERNAME = "DATA_KEY_USERNAME"
        val DATA_KEY_PASSWORD = "DATA_KEY_PASSWORD"
        val DATA_KEY_TOKEN = "DATA_KEY_TOKEN_"
        val DATA_KEY_TIME_DIFF = "DATA_KEY_TIME_DIFF_"
        val DATA_KEY_DATA_INTERFACE_RETURN_NETWORK_BASE_URL = "DATA_KEY_DATA_INTERFACE_RETURN_NETWORK_BASE_URL"


        fun saveSystemSetting(dataInterfaceReturnNetworkBaseUrl:String){
            APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE).edit {
                putString(DATA_KEY_DATA_INTERFACE_RETURN_NETWORK_BASE_URL, dataInterfaceReturnNetworkBaseUrl)
            }
        }

        fun getSystemSetting():String{
            val db = APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE)
            var dataInterfaceReturnNetworkBaseUrl = db.getString(DATA_KEY_DATA_INTERFACE_RETURN_NETWORK_BASE_URL,
                null)
            if (dataInterfaceReturnNetworkBaseUrl?.endsWith("/") == false){
                dataInterfaceReturnNetworkBaseUrl+="/"
            }
            return dataInterfaceReturnNetworkBaseUrl?: BuildConfig.BASEURL
        }

        /**
         * 保存Token和时间差
         */
        fun saveTokenAndTimeDiff(token:String?,timeDiff:Long?=null) =
            APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE).edit {
                putString(DATA_KEY_TOKEN, token)
                putLong(DATA_KEY_TIME_DIFF, timeDiff?:0)
            }

        fun getToken():String?{
            val db = APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE)
            var token = db.getString(DATA_KEY_TOKEN,null)
            return token
        }

        fun getTimeDiff():Long{
            val db = APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE)
            var timeDiff = db.getLong(DATA_KEY_TIME_DIFF,0)
            return timeDiff
        }


        /**
         * 保存所有登陆信息，包括账号密码
         */
        fun saveLoginInfo(userName: String, password: String) =
            APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE).edit {
                putString(DATA_KEY_USERNAME, userName)
                putString(DATA_KEY_PASSWORD, password)
            }

        fun getLoginInfo():Array<String>?{
            val db = APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE)
            var userName = db.getString(DATA_KEY_USERNAME,null)
            var password = db.getString(DATA_KEY_PASSWORD,null)
            if (userName.isNullOrBlank() || password.isNullOrBlank()) {
                return null
            }
            return arrayOf(userName,password)
        }

        /**
         * 清空保存的token
         */
        fun clearToken() = APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE).edit {
            remove(DATA_KEY_TOKEN)
            remove(DATA_KEY_TIME_DIFF)
        }


        /**
         * 清空保存的所有登陆信息，包括账号密码
         */
        fun clearLoginInfo() = APP.CONTEXT.getSharedPreferences(DATA_NAME, MODE_PRIVATE).edit {
            remove(DATA_KEY_USERNAME)
            remove(DATA_KEY_PASSWORD)
        }


    }
}
