package com.tencent.bkrepo.analyst.pojo.response.statistics

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate

@ApiModel("扫描统计信息")
data class ScanStat(
    @ApiModelProperty("扫描制品总数")
    var scanCount: Long = 0,
    @ApiModelProperty("质量规则触发总数")
    var qualityTriggerCount: Long = 0,
    @ApiModelProperty("质量规则触发率")
    var triggerRate: Int = 0,
    @ApiModelProperty("每日详细数据")
    val dailyStatisticsDetails: List<ScanDetail>
) {
    fun calculateTriggerRate() {
        if (scanCount != 0L) {
            triggerRate = (qualityTriggerCount.toDouble() / scanCount * 100).toInt()
        }
    }
}

@ApiModel("单日扫描具体信息")
data class ScanDetail(
    @ApiModelProperty("扫描日期")
    val date: LocalDate,
    @ApiModelProperty("当日扫描数")
    var scanCount: Long = 0,
    @ApiModelProperty("当日触发数")
    var qualityTriggerCount: Long = 0
)
