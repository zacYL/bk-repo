package com.tencent.bkrepo.analyst.pojo.response.statistics

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("漏洞统计信息")
data class LeakStat(
    @ApiModelProperty("危急漏洞数")
    var critical: Long = 0,
    @ApiModelProperty("高危漏洞数")
    var high: Long = 0,
    @ApiModelProperty("中危漏洞数")
    var medium: Long = 0,
    @ApiModelProperty("低危漏洞数")
    var low: Long = 0
)
