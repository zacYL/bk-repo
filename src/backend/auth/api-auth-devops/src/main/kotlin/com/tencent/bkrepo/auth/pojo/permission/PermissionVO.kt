package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.annotations.ApiModelProperty

data class PermissionVO(
    @ApiModelProperty("ID")
    val id: String,
    @ApiModelProperty("授权主体ID")
    val subjectId: String,
    @ApiModelProperty("授权主体类型Code")
    val subjectCode: String,
    @ApiModelProperty("资源实例ID")
    val instanceId: String,
    @ApiModelProperty("资源类型标识")
    val resourceCode: String,
    @ApiModelProperty("资源动作Code")
    val actionCode: String,
    @ApiModelProperty("作用域ID")
    val scopeId: String,
    @ApiModelProperty("作用域类型")
    val scopeCode: String,
)
