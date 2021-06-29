package com.tencent.bkrepo.auth.pojo.user

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("分页用户信息")
data class UserInfo(
    @ApiModelProperty("用户ID")
    val userId: String,
    @ApiModelProperty("用户名")
    val name: String,
    @ApiModelProperty("用户名")
    val locked: Boolean,
    @ApiModelProperty("是否管理员")
    val admin: Boolean
)
