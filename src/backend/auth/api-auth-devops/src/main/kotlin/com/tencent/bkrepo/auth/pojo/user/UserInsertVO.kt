package com.tencent.bkrepo.auth.pojo.user

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("UserInsertVO")
data class UserInsertVO(
    @ApiModelProperty("userId", required = true)
    val userId: String = "",
    @ApiModelProperty("displayName", required = true)
    val displayName: String = "",
    @ApiModelProperty("headPortrait", required = true)
    var headPortrait: String? = "",
    @ApiModelProperty("email", required = true)
    val email: String = "",
    @ApiModelProperty("telephone", required = true)
    val telephone: String = "",
    @ApiModelProperty("staffStatus", required = true)
    val staffStatus: Boolean = false,
    @ApiModelProperty("weChatId", required = true)
    val weChatId: String? = "",
    @ApiModelProperty("position", required = true)
    val position: String? = ""
)
