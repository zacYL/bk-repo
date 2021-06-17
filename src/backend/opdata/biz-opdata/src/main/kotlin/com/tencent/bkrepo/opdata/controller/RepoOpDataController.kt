package com.tencent.bkrepo.opdata.controller

import DEFAULT_PROJECT
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.opdata.pojo.RepoTypeSum
import com.tencent.bkrepo.opdata.pojo.SortType
import com.tencent.bkrepo.opdata.pojo.ArtifactDownload
import com.tencent.bkrepo.opdata.pojo.RepoCapacityList
import com.tencent.bkrepo.opdata.service.RepoOpDataService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("op信息接口")
@RestController
@RequestMapping("/api/op")
class RepoOpDataController(
    private val packageClient: PackageClient,
    private val nodeClient: NodeClient,
    private val repoOpDataService: RepoOpDataService
) {

    @ApiOperation("制品数量")
    @GetMapping("/packages")
    fun count(
        @ApiParam(required = false)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam(required = false)
        @RequestParam repoName: String?
    ): Response<Long> {
        return packageClient.existArtifact(projectId, repoName)
    }

    @ApiOperation("仓库数量")
    @GetMapping("/repos")
    fun repos(
        @ApiParam("项目名", required = false)
        @RequestParam projectId: String = DEFAULT_PROJECT
    ): Response<Int> {
        return ResponseBuilder.success(repoOpDataService.repos(projectId))
    }

    @ApiOperation("已用容量")
    @GetMapping("/capacity")
    fun capacity(
        @ApiParam("项目名", required = false)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?
    ): Response<Long> {
        return nodeClient.capacity(projectId, repoName)
    }

    @ApiOperation("符合条件仓库容量排行")
    @GetMapping("/capacities")
    fun capacities(
        @ApiParam("项目名", required = false)
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

    //todo 访问数
    //todo 本周访问数

    //todo 上传数
    //todo 本周上传数

    @ApiOperation("下载总量")
    @GetMapping("/package/downs")
    fun packageDownsCount(
        @ApiParam("项目名", required = false)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名，可为空", required = false)
        @RequestParam repoName: String?
    ): Response<Long> {
        return ResponseBuilder.success(repoOpDataService.downSum(projectId, repoName))
    }

    //todo 本自然周下载量

    //todo 下载趋势

    //todo 上传趋势

    //todo 访问趋势


    @ApiOperation("仓库类型分布")
    @GetMapping("/repotype")
    fun repoType(
        @ApiParam("项目名", required = false)
        @RequestParam projectId: String = DEFAULT_PROJECT
    ): Response<RepoTypeSum> {
        return ResponseBuilder.success(repoOpDataService.repoType(projectId))
    }

    @ApiOperation("制品下载排行")
    @GetMapping("/package/download")
    fun packageSortByDowns(
        @ApiParam("项目名", required = false)
        @RequestParam projectId: String = DEFAULT_PROJECT,
        @ApiParam("仓库名", required = false)
        @RequestParam repoName: String?
    ): Response<ArtifactDownload> {
        val list = repoOpDataService.sortByDownload(projectId, repoName)
        return ResponseBuilder.success(ArtifactDownload(list))
    }

}
