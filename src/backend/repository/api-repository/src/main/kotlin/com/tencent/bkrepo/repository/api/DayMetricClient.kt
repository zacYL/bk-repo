package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import com.tencent.bkrepo.repository.pojo.bksoftware.DownloadMetric
import com.tencent.bkrepo.repository.pojo.bksoftware.UploadMetric

@Api("每日下载量")
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "DayMetricClient")
@Primary
@RequestMapping("/day")
interface DayMetricClient {
    @ApiOperation("查询每日下载量")
    @GetMapping("/download")
    fun listByDownload(
        @ApiParam("项目名", required = false)
        @RequestParam projectId: String?,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?,
        @ApiParam("查询几天内数据，从当天开始，默认7天", required = false)
        @RequestParam days: Long?
    ): Response<DownloadMetric>

    @ApiOperation("查询每日上传量")
    @GetMapping("/upload")
    fun listByUpload(
        @ApiParam("项目名", required = false)
        @RequestParam projectId: String?,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?,
        @ApiParam("查询几天内数据，从当天开始，默认7天", required = false)
        @RequestParam days: Long?
    ): Response<UploadMetric>
}
