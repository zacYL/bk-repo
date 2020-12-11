package com.tencent.bkrepo.auth.service.canway.pojo

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction

data class CanwayAction(
    val action: PermissionAction,
    val nickName: String
)
