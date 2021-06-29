package com.tencent.bkrepo.repository.pojo.operate

import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.log.ResourceType
import io.swagger.annotations.Api
import org.springframework.boot.autoconfigure.data.RepositoryType
import java.time.LocalDateTime

@Api("操作日志")
data class OperateLogResponse(
    val createdDate: LocalDateTime,
    val resourceType: ResourceType,
    val operateType: String,
    val userId: String,
    val clientAddress: String,
    val result: Boolean,
    val content: Content
) {
    open class Content(
        val projectId: String? = null,
        val repoType: String? = null,
        val resKey: String,
        val des: String? = null
    )

}