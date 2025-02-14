package com.tencent.bkrepo.common.devops.pojo.accesstoken

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("个人令牌详细信息")
data class DevopsAccessToken(
    @ApiModelProperty("令牌id", required = true)
    val id: String,
    @ApiModelProperty("当前用户", required = true)
    val userId: String,
    @ApiModelProperty("令牌名称", required = true)
    val tokenName: String,
    @ApiModelProperty("令牌token", required = true)
    val personalToken: String,
    @ApiModelProperty("是否删除", required = true)
    val isDelete: Boolean,
    @ApiModelProperty("创建时间", required = true)
    val createTime: String,
    @ApiModelProperty("创建人", required = true)
    val createUser: String,
    @ApiModelProperty("更新时间", required = true)
    val updateTime: String,
    @ApiModelProperty("更新人", required = true)
    val updateUser: String,
    @ApiModelProperty("令牌范围", required = true)
    val tokenScope: List<String> = mutableListOf()
)
