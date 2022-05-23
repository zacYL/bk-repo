package com.tencent.bkrepo.repository.pojo.metric

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("指定通用文件的下载量")
data class NodeDownloadCount(
    @ApiModelProperty("仓库名")
    val repoName: String,
    @ApiModelProperty("全路径")
    val fullPath: String,
    @ApiModelProperty("文件名")
    val name: String,
    @ApiModelProperty("下载量")
    var downloadCount: Long
)
