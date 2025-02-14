package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.metric.NodeDownloadCount
import com.tencent.bkrepo.repository.pojo.metric.PackageDownloadCount
import com.tencent.bkrepo.repository.pojo.node.NodeStatisticsSummary
import com.tencent.bkrepo.repository.pojo.project.ProjectStatisticsSummary
import com.tencent.bkrepo.repository.service.project.ProjectStatisticsService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Api("项目数据统计用户接口")
@RestController
@RequestMapping("/api/project/statistics")
class UserProjectStatisticsController(
    private val projectStatisticsService: ProjectStatisticsService
) {
    @ApiOperation("查询时间段内依赖包版本上传下载数和相关用户数")
    @GetMapping("/summary")
    fun queryVersionStatisticsSummary(
        @RequestParam projectId: String,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") fromDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") toDate: LocalDate
    ): Response<ProjectStatisticsSummary> {
        return ResponseBuilder.success(projectStatisticsService.queryVersionSummary(projectId, fromDate, toDate))
    }

    @ApiOperation("查询时间段内依赖包(包含所有版本)下载量排行")
    @GetMapping("/download/rank")
    fun queryPackageDownloadRank(
        @RequestParam projectId: String,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") fromDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") toDate: LocalDate
    ): Response<List<PackageDownloadCount>> {
        return ResponseBuilder.success(projectStatisticsService.queryPackageDownloadRank(projectId, fromDate, toDate))
    }

    @ApiOperation("查询时间段内通用文件上传下载数和相关用户数")
    @GetMapping("/node/summary")
    fun queryNodeStatisticsSummary(
        @RequestParam projectId: String,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") fromDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") toDate: LocalDate
    ): Response<NodeStatisticsSummary> {
        return ResponseBuilder.success(projectStatisticsService.queryNodeSummary(projectId, fromDate, toDate))
    }

    @ApiOperation("查询时间段内通用文件下载量排行")
    @GetMapping("/node/download/rank")
    fun queryNodeDownloadRank(
        @RequestParam projectId: String,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") fromDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") toDate: LocalDate
    ): Response<List<NodeDownloadCount>> {
        return ResponseBuilder.success(projectStatisticsService.queryNodeDownloadRank(projectId, fromDate, toDate))
    }
}
