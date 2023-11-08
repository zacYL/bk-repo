package com.tencent.bkrepo.auth.pojo.user

import io.swagger.v3.oas.annotations.media.Schema

data class UserPasswordUpdateVO(
    @Schema(name = "用户名")
    var username: String,
    @Schema(name = "密码")
    var password: String,
    @Schema(name = "独立制品库用户", required = true)
    val isCpackUser: Boolean = false,
)
