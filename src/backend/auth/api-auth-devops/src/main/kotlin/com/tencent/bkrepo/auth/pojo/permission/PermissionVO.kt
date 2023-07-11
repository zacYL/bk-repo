package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.v3.oas.annotations.media.Schema

data class PermissionVO(
        @Schema(description = "ID")
        val id: String,
        @Schema(description = "授权主体ID")
        val subjectId: String,
        @Schema(description = "授权主体类型Code")
        val subjectCode: String,
        @Schema(description = "资源实例ID")
        val instanceId: String,
        @Schema(description = "资源类型标识")
        val resourceCode: String,
        @Schema(description = "资源动作Code")
        val actionCode: String,
        @Schema(description = "作用域ID")
        val scopeId: String,
        @Schema(description = "作用域类型")
        val scopeCode: String,
)