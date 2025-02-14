package net.canway.devops.auth.pojo.resource.action

import io.swagger.annotations.ApiModelProperty

class ResourceActionVO(
    @ApiModelProperty("id")
    val id: String,
    @ApiModelProperty("动作名称")
    val name: String,
    @ApiModelProperty("动作英文名称")
    val englishName: String,
    @ApiModelProperty("资源动作码")
    val actionCode: String,
    @ApiModelProperty("资源码")
    val resourceCode: String,
    @ApiModelProperty("等级,project、tenant、system")
    val parentResourceCode: String,
    @ApiModelProperty("是否关联实例")
    val associateInstance: Boolean,
    @ApiModelProperty("权重")
    val weight: Int,
    @ApiModelProperty("模块")
    val module: String
)
