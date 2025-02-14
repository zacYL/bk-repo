package com.tencent.bkrepo.auth.pojo.project

import com.tencent.bkrepo.auth.pojo.permission.ProjectUserPermissionSourceVO
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目成员信息")
data class ProjectMemberVO(
    @ApiModelProperty("用户ID")
    val userId: String,
    @ApiModelProperty("用户名称")
    val username: String,
    @ApiModelProperty("是否是系统管理员")
    val systemAdmin: Boolean,
    @ApiModelProperty("是否是租户管理员")
    val tenantAdmin: Boolean,
    @ApiModelProperty("是否项目统管理员")
    val projectAdmin: Boolean,
    @ApiModelProperty("启用/禁用")
    val enable: Boolean,
    @ApiModelProperty("用户权限来源")
    val permissionSource: ProjectUserPermissionSourceVO
)
