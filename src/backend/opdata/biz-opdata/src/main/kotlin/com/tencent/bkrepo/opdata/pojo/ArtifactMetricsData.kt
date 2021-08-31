package com.tencent.bkrepo.opdata.pojo

import com.tencent.bkrepo.repository.pojo.packages.PackageType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("制品统计数据")
data class ArtifactMetricsData(
    @ApiModelProperty("项目名", example = "bkrepo")
    val projectId: String,
    @ApiModelProperty("仓库名", example = "maven")
    val repoName: String,
    @ApiModelProperty("包类型", example = "MAVEN")
    val repoType: PackageType,
    @ApiModelProperty("包名", example = "MAVEN")
    val packageName: String,
    @ApiModelProperty("版本", example = "1.0")
    val packageVersion: String,
    @ApiModelProperty("下载次数", example = "4")
    val count: Long,
    @ApiModelProperty("制品大小", example = "1323")
    val size: Long,
    @ApiModelProperty("packageKey")
    val packageKey: String
)

@ApiModel("制品下载统计数据")
data class ArtifactDownload(
    @ApiModelProperty("制品下载统计数据 列表")
    val artifactDownloadMetrics: List<ArtifactMetricsData?>
)

@ApiModel("制品大小统计数据")
data class ArtifactSize(
    @ApiModelProperty("制品大小统计数据 列表")
    val artifactSizeMetrics: List<ArtifactMetricsData?>
)
