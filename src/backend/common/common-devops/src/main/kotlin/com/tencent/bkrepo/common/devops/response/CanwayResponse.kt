package com.tencent.bkrepo.common.devops.response

data class CanwayResponse<T>(
    val status: Int,
    val data: T?
)
