package com.tencent.bkrepo.auth.pojo.role

data class SlimNamedRoleVO(
    val id: String,
    val name: String,
    val type: String,
    val ownerId: String,
    val ownerCode: String,
)