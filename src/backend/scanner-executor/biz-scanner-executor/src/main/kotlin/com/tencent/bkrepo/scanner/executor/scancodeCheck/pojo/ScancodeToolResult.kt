package com.tencent.bkrepo.scanner.executor.scancodeCheck.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("scancode 工具扫描结果")
data class ScancodeToolResult(
    @ApiModelProperty("依赖路径")
    val dependentPath:String,
    @ApiModelProperty("许可id")
    val licenseId:String
)