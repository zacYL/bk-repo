package com.tencent.bkrepo.scanner.pojo.request

import com.tencent.bkrepo.scanner.pojo.enums.PlanType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扫描方案请求")
data class ScanPlanRequest(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("方案ID")
    val id: String?,
    @ApiModelProperty("方案名称")
    val name: String?,
    @ApiModelProperty("方案类型")
    val type: PlanType?,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("是否自动扫描")
    val autoScan: Boolean?,
    @ApiModelProperty("自动扫描仓库")
    val repoNameList: List<String>?,
    @ApiModelProperty("自动扫描规则")
    val artifactRules: List<ArtifactRule>?
)
