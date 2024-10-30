package com.tencent.bkrepo.auth.pojo.role

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("角色成员")
data class RoleMemberVO(
    @ApiModelProperty("ID")
    val id: String,
    @ApiModelProperty("角色ID")
    val roleId: String,
    @ApiModelProperty("所有者ID")
    val ownerId: String,
    @ApiModelProperty("所有者标识")
    val ownerCode: String,
    @ApiModelProperty("作用域ID")
    val scopeId: String,
    @ApiModelProperty("作用域类型")
    val scopeCode: String,
    @ApiModelProperty("角色类型")
    val roleType: String,
    @ApiModelProperty("授权主体ID, eg. 用户ID/用户组ID/组织ID")
    val subjectId: String,
    @ApiModelProperty("授权主体名")
    val subjectName: String,
    @ApiModelProperty("授权主体标识", example = "USER/ROLE/...")
    val subjectCode: String,
    @ApiModelProperty("创建者")
    val createdBy: String,
    @ApiModelProperty("创建者名")
    val createdByName: String,
    @ApiModelProperty("创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime
)
