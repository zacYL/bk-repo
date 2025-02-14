package com.tencent.bkrepo.auth.pojo.user

import io.swagger.annotations.ApiModelProperty

data class UserPasswordUpdateVO(
    @ApiModelProperty("用户名")
    var username: String,
    @ApiModelProperty("密码")
    var password: String,
    @ApiModelProperty("独立制品库用户", required = true)
    val isCpackUser: Boolean = false,
)
