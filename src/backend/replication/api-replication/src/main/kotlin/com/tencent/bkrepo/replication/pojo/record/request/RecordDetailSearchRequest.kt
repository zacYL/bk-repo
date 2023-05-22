package com.tencent.bkrepo.replication.pojo.record.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("同步详情搜索请求")
data class RecordDetailSearchRequest(
    @ApiModelProperty("分发计划key")
    val taskKey: String,
    @ApiModelProperty("记录id")
    val recordId: String? = null,
    @ApiModelProperty("仓库名称")
    val repoName: String,
    @ApiModelProperty("制品名称")
    val artifactName: String,
    @ApiModelProperty("制品版本")
    val version: String? = null
)
