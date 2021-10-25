package com.tencent.bkrepo.repository.pojo.log

import io.swagger.annotations.Api
import jdk.management.resource.ResourceType
import java.time.LocalDateTime

@Api("操作日志")
data class OperateLogPojo(
    val createdDate: LocalDateTime,
    val resourceType: ResourceType,
    val resourceKey: String,
    val operateType: OperateType,
    val userId: String,
    val clientAddress: String,
    val result: Boolean,
    val map: Map<String, Any>
)
