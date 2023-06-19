package com.tencent.bkrepo.replication.pojo.record

import io.swagger.annotations.ApiModelProperty

data class ReplicaArtifactStatistics(
    @ApiModelProperty("仓库")
    val localRepoName: String,
    @ApiModelProperty("制品名称")
    val artifactName: String,
    @ApiModelProperty("版本")
    val version: String? = null,
    @ApiModelProperty("成功数量")
    var success: Long = 0L,
    @ApiModelProperty("失败数量")
    var failed: Long = 0L
)
