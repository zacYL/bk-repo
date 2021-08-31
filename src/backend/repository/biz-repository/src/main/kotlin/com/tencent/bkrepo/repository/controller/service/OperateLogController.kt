package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.OperateLogClient
import com.tencent.bkrepo.repository.pojo.event.EventCreateRequest
import com.tencent.bkrepo.repository.pojo.log.OperateLogPojo
import com.tencent.bkrepo.repository.service.log.OperateLogService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

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

    override fun saveEvent(request: EventCreateRequest): Response<Boolean> {
        operateLogService.saveEventRequest(request)
        return ResponseBuilder.success()
    }

    override fun operateLogLimitByTime(time: LocalDateTime, pageNumber: Int, pageSize: Int):
            Response<List<OperateLogPojo>> {
        val result = operateLogService.operateLogLimitByTime(time, pageNumber, pageSize)
        return ResponseBuilder.success(result)
    }
}
