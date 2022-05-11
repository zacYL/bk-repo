package com.tencent.bkrepo.repository.pojo.node

import com.tencent.bkrepo.repository.pojo.metric.DayDetail
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目通用文件统计信息总览")
data class NodeStatisticsSummary(
    @ApiModelProperty("通用文件上传数")
    val nodeUploadCount: Long,
    @ApiModelProperty("通用文件下载数")
    val nodeDownloadCount: Long,
    @ApiModelProperty("上传通用文件的用户数")
    val userUploadCount: Long,
    @ApiModelProperty("下载通用文件的用户数")
    val userDownloadCount: Long,
    @ApiModelProperty("时间段内每天具体统计信息")
    val dailyStatisticsDetails: List<DayDetail>
)
