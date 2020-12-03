package com.tencent.bkrepo.auth.service.canway.pojo

import com.tencent.bkrepo.auth.service.canway.ciBelongCode

data class CanwayRoleRequest(
    val name: String,
    val parentId: String,
    val service: String,
    val belongCode: String = ciBelongCode,
    val belongInstance: String,
    val description: String
)
