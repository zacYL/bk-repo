package com.tencent.bkrepo.auth.service.canway.pojo

data class CanwayPermissionResult(
    val hasPermission: Boolean,
    val roles: String?,
    val departments: String?
)
