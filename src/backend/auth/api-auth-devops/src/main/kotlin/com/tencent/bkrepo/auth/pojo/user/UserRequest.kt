package com.tencent.bkrepo.auth.pojo.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "新增用户请求")
data class UserRequest(
    @Schema(name = "userInsertVO", required = true)
    val userInsertVO: UserInsertVO,
    @Schema(name = "insertUserFieldInfos", required = true)
    val insertUserFieldInfos: List<InsertUserFieldInfo>? = mutableListOf(),
    @Schema(name = "orgIds", required = true)
    val orgIds: List<String>? = mutableListOf()
)
