package com.tencent.bkrepo.auth.service.canway.pojo

data class CanwayRolePermissionRequest(
    val code: String,
    val name: String = "",
    val functionList: List<CanwayFunction>?
)

data class CanwayFunction(
    val id: String,
    val name:String = ""
)
