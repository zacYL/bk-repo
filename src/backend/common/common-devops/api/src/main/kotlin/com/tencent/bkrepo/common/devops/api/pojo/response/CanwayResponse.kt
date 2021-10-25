package com.tencent.bkrepo.common.devops.api.pojo.response

data class CanwayResponse<T>(
    val status: Int,
    val data: T?
)
