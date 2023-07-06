package com.tencent.bkrepo.auth.pojo.permission

import com.tencent.bkrepo.auth.constant.AuthConstant
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "用户鉴权请求")
data class UserPermissionValidateDTO(
    @Schema(description = "用户ID")
    val userId: String,
    @Schema(description = "资源实例ID，功能权限使用 *")
    val instanceId: String = AuthConstant.ANY_RESOURCE_CODE,
    @Schema(description = "资源类型CODE")
    val resourceCode: String,
    @Schema(description = "资源动作CODE，只有满足集合内的所有动作时才会返回true")
    val actionCodes: List<String>
)

