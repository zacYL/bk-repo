package com.tencent.bkrepo.auth.pojo.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "新增扩展字段值")
data class InsertUserFieldInfo(
    @Schema(name = "关联ID/修改时必传")
    val userFieldId: String = "",
    @Schema(name = "扩展字段ID/新增时必传")
    val id: String = "",
    @Schema(name = "扩展字段值")
    val fieldValue: String = ""
)
