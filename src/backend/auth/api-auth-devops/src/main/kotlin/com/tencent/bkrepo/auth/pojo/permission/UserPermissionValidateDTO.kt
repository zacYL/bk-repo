package com.tencent.bkrepo.auth.pojo.permission

import com.tencent.bkrepo.auth.constant.AuthConstant
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel(description = "用户鉴权请求")
data class UserPermissionValidateDTO(
    @ApiModelProperty("用户ID")
    val userId: String,
    @ApiModelProperty("资源实例ID，功能权限使用 *")
    val instanceId: String = AuthConstant.ANY_RESOURCE_CODE,
    @ApiModelProperty("资源类型CODE")
    val resourceCode: String,
    @ApiModelProperty("资源动作CODE，只有满足集合内的所有动作时才会返回true")
    val actionCodes: List<String>
)
