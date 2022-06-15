package com.tencent.bkrepo.scanner.pojo.license

import io.swagger.annotations.ApiModelProperty

data class UpdateLicenseRequest(
    @ApiModelProperty("是否可信")
    val isTrust: Boolean? = null,
    @ApiModelProperty("风险等级")
    val risk: String? = null
)
