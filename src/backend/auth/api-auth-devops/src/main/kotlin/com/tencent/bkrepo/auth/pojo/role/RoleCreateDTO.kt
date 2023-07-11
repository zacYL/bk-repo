package com.tencent.bkrepo.auth.pojo.role

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "角色创建DTO")
class RoleCreateDTO(
    @Schema(description = "角色名")
    val name: String,
    @Schema(description = "描述")
    val desc: String,
    @Schema(description = "角色类型；租户角色、项目角色（租户内、项目内）")
    val type: String
)
