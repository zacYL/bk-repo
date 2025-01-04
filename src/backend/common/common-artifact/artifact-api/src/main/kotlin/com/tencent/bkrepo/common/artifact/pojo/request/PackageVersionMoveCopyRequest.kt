package com.tencent.bkrepo.common.artifact.pojo.request

import io.swagger.annotations.ApiModelProperty

/**
 * 包版本移动/复制请求
 */
data class PackageVersionMoveCopyRequest(
    @ApiModelProperty("源项目id", required = true)
    val srcProjectId: String,
    @ApiModelProperty("源仓库名称", required = true)
    val srcRepoName: String,
    @ApiModelProperty("目的项目id", required = true)
    val dstProjectId: String,
    @ApiModelProperty("目的仓库名称", required = true)
    val dstRepoName: String,
    @ApiModelProperty("包唯一Key", required = true)
    val packageKey: String,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("是否覆盖", required = false)
    val overwrite: Boolean = false
)
