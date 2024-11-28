package com.tencent.bkrepo.auth.pojo.permission

import com.tencent.bkrepo.auth.pojo.role.SubjectDTO
import io.swagger.annotations.ApiModelProperty
import com.tencent.bkrepo.auth.pojo.general.ScopeDTO

class CustomPermissionQueryDTO(
    @ApiModelProperty("作用域")
    val scopes: List<ScopeDTO>,
    @ApiModelProperty("授权主体权限")
    val subjects: List<SubjectDTO>,
    @ApiModelProperty("资源实例ID")
    val instanceIds: List<String> = emptyList(),
    @ApiModelProperty("资源类型")
    val resourceCodes: List<String> = emptyList(),
)
