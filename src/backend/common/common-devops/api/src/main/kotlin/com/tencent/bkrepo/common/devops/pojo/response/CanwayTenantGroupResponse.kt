package com.tencent.bkrepo.common.devops.pojo.response

data class CanwayTenantGroupResponse(
    val id: String,
    val name: String,
    val desc: String? = "",
    val ownerCode: String? = "",
    val ownerId: String? = "",
    val deleted: Boolean
)
