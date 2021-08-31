package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.repository.pojo.metric.PackageDetail
import io.swagger.annotations.ApiOperation
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDateTime

@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "PackageStatisticsClient")
@RequestMapping("/service/statistic")
interface PackageStatisticsClient {

    @ApiOperation("查询出全部制品数量")
    @GetMapping("/package/count")
    fun packageTotal(
        @RequestParam projectId: String?,
        @RequestParam repoName: String?
    ): Response<Long>

    @ApiOperation("查询在指定时间之后有更新操作的制品")
    @GetMapping("/package/limit/modified")
    fun packageModifiedLimitByTime(
        @RequestParam time: LocalDateTime,
        @RequestParam pageNumber: Int,
        @RequestParam pageSize: Int
    ): Response<List<PackageDetail?>>

    @ApiOperation("制品下载总量")
    @GetMapping("/package/downs")
    fun packageDownloadSum(
        @RequestParam projectId: String?,
        @RequestParam repoName: String?
    ): Response<Long>
}
