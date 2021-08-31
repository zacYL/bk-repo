package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.PackageStatisticsClient
import com.tencent.bkrepo.repository.pojo.metric.PackageDetail
import com.tencent.bkrepo.repository.service.packages.PackageStatisticsService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class PackageStatisticsController(
    private val packageStatisticsService: PackageStatisticsService
) : PackageStatisticsClient {
    override fun packageTotal(projectId: String?, repoName: String?): Response<Long> {
        return ResponseBuilder.success(packageStatisticsService.packageTotal(projectId, repoName))
    }

    override fun packageDownloadSum(projectId: String?, repoName: String?): Response<Long> {
        return ResponseBuilder.success(packageStatisticsService.packageDownloadSum(projectId, repoName))
    }

    override fun packageModifiedLimitByTime(
        time: LocalDateTime,
        pageNumber: Int,
        pageSize: Int
    ): Response<List<PackageDetail?>> {
        return ResponseBuilder.success(packageStatisticsService.packageModifiedLimitByTime(time, pageNumber, pageSize))
    }
}
