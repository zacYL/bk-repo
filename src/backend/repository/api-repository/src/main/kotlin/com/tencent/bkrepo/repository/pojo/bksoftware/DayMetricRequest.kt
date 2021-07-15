package com.tencent.bkrepo.repository.pojo.bksoftware

import com.tencent.bkrepo.repository.pojo.log.OperateType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate

@ApiModel("每日 操作统计")
class DayMetricRequest(
    @ApiModelProperty("日期", example = "2021-04-08")
    val day: LocalDate,
    @ApiModelProperty("项目", example = "bkrepo")
    val projectId: String,
    @ApiModelProperty("仓库", example = "docker-local")
    val repoName: String,
    @ApiModelProperty("下载/上传 次数", example = "3")
    val type: OperateType
)
