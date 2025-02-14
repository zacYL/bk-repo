package com.tencent.bkrepo.repository.pojo.project

import com.tencent.bkrepo.repository.pojo.metric.DayDetail
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目依赖包统计信息总览")
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
