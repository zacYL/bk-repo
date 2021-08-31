package com.tencent.bkrepo.repository.pojo.log

import com.tencent.bkrepo.common.api.event.base.EventType
import io.swagger.annotations.Api
import java.time.LocalDateTime

@Api("操作日志")
data class OperateLogPojo(
    val createdDate: LocalDateTime,
    val type: EventType,
    val projectId: String?,
    val repoName: String?,
    val resourceKey: String,
    val userId: String,
    val clientAddress: String,
    val description: Map<String, Any>
)
