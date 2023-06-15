package com.tencent.bkrepo.auth.pojo


class UserPermissionQueryDTO(
    val userId: String,
    val resourceCode: String,
    val actionCodes: List<String> = emptyList(),
    val instanceIds: List<String> = emptyList(),
    val paddingInstancePermission: Boolean = false
)
