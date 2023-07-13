package com.tencent.bkrepo.auth.pojo.role

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "角色成员")
data class RoleMemberVO(
    @Schema(description = "ID")
    val id: String,
    @Schema(description = "角色ID")
    val roleId: String,
    @Schema(description = "所有者ID")
    val ownerId: String,
    @Schema(description = "所有者标识")
    val ownerCode: String,
    @Schema(description = "作用域ID")
    val scopeId: String,
    @Schema(description = "作用域类型")
    val scopeCode: String,
    @Schema(description = "角色类型")
    val roleType: String,
    @Schema(description = "授权主体ID, eg. 用户ID/用户组ID/组织ID")
    val subjectId: String,
    @Schema(description = "授权主体名")
    val subjectName: String,
    @Schema(description = "授权主体标识", example = "USER/ROLE/...")
    val subjectCode: String,
    @Schema(description = "创建者")
    val createdBy: String,
    @Schema(description = "创建者名")
    val createdByName: String,
    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime
)