package com.tencent.bkrepo.auth.pojo.user

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("新增用户请求")
data class UserRequest(
    @ApiModelProperty("userInsertVO", required = true)
    val userInsertVO: UserInsertVO,
    @ApiModelProperty("insertUserFieldInfos", required = true)
    val insertUserFieldInfos: List<InsertUserFieldInfo>? = mutableListOf(),
    @ApiModelProperty("orgIds", required = true)
    val orgIds: List<String>? = mutableListOf()
)
