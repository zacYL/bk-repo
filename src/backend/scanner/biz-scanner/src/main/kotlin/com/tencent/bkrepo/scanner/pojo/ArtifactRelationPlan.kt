package com.tencent.bkrepo.scanner.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("制品关联方案信息")
data class ArtifactRelationPlan(
    @ApiModelProperty("projectId")
    val projectId: String,
    @ApiModelProperty("方案id")
    val id: String,
    @ApiModelProperty("方案类型")
    val planType: String,
    @ApiModelProperty("方案名")
    val name: String,
    @ApiModelProperty("当前扫描状态")
    val status: String,
    @ApiModelProperty("扫描记录id")
    val recordId: String
)