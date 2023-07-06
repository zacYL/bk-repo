package com.tencent.bkrepo.auth.pojo.project

import com.tencent.bkrepo.auth.pojo.permission.ProjectUserPermissionSourceVO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "项目成员信息")
data class ProjectMemberVO(
    @Schema(description = "用户ID")
    val userId: String,
    @Schema(description = "用户名称")
    val username: String,
    @Schema(description = "是否是系统管理员")
    val systemAdmin: Boolean,
    @Schema(description = "是否是租户管理员")
    val tenantAdmin: Boolean,
    @Schema(description = "是否项目统管理员")
    val projectAdmin: Boolean,
    @Schema(description = "启用/禁用")
    val enable: Boolean,
    @Schema(description = "用户权限来源")
    val permissionSource: ProjectUserPermissionSourceVO
)