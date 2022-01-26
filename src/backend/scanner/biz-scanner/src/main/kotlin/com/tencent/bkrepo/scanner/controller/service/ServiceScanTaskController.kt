package com.tencent.bkrepo.scanner.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.scanner.api.ScanTaskClient
import com.tencent.bkrepo.scanner.service.ScanTaskService
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceScanTaskController(
    private val scanTaskService: ScanTaskService
) : ScanTaskClient {
    override fun createTask(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        sha256: String
    ): Response<Boolean> {
        return ResponseBuilder.success(
            scanTaskService.createTask(
                projectId,
                repoName,
                packageKey,
                version,
                sha256
            )
        )
    }
}
