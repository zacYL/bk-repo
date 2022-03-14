package com.tencent.bkrepo.scanner.pojo

import com.tencent.bkrepo.scanner.pojo.request.ArtifactRule
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扫描方案基础信息")
data class ScanPlanBase(
    @ApiModelProperty("方案id")
    val id: String,
    @ApiModelProperty("方案名")
    val name: String?,
    @ApiModelProperty("方案类型")
    val type: String,
    @ApiModelProperty("描述")
    val description: String? = "",
    @ApiModelProperty("projectId")
    val projectId: String,
    @ApiModelProperty("是否开启自动扫描")
    val autoScan: Boolean,
    @ApiModelProperty("自动扫描仓库")
    val repoNameList: List<String> = emptyList(),
    @ApiModelProperty("自动扫描规则")
    val artifactRules: List<ArtifactRule> = emptyList(),

    @ApiModelProperty("创建者")
    val createdBy: String,
    @ApiModelProperty("创建时间")
    val createdDate: String,
    @ApiModelProperty("修改者")
    val lastModifiedBy: String,
    @ApiModelProperty("修改时间")
    val lastModifiedDate: String
)