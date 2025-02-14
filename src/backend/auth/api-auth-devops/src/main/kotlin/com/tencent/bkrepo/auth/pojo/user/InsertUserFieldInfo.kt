package com.tencent.bkrepo.auth.pojo.user

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("新增扩展字段值")
data class InsertUserFieldInfo(
    @ApiModelProperty("关联ID/修改时必传")
    val userFieldId: String = "",
    @ApiModelProperty("扩展字段ID/新增时必传")
    val id: String = "",
    @ApiModelProperty("扩展字段值")
    val fieldValue: String = ""
)
