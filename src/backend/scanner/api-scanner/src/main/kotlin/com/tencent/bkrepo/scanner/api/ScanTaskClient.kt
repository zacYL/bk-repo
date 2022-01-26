package com.tencent.bkrepo.scanner.api

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.scanner.SCANNER_SERVICE_NAME
import io.swagger.annotations.Api
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * 扫描任务接口
 */
@Api("扫描任务接口")
@Primary
@FeignClient(SCANNER_SERVICE_NAME, contextId = "")
@RequestMapping("/service/task")
interface ScanTaskClient {

    @PostMapping("/{projectId}")
    fun createTask(
        @PathVariable projectId: String,
        @RequestParam repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String,
        @RequestParam sha256: String
    ): Response<Boolean>
}
