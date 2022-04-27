package com.tencent.bkrepo.repository.pojo.project

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate

@ApiModel("项目统计信息总览")
data class ProjectStatisticsSummary(
    @ApiModelProperty("版本上传数")
    val versionUploadCount: Long,
    @ApiModelProperty("版本下载数")
    val versionDownloadCount: Long,
    @ApiModelProperty("上传制品的用户数")
    val userUploadCount: Long,
    @ApiModelProperty("下载制品的用户数")
    val userDownloadCount: Long,
    @ApiModelProperty("时间段内每天具体统计信息")
    val dailyStatisticsDetails: List<DayDetail>
)

@ApiModel("每天具体统计信息")
data class DayDetail(
    @ApiModelProperty("日期")
    val date: LocalDate,
    @ApiModelProperty("版本上传数")
    var uploadCount: Long,
    @ApiModelProperty("版本下载数")
    var downloadCount: Long
)
