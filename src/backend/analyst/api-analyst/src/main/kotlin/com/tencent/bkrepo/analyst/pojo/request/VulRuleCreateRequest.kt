package com.tencent.bkrepo.analyst.pojo.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量创建漏洞黑白名单条目请求")
data class VulRuleCreateRequest(
    @ApiModelProperty("漏洞规则列表")
    val vulRules: List<VulRuleItem>,
    @ApiModelProperty("冲突处理策略")
    val overwrite: Boolean = false
)

@ApiModel("漏洞规则项")
data class VulRuleItem(
    @ApiModelProperty("漏洞编号")
    val vulId: String,
    @ApiModelProperty("规则类型，为true时不计入漏洞统计，为false时触发禁用")
    val pass: Boolean,
    @ApiModelProperty("自定义描述")
    val description: String? = null
)
