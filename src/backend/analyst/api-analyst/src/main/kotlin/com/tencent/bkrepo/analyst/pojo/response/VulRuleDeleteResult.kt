package com.tencent.bkrepo.analyst.pojo.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量删除漏洞黑白名单条目结果")
data class VulRuleDeleteResult(
    @ApiModelProperty("成功删除数量")
    val count: Long
)
