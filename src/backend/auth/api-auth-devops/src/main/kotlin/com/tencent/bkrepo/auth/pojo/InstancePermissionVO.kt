package com.tencent.bkrepo.auth.pojo

class InstancePermissionVO(
    val instanceId: String,
    val resourceCode: String,
    var actionCodes: List<String>
)
