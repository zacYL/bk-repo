package com.tencent.bkrepo.auth.pojo.role

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "角色")
data class RoleVO(
    @Schema(description = "角色ID")
    val id: String,
    @Schema(description = "角色名")
    val name: String,
    @Schema(description = "描述")
    val desc: String,
    @Schema(description = "角色类型")
    val type: String,
    @Schema(description = "所有者ID")
    val ownerId: String,
    @Schema(description = "所有者标识", example = "PROJECT/TENANT/SYSTEM")
    val ownerCode: String,
    @Schema(description = "创建者")
    val createdBy: String,
    @Schema(description = "创建者名字")
    val createdByName: String,
    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime,
    @Schema(description = "更新者")
    val updatedBy: String,
    @Schema(description = "更新者名字")
    val updatedByName: String,
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val updatedTime: LocalDateTime
)
