package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

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

}