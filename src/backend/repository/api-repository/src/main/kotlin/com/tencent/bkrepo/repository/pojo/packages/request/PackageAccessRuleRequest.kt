package com.tencent.bkrepo.repository.pojo.packages.request

import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.VersionRuleType
import io.swagger.annotations.ApiModelProperty

data class PackageAccessRuleRequest(
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("包类型")
    val packageType: PackageType,
    @ApiModelProperty("包key")
    val key: String,
    @ApiModelProperty("版本号")
    val version: String? = null,
    @ApiModelProperty("版本关系")
    val versionRuleType: VersionRuleType? = null,
    @ApiModelProperty("规则行为")
    val pass: Boolean
)
