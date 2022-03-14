package com.tencent.bkrepo.scanner.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扫描方案信息")
data class ScanPlanInfo(
    @ApiModelProperty("projectId")
    val projectId: String,
    @ApiModelProperty("方案id")
    val id: String,
    @ApiModelProperty("方案类型")
    val planType: String,
    @ApiModelProperty("方案名")
    val name: String?,
    @ApiModelProperty("方案状态")
    val status: String,
    @ApiModelProperty("累计扫描制品数")
    val artifactCount: Int = 0,
    @ApiModelProperty("危急漏洞数")
    val critical: Int = 0,
    @ApiModelProperty("高危漏洞数")
    val high: Int = 0,
    @ApiModelProperty("中危漏洞数")
    val medium: Int = 0,
    @ApiModelProperty("低危漏洞数")
    val low: Int = 0,
    @ApiModelProperty("漏洞总数")
    val total: Int = 0,
    @ApiModelProperty("创建者")
    val createdBy: String,
    @ApiModelProperty("创建时间")
    val createdDate: String,
    @ApiModelProperty("修改者")
    val lastModifiedBy: String,
    @ApiModelProperty("修改时间")
    val lastModifiedDate: String,
    @ApiModelProperty("最后扫描时间")
    val lastScanDate: String?
)