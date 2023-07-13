package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.v3.oas.annotations.media.Schema

data class AnyResourcePermissionSaveDTO(
        @Schema(description = "资源类型标识")
        val resourceCode: String,
        @Schema(description = "资源动作Code")
        val actionCode: String
)