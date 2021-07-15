package com.tencent.bkrepo.repository.pojo.bksoftware

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate

@ApiModel("每日下载/上传 统计数据")
data class DayMetricsData(
    @ApiModelProperty("日期", example = "2021-04-08")
    val time: LocalDate,
    @ApiModelProperty("项目", example = "bkrepo")
    val projectId: String?,
    @ApiModelProperty("仓库", example = "docker-local")
    val repoName: String?,
    @ApiModelProperty("下载/上传 次数", example = "3")
    var count: Long
)

@ApiModel("下载统计")
data class DownloadMetric(
    @ApiModelProperty("每日下载统计数据 列表")
    val downloadMetrics: List<DayMetricsData?>
)

@ApiModel("每日上传量统计")
data class UploadMetric(
    @ApiModelProperty("每日下载统计数据 列表")
    val uploadMetrics: List<DayMetricsData?>
)
