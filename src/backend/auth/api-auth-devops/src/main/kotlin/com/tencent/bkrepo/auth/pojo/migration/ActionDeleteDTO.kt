package com.tencent.bkrepo.auth.pojo.migration

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("动作删除")
data class ActionDeleteDTO(
    @ApiModelProperty("动作ID")
    val actionIds: List<String>,
    @ApiModelProperty("是否物理删除")
    val physicalDelete: Boolean = false,
)
