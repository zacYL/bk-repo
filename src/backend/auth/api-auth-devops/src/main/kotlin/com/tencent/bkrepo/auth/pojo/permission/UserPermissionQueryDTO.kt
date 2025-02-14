package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户权限查询请求")
data class UserPermissionQueryDTO(
    @ApiModelProperty("用户ID")
    val userId: String,
    @ApiModelProperty("资源类型CODE")
    val resourceCode: String,
    @ApiModelProperty("资源动作CODE集合，")
    val actionCodes: List<String> = emptyList(),
    @ApiModelProperty("资源实例ID集合，")
    val instanceIds: List<String> = emptyList(),
    @ApiModelProperty("为管理员或任意权限生成具体实例权限，目前只允许CCI调用！其他模块有需求请联系权限中心")
    val paddingInstancePermission: Boolean = false
)
