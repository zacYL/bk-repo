package com.tencent.bkrepo.auth.pojo.permission

data class InstancePermissionVO(
    val instanceId: String,
    val resourceCode: String,
    var actionCodes: List<String>
)
