package com.tencent.bkrepo.scanner.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("制品统计信息")
data class ArtifactCountInfo(
    @ApiModelProperty("扫描记录id")
    val recordId: String,
    @ApiModelProperty("制品名")
    val name: String,
    @ApiModelProperty("packageKey")
    val packageKey: String? = null,
    @ApiModelProperty("版本")
    val version: String? = null,
    @ApiModelProperty("路径")
    val fullPath: String? = null,
    @ApiModelProperty("仓库类型")
    val repoType: String,
    @ApiModelProperty("仓库名")
    val repoName: String,
    @ApiModelProperty("最高漏洞等级")
    val highestLeakLevel: String? = null,
    @ApiModelProperty("扫描时长")
    val duration: Long,
    @ApiModelProperty("完成时间")
    val finishTime: LocalDateTime,
    @ApiModelProperty("状态")
    val status: String,
    @ApiModelProperty("危急漏洞数")
    val critical: Int = 0,
    @ApiModelProperty("高危漏洞数")
    val high: Int = 0,
    @ApiModelProperty("中危漏洞数")
    val medium: Int = 0,
    @ApiModelProperty("低危漏洞数")
    val low: Int = 0,
    @ApiModelProperty("漏洞总数")
    val total: Int = 0
)
