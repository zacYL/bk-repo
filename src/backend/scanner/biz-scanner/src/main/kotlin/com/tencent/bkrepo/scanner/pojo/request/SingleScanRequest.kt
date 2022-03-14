package com.tencent.bkrepo.scanner.pojo.request

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("单个制品扫描请求")
data class SingleScanRequest(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("仓库名")
    val repoName: String,
    @ApiModelProperty("仓库类型")
    val repoType: RepositoryType,
    @ApiModelProperty("制品名称")
    val name: String,
    @ApiModelProperty("方案ID")
    val id: String,
    @ApiModelProperty("packageKey")
    val packageKey: String?,
    @ApiModelProperty("version")
    val version: String?,
    @ApiModelProperty("fullPath")
    var fullPath: String?
)
