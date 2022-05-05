package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.metric.PackageDownloadCount
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
    @ApiOperation("查询项目数据统计总览--时间段内版本上传下载数和相关用户数")
    @GetMapping("/summary")
    fun queryStatisticsSummary(
        @RequestParam projectId: String,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") fromDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") toDate: LocalDate
    ): Response<ProjectStatisticsSummary> {
        return ResponseBuilder.success(projectStatisticsService.querySummary(projectId, fromDate, toDate))
    }

    @ApiOperation("查询时间段内制品包(包含所有版本)下载量排行")
    @GetMapping("/download/rank")
    fun queryPackageDownloadRank(
        @RequestParam projectId: String,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") fromDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") toDate: LocalDate
    ): Response<List<PackageDownloadCount>> {
        return ResponseBuilder.success(projectStatisticsService.queryPackageDownloadRank(projectId, fromDate, toDate))
    }
}
