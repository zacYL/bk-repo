package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.DayMetricClient
import com.tencent.bkrepo.repository.pojo.bksoftware.DownloadMetric
import com.tencent.bkrepo.repository.pojo.bksoftware.UploadMetric
import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.service.bksoftware.DayMetricService
import org.springframework.web.bind.annotation.RestController

@RestController
class DayMetricController(
    private val dayMetricService: DayMetricService
) : DayMetricClient {

    override fun listByDownload(
        projectId: String?,
        repoName: String?,
        days: Long?
    ): Response<DownloadMetric> {
        val downloadMetric = dayMetricService.list(projectId, repoName, days ?: 7L, arrayOf(OperateType.DOWNLOAD))
        return ResponseBuilder.success(DownloadMetric(downloadMetric))
    }

    override fun listByUpload(
        projectId: String?,
        repoName: String?,
        days: Long?
    ): Response<UploadMetric> {
        val uploadMetric = dayMetricService.list(
            projectId, repoName, days ?: 7L,
            arrayOf(OperateType.CREATE, OperateType.UPDATE)
        )
        return ResponseBuilder.success(UploadMetric(uploadMetric))
    }
}
