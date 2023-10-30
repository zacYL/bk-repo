package com.tencent.bkrepo.auth.pojo.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "UserInsertVO")
data class UserInsertVO(
    @Schema(name = "userId", required = true)
    val userId: String = "",
    @Schema(name = "displayName", required = true)
    val displayName: String = "",
    @Schema(name = "headPortrait", required = true)
    var headPortrait: String? = "",
    @Schema(name = "email", required = true)
    val email: String = "",
    @Schema(name = "telephone", required = true)
    val telephone: String = "",
    @Schema(name = "staffStatus", required = true)
    val staffStatus: Boolean = false,
    @Schema(name = "weChatId", required = true)
    val weChatId: String? = "",
    @Schema(name = "position", required = true)
    val position: String? = ""
)
