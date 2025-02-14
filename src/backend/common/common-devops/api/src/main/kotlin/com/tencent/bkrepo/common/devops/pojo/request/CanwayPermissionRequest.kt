package com.tencent.bkrepo.common.devops.pojo.request

import com.tencent.bkrepo.common.devops.RESOURCECODE

data class CanwayPermissionRequest(
    val userId: String,
    val instanceId: String? = "*",
    val resourceCode: String? = RESOURCECODE,
    val actionCodes: List<String>
)