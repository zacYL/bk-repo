package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.OperateLogClient
import com.tencent.bkrepo.repository.service.log.OperateLogService
import org.springframework.web.bind.annotation.RestController

@RestController
class OperateLogController(
    private val operateLogService: OperateLogService
) : OperateLogClient {
    override fun uploads(projectId: String?, repoName: String?, latestWeek: Boolean?): Response<Long> {
        val count = operateLogService.uploads(projectId, repoName, latestWeek)
        return ResponseBuilder.success(count)
    }

    override fun downloads(projectId: String?, repoName: String?, latestWeek: Boolean?): Response<Long> {
        val count = operateLogService.downloads(projectId, repoName, latestWeek)
        return ResponseBuilder.success(count)
    }
}
