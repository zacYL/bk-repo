package com.tencent.bkrepo.auth.service.canway.pojo

data class CanwayResponse<T>(
    val status: Int,
    val data: T?
)
