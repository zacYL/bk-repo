package com.tencent.bkrepo.scanner.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("制品漏洞信息")
data class ArtifactLeakInfo(
    @ApiModelProperty("漏洞id")
    val cveId: String,
    @ApiModelProperty("漏洞等级")
    val severity: String,
    @ApiModelProperty("所属依赖")
    val pkgName: String,
    @ApiModelProperty("安装版本")
    val installedVersion: String,
    @ApiModelProperty("漏洞标题")
    val title: String,
    @ApiModelProperty("漏洞描述")
    val description: String?,
    @ApiModelProperty("修复建议")
    val officialSolution: String? = null,
    @ApiModelProperty("相关信息")
    val reference: List<String>? = null
)
