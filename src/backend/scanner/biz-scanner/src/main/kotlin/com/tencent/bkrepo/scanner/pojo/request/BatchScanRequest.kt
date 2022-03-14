package com.tencent.bkrepo.scanner.pojo.request

import com.tencent.bkrepo.scanner.pojo.enums.TriggerType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("方案批量扫描请求")
data class BatchScanRequest(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("方案ID")
    val id: String,
    @ApiModelProperty("触发方式")
    val triggerMethod: TriggerType = TriggerType.MANUAL,
    //空：扫描所有制品仓库
    //非空：扫描指定仓库
    @ApiModelProperty("仓库名")
    val repoNameList: List<String> = emptyList(),
    //空：扫描所选制品仓库内所有制品包的最新版本
    //非空：扫描满足规则的制品
    @ApiModelProperty("制品规则")
    val artifactRules: List<ArtifactRule> = emptyList()
)
