package com.tencent.bkrepo.repository.pojo.bksoftware

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
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
    @ApiModelProperty("事件类型", example = "3")
    val type: EventType,
    @ApiModelProperty("仓库类型")
    val repoType: RepositoryType
)
