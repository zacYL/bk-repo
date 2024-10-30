package com.tencent.bkrepo.auth.pojo.role

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("角色创建DTO")
class RoleCreateDTO(
    @ApiModelProperty("角色名")
    val name: String,
    @ApiModelProperty("描述")
    val desc: String,
    @ApiModelProperty("角色类型；租户角色、项目角色（租户内、项目内）")
    val type: String
)
