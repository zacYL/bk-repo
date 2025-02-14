package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.annotations.ApiModelProperty

data class AnyResourcePermissionSaveDTO(
    @ApiModelProperty("资源类型标识")
    val resourceCode: String,
    @ApiModelProperty("资源动作Code")
    val actionCode: String
)
