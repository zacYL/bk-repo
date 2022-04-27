package com.tencent.bkrepo.repository.pojo.metric

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("制品包下载量")
data class PackageDownloadCount(
    @ApiModelProperty("包名")
    val packageName: String,
    @ApiModelProperty("下载数")
    var downloadCount: Long
)
