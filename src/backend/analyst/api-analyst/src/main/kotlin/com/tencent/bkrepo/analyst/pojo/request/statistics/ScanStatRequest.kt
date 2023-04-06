package com.tencent.bkrepo.analyst.pojo.request.statistics

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

@ApiModel("扫描统计请求")
data class ScanStatRequest(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("开始日期(包含)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val startTime: LocalDate,
    @ApiModelProperty("截止日期(包含)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val endTime: LocalDate
)
