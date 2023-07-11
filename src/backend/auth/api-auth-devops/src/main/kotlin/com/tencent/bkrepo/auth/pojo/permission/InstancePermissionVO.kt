package com.tencent.bkrepo.auth.pojo.permission

class InstancePermissionVO(
    val instanceId: String,
    val resourceCode: String,
    var actionCodes: List<String>
)
