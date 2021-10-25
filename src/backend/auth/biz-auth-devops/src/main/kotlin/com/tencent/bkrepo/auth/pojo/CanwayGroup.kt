package com.tencent.bkrepo.auth.pojo

data class CanwayGroup(
    val id: String,
    val name: String,
    val description: String,
    val tenantCode: String,
    val users: List<Any>
)
