package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.repository.pojo.event.EventCreateRequest
import com.tencent.bkrepo.repository.pojo.log.OperateLogPojo
import io.swagger.annotations.Api
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.time.LocalDateTime

@Api("操作日志统计接口")
@Primary
@RequestMapping("/service/operate")
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "OperateLogClient")
interface OperateLogClient {
    @GetMapping("/uploads")
    fun uploads(
        @RequestParam projectId: String?,
        @RequestParam repoName: String?,
        @RequestParam latestWeek: Boolean?
    ): Response<Long>

    @GetMapping("/downloads")
    fun downloads(
        @RequestParam projectId: String?,
        @RequestParam repoName: String?,
        @RequestParam latestWeek: Boolean?
    ): Response<Long>

    @PostMapping("/event")
    fun saveEvent(
        @RequestBody request: EventCreateRequest
    ): Response<Boolean>

    @GetMapping("/log/time/limit")
    fun operateLogLimitByTime(
        @RequestParam time: LocalDateTime,
        @RequestParam pageNumber: Int,
        @RequestParam pageSize: Int
    ): Response<List<OperateLogPojo>>
}
