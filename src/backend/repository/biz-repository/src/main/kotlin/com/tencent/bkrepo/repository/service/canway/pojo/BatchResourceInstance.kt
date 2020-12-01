package com.tencent.bkrepo.repository.service.canway.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量注册资源模型")
data class BatchResourceInstance(
        @ApiModelProperty("创建人", required = true)
        val userId: String?,
        @ApiModelProperty("资源标识", required = true)
        val resourceCode: String,
        @ApiModelProperty("资源所属标识")
        val belongCode: String,
        @ApiModelProperty("资源所属实例")
        val belongInstance: String = "",
        @ApiModelProperty("实例", required = true)
        val instances: List<Instance>
) {
    @ApiModel("实例")
    data class Instance(
            @ApiModelProperty("实例标识", required = true)
            val instanceCode: String,
            @ApiModelProperty("实例名称", required = true)
            val instanceName: String,
            @ApiModelProperty("父资源实例标识")
            val parentInstance:String?
    )
}