package com.tencent.bkrepo.repository.service.canway.pojo

data class CanwayResponse<T>(
    val status: Int,
    val data: T?
)
