package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.annotations.ApiModelProperty

class ResourcePermissionSaveDTO(
    @ApiModelProperty("资源类型标识")
    val resourceCode: String,
    @ApiModelProperty("资源任意实例权限集合")
    val anyInstancePermission: List<PermissionActionSaveDTO>,
    @ApiModelProperty("资源实例权限集合")
    val instancePermission: List<PermissionInstanceSaveDTO>,
)

class PermissionActionSaveDTO(
    @ApiModelProperty("资源动作CODE")
    val actionCode: String
)

class PermissionInstanceSaveDTO(
    @ApiModelProperty("资源实例ID")
    val instanceId: String,
    @ApiModelProperty("资源动作CODE")
    val actionCode: String
)
