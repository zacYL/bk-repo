package com.tencent.bkrepo.analyst.pojo.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量删除漏洞黑白名单条目请求")
data class VulRuleDeleteRequest(
    @ApiModelProperty("漏洞编号列表")
    val vulIdList: List<String>
)
