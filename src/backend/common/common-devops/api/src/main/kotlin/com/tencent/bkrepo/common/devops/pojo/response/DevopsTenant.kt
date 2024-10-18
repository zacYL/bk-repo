package com.tencent.bkrepo.common.devops.pojo.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "租户信息")
class DevopsTenant(
    @Schema(description = "租户ID")
    val id: String,
    @Schema(description = "租户标识", example = "bk_ci")
    val code: String,
    @Schema(description = "租户名")
    val name: String,
    @Schema(description = "启用用户数")
    val enabledUserNum: Int?,
    @Schema(description = "是否启用")
    val enabled: Boolean,
    @Schema(description = "是否启用新用户")
    val autoEnableUser: Boolean,
    @Schema(description = "创建者")
    val createdBy: String,
    @Schema(description = "创建者名")
    val createdByName: String,
    @Schema(description = "更新者")
    val updatedBy: String,
    @Schema(description = "更新者名字")
    val updatedByName: String,
    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime,
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val updatedTime: LocalDateTime,
    @Schema(description = "关联的组织信息")
    val department: DepartmentSlimVO,
    @Schema(description = "管理员列表")
    val admins: List<AdminVO>
) {
    data class DepartmentSlimVO(
        @Schema(description = "部门ID")
        val id: String,
        @Schema(description = "部门名")
        val name: String,
        @Schema(description = "父部门ID,根部门的父部门ID默认为0")
        val parentId: String
    )

    @Schema(description = "管理员")
    data class AdminVO(
        @Schema(description = "ID")
        val id: String?,
        @Schema(description = "用户ID")
        val userId: String,
        @Schema(description = "姓名")
        val username: String,
        @Schema(description = "所属作用域ID")
        val scopeId: String,
        @Schema(description = "所属作用域标识: system/tenant/project")
        val scopeCode: String,
        @Schema(description = "创建者")
        val createdBy: String,
        @Schema(description = "创建时间")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val createdTime: LocalDateTime,
        @Schema(description = "创建者名")
        val createdByName: String,
        @Schema(description = "更新者")
        val updatedBy: String,
        @Schema(description = "更新时间")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val updatedTime: LocalDateTime,
        @Schema(description = "更新者名")
        val updatedByName: String
    )
}
