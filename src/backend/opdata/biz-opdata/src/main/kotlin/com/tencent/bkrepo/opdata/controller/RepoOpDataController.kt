package com.tencent.bkrepo.opdata.controller

import DEFAULT_PROJECT
import TIMEOUT_LIMIT
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.opdata.pojo.*
import com.tencent.bkrepo.opdata.pojo.response.*
import com.tencent.bkrepo.opdata.service.RepoOpDataService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.OperateLogClient
import com.tencent.bkrepo.repository.api.PackageClient
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@Api("op信息接口")
@RestController
@RequestMapping("/api/op")
class RepoOpDataController(
    private val packageClient: PackageClient,
    private val nodeClient: NodeClient,
    private val repoOpDataService: RepoOpDataService,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val operateLogClient: OperateLogClient
) {

    @ApiOperation("仓库容量详情列表")
    @GetMapping("/repo/capacity")
    fun repoCapacity(
        @ApiParam(required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam(required = false)
        @RequestParam repoName: String?
    ): Response<List<RepoCapacityData>> {
        val repoCapacityData = repoOpDataService.repoCapacityData(projectId, repoName)
        return ResponseBuilder.success(repoCapacityData)
    }

    @ApiOperation("仓库访问详情列表")
    @GetMapping("/repo/visit")
    fun repoVisit(
        @ApiParam(required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam(required = false)
        @RequestParam repoName: String?
    ): Response<List<RepoVisitData>> {
        val repoCapacityData = repoOpDataService.repoVisitData(projectId, repoName)
        return ResponseBuilder.success(repoCapacityData)
    }


    @ApiOperation("制品数量")
    @GetMapping("/packages")
    fun count(
        @ApiParam(required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam(required = false)
        @RequestParam repoName: String?
    ): Response<Long> {
        return packageClient.existArtifact(projectId, repoName)
    }

    @ApiOperation("仓库数量")
    @GetMapping("/repos")
    fun repos(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT
    ): Response<Int> {
        return ResponseBuilder.success(repoOpDataService.repos(projectId))
    }

    @ApiOperation("已用容量")
    @GetMapping("/capacity")
    fun capacity(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?
    ): Response<Long> {
        val valueOperations = redisTemplate.opsForValue()
        var capacity: Long? = (valueOperations.get("capacity") as Int?)?.toLong()
        if (capacity == null) {
            capacity = nodeClient.capacity(projectId, repoName).data!!
            valueOperations.set("capacity", capacity, TIMEOUT_LIMIT, TimeUnit.MINUTES)
        }
        return ResponseBuilder.success(capacity)
    }

    @ApiOperation("符合条件仓库容量排行")
    @GetMapping("/capacities")
    fun capacities(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?,
        @ApiParam("须返回仓库详细数据的数量")
        @RequestParam limit: Int = 5,
        @ApiParam("排序类型")
        @RequestParam sort: SortType
    ): Response<RepoCapacityList> {
        return ResponseBuilder.success(repoOpDataService.repoCapacity(projectId, repoName, limit, sort))
    }

    @ApiOperation("统计上传数")
    @GetMapping("/package/uploads")
    fun uploads(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?
    ): Response<UseMetricResponse> {
        val sum = operateLogClient.uploads(projectId, repoName, null).data ?: 0L
        val latestWeek = operateLogClient.uploads(projectId, repoName, true).data ?: 0L
        return ResponseBuilder.success(
            UseMetricResponse(
                sum = sum,
                latestWeek = latestWeek
            )
        )
    }

    @ApiOperation("统计下载数")
    @GetMapping("/package/downloads")
    fun downloads(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?
    ): Response<UseMetricResponse> {
        val sum = operateLogClient.downloads(projectId, repoName, null).data ?: 0L
        val latestWeek = operateLogClient.downloads(projectId, repoName, true).data ?: 0L
        return ResponseBuilder.success(
            UseMetricResponse(
                sum = sum,
                latestWeek = latestWeek
            )
        )
    }

    @ApiOperation("下载总量")
    @GetMapping("/package/downs")
    fun packageDownsCount(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?
    ): Response<Long> {
        return ResponseBuilder.success(repoOpDataService.downSum(projectId, repoName))
    }

    @ApiOperation("查询每日下载量")
    @GetMapping("/day/downloads")
    fun downloadsByDay(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String?,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?,
        @ApiParam("查询几天内数据，从当天开始，默认7天", required = false)
        @RequestParam days: Long?
    ): Response<DownloadMetrics> {
        TODO("Not yet implemented")
    }

    @ApiOperation("查询每日上传量")
    @GetMapping("/day/uploads")
    fun uploadsByDay(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String?,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?,
        @ApiParam("查询几天内数据，从当天开始，默认7天", required = false)
        @RequestParam days: Long?,
        @ApiParam(required = false)
        @RequestParam cluster: String? = null
    ): Response<UploadMetrics> {
        TODO("Not yet implemented")
    }

    @ApiOperation("仓库类型分布")
    @GetMapping("/repotype")
    fun repoType(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT
    ): Response<RepoTypeSum> {
        return ResponseBuilder.success(repoOpDataService.repoType(projectId))
    }

    @ApiOperation("制品下载排行")
    @GetMapping("/package/download")
    fun packageSortByDowns(
        @ApiParam("项目名", required = true)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名", required = false)
        @RequestParam repoName: String?
    ): Response<ArtifactDownload> {
        val list = repoOpDataService.sortByDownload(projectId, repoName)
        return ResponseBuilder.success(ArtifactDownload(list))
    }
}

