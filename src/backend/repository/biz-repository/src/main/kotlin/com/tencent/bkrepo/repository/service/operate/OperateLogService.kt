package com.tencent.bkrepo.repository.service.operate

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.pojo.operate.OperateLogPojo
import com.tencent.bkrepo.repository.pojo.log.ResourceType
import com.tencent.bkrepo.repository.pojo.operate.OperateLogResponse


interface OperateLogService {
    fun page(
        type: ResourceType?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<OperateLogResponse>
}