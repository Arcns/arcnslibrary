package com.arcns.core.network

import okhttp3.Response


//任务失败
typealias OnTaskFailure<T> = (T, Exception?, Response?) -> Unit

//任务成功
typealias OnTaskSuccess<T> = (T, Response) -> Unit



const val TASK_NOTIFICATION_PLACEHOLDER_FILE_NAME = "{fileName}"
const val TASK_NOTIFICATION_PLACEHOLDER_LENGTH = "{length}"
const val TASK_NOTIFICATION_PLACEHOLDER_PERCENTAGE = "{percentage}"