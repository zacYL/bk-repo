package com.tencent.bkrepo.auth.pojo.role

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("角色")
data class RoleVO(
    @ApiModelProperty("角色ID")
    val id: String,
    @ApiModelProperty("角色名")
    val name: String,
    @ApiModelProperty("描述")
    val desc: String,
    @ApiModelProperty("角色类型")
    val type: String,
    @ApiModelProperty("所有者ID")
    val ownerId: String,
    @ApiModelProperty("所有者标识", example = "PROJECT/TENANT/SYSTEM")
    val ownerCode: String,
    @ApiModelProperty("创建者")
    val createdBy: String,
    @ApiModelProperty("创建者名字")
    val createdByName: String,
    @ApiModelProperty("创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime,
    @ApiModelProperty("更新者")
    val updatedBy: String,
    @ApiModelProperty("更新者名字")
    val updatedByName: String,
    @ApiModelProperty("更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val updatedTime: LocalDateTime
)
