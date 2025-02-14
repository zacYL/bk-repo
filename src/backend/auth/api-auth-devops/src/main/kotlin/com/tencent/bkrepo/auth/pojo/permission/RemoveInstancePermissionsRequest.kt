package com.tencent.bkrepo.auth.pojo.permission

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("移除资源权限请求")
data class RemoveInstancePermissionsRequest(
    @ApiModelProperty("资源实例ID")
    val instanceIds: List<String>,
    @ApiModelProperty("资源类型CODE")
    val resourceCode: String,
    @ApiModelProperty("制定作用域，默认为null，全部作用域")
    val scope: Pair<String, String>? = null,
)
