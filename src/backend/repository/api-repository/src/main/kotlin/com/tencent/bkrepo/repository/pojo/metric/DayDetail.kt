package com.tencent.bkrepo.repository.pojo.metric

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate

@ApiModel("每天具体统计信息")
data class DayDetail(
    @ApiModelProperty("日期")
    val date: LocalDate,
    @ApiModelProperty("通用文件上传数")
    var uploadCount: Long,
    @ApiModelProperty("通用文件下载数")
    var downloadCount: Long
)
