package com.tencent.bkrepo.common.devops.pojo.response

data class CanwayResponse<T>(
    val status: Int,
    val data: T?
)
