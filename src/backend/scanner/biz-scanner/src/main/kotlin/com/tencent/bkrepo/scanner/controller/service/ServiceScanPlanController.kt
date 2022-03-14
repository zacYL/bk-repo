package com.tencent.bkrepo.scanner.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.scanner.api.ScanPlanClient
import com.tencent.bkrepo.scanner.pojo.context.ArtifactPlanContext
import com.tencent.bkrepo.scanner.service.ScanPlanService
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceScanPlanController(
    private val scanPlanService: ScanPlanService
) : ScanPlanClient {
    override fun artifactPlanStatus(
        projectId: String,
        repoName: String,
        repoType: RepositoryType,
        packageKey: String?,
        version: String?,
        fullPath: String?
    ): Response<String?> {
        return ResponseBuilder.success(
            scanPlanService.artifactPlanStatus(
                ArtifactPlanContext(
                    projectId = projectId,
                    repoName = repoName,
                    repoType = repoType,
                    packageKey = packageKey,
                    version = version,
                    fullPath = fullPath
                )
            )
        )
    }

}
