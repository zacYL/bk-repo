package com.tencent.bkrepo.scanner.pojo.request

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("自动扫描请求")
data class AtomScanRequest(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("仓库名")
    val repoName: String,
    @ApiModelProperty("仓库类型")
    val repoType: RepositoryType,
    @ApiModelProperty("制品名称")
    val artifactName: String,
    @ApiModelProperty("packageKey")
    val packageKey: String? = null,
    @ApiModelProperty("version")
    val version: String? = null,
    @ApiModelProperty("fullPath")
    val fullPath: String? = null
)
