package com.tencent.bkrepo.auth.pojo

data class CanwayPermissionResult(
    val hasPermission: Boolean,
    val roles: String?,
    val departments: String?
)
