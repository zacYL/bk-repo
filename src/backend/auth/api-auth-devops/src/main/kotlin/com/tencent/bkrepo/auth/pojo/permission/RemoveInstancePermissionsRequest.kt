package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "移除资源权限请求")
data class RemoveInstancePermissionsRequest(
    @Schema(description = "资源实例ID")
    val instanceIds: List<String>,
    @Schema(description = "资源类型CODE")
    val resourceCode: String,
    @Schema(description = "制定作用域，默认为null，全部作用域")
    val scope: Pair<String, String>? = null,
)