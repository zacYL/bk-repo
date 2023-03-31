package com.tencent.bkrepo.common.devops.pojo.request

data class CanwayUserGroupRequest(
    val owner: Owner? = null,
    val groupIds: List<String>
)

data class Owner(
    val scopeCode: String,
    val scopeId: String
)