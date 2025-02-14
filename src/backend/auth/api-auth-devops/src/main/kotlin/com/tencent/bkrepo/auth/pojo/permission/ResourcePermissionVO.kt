package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.annotations.ApiModelProperty

class ResourcePermissionVO(
    @ApiModelProperty("资源类型标识")
    val resourceCode: String,
    @ApiModelProperty("资源任意实例权限集合")
    val anyInstancePermission: List<PermissionVO>,
    @ApiModelProperty("资源实例权限集合")
    val instancePermission: List<PermissionVO>,
)
