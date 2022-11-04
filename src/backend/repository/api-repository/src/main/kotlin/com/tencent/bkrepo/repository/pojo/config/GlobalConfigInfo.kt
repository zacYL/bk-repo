package com.tencent.bkrepo.repository.pojo.config

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * 全局配置信息
 */
@ApiModel("全局配置信息")
data class GlobalConfigInfo(
    @ApiModelProperty("创建者")
    val createdBy: String,
    @ApiModelProperty("创建时间")
    val createdDate: String,
    @ApiModelProperty("修改者")
    val lastModifiedBy: String,
    @ApiModelProperty("修改时间")
    val lastModifiedDate: String,
    @ApiModelProperty("类型")
    val type: ConfigType,
    @ApiModelProperty("配置")
    val configuration: String
)
