package com.tencent.bkrepo.common.devops.pojo.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("租户信息")
class DevopsTenant(
    @ApiModelProperty("租户ID")
    val id: String,
    @ApiModelProperty("租户标识", example = "bk_ci")
    val code: String,
    @ApiModelProperty("租户名")
    val name: String,
    @ApiModelProperty("启用用户数")
    val enabledUserNum: Int?,
    @ApiModelProperty("是否启用")
    val enabled: Boolean,
    @ApiModelProperty("是否启用新用户")
    val autoEnableUser: Boolean,
    @ApiModelProperty("创建者")
    val createdBy: String,
    @ApiModelProperty("创建者名")
    val createdByName: String,
    @ApiModelProperty("更新者")
    val updatedBy: String,
    @ApiModelProperty("更新者名字")
    val updatedByName: String,
    @ApiModelProperty("创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime,
    @ApiModelProperty("更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val updatedTime: LocalDateTime,
    @ApiModelProperty("关联的组织信息")
    val department: DepartmentSlimVO,
    @ApiModelProperty("管理员列表")
    val admins: List<AdminVO>
) {
    data class DepartmentSlimVO(
        @ApiModelProperty("部门ID")
        val id: String,
        @ApiModelProperty("部门名")
        val name: String,
        @ApiModelProperty("父部门ID,根部门的父部门ID默认为0")
        val parentId: String
    )

    @ApiModel("管理员")
    data class AdminVO(
        @ApiModelProperty("ID")
        val id: String?,
        @ApiModelProperty("用户ID")
        val userId: String,
        @ApiModelProperty("姓名")
        val username: String,
        @ApiModelProperty("所属作用域ID")
        val scopeId: String,
        @ApiModelProperty("所属作用域标识: system/tenant/project")
        val scopeCode: String,
        @ApiModelProperty("创建者")
        val createdBy: String,
        @ApiModelProperty("创建时间")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val createdTime: LocalDateTime,
        @ApiModelProperty("创建者名")
        val createdByName: String,
        @ApiModelProperty("更新者")
        val updatedBy: String,
        @ApiModelProperty("更新时间")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val updatedTime: LocalDateTime,
        @ApiModelProperty("更新者名")
        val updatedByName: String
    )
}
