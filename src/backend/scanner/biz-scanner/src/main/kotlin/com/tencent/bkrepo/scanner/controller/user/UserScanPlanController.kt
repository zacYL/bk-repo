package com.tencent.bkrepo.scanner.controller.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.scanner.pojo.ArtifactRelationPlan
import com.tencent.bkrepo.scanner.pojo.ScanArtifactInfo
import com.tencent.bkrepo.scanner.pojo.ScanPlanBase
import com.tencent.bkrepo.scanner.pojo.ScanPlanInfo
import com.tencent.bkrepo.scanner.pojo.context.ArtifactPlanContext
import com.tencent.bkrepo.scanner.pojo.context.PlanArtifactContext
import com.tencent.bkrepo.scanner.pojo.enums.LeakType
import com.tencent.bkrepo.scanner.pojo.enums.PlanType
import com.tencent.bkrepo.scanner.pojo.enums.ScanStatus
import com.tencent.bkrepo.scanner.pojo.request.ScanPlanRequest
import com.tencent.bkrepo.scanner.service.ScanPlanService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/scan/plan")
class UserScanPlanController(
    private val scanPlanService: ScanPlanService
) {

    @ApiOperation("创建扫描方案")
    @PostMapping("/create")
    fun createScanPlan(
        @RequestAttribute userId: String,
        @RequestBody request: ScanPlanRequest
    ): Response<Boolean> {
        return ResponseBuilder.success(scanPlanService.createScanPlan(userId, request))
    }

    @ApiOperation("查询扫描方案基础信息")
    @GetMapping("/detail/{projectId}/{id}")
    fun getScanPlan(
        @ApiParam(value = "projectId")
        @PathVariable projectId: String,
        @ApiParam(value = "方案id")
        @PathVariable id: String
    ): Response<ScanPlanBase?> {
        return ResponseBuilder.success(scanPlanService.getScanPlanBase(projectId, id))
    }

    @ApiOperation("删除扫描方案")
    @DeleteMapping("/delete/{projectId}/{id}")
    fun deleteScanPlan(
        @RequestAttribute userId: String,
        @ApiParam(value = "projectId")
        @PathVariable projectId: String,
        @ApiParam(value = "方案id")
        @PathVariable id: String
    ): Response<Boolean> {
        return ResponseBuilder.success(scanPlanService.updateStatus(userId, projectId, id))
    }

    @ApiOperation("更新扫描方案")
    @PostMapping("/update")
    fun updateScanPlan(
        @RequestAttribute userId: String,
        @RequestBody request: ScanPlanRequest
    ): Response<Boolean> {
        return ResponseBuilder.success(scanPlanService.updateScanPlan(userId, request))
    }

    @ApiOperation("扫描方案列表-分页")
    @GetMapping("/list/{projectId}")
    fun scanPlanList(
        @ApiParam(value = "projectId", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "方案类型(DEPENDENT/MOBILE)")
        @RequestParam type: PlanType?,
        @ApiParam(value = "方案名")
        @RequestParam name: String?,
        @ApiParam("页数", required = false, defaultValue = "1")
        @RequestParam pageNumber: Int?,
        @ApiParam("每页数量", required = false, defaultValue = "20")
        @RequestParam pageSize: Int?
    ): Response<Page<ScanPlanInfo?>> {
        val page = scanPlanService.scanPlanList(
            projectId = projectId,
            type = type,
            name = name,
            pageNumber = pageNumber ?: 1,
            pageSize = pageSize ?: 20
        )
        return ResponseBuilder.success(page)
    }

    @ApiOperation("所有扫描方案")
    @GetMapping("/all/{projectId}")
    fun scanPlanList(
        @ApiParam(value = "projectId", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "方案类型(DEPENDENT/MOBILE)")
        @RequestParam type: PlanType?
    ): Response<List<ScanPlanBase>> {
        return ResponseBuilder.success(scanPlanService.scanPlanList(projectId, type))
    }

    @ApiOperation("方案详情-统计数据")
    @GetMapping("/count/{projectId}/{id}")
    fun planDetailCount(
        @ApiParam(value = "projectId")
        @PathVariable projectId: String,
        @ApiParam(value = "方案id")
        @PathVariable id: String
    ): Response<ScanPlanInfo?> {
        return ResponseBuilder.success(scanPlanService.countInfo(projectId, id))
    }

    @ApiOperation("方案详情-制品信息")
    @GetMapping("/artifact")
    fun planArtifactList(
        @ApiParam(value = "项目id", required = true)
        @RequestParam projectId: String,
        @ApiParam(value = "方案id", required = true)
        @RequestParam id: String,
        @ApiParam(value = "制品名称", required = false)
        @RequestParam name: String?,
        @ApiParam(value = "最高漏洞等级", required = false)
        @RequestParam highestLeakLevel: LeakType?,
        @ApiParam(value = "仓库类型", required = false)
        @RequestParam repoType: RepositoryType?,
        @ApiParam(value = "仓库名, required = false")
        @RequestParam repoName: String?,
        @ApiParam(value = "扫描状态, required = false")
        @RequestParam status: ScanStatus?,
        @ApiParam(value = "扫描开始时间, required = false")
        @RequestParam startTime: String?,
        @ApiParam(value = "扫描结束时间, required = false")
        @RequestParam endTime: String?,
        @ApiParam("页数", required = false, defaultValue = "1")
        @RequestParam pageNumber: Int?,
        @ApiParam("每页数量", required = false, defaultValue = "20")
        @RequestParam pageSize: Int?
    ): Response<Page<ScanArtifactInfo?>> {
        return ResponseBuilder.success(
            scanPlanService.planArtifactList(
                PlanArtifactContext(
                    projectId = projectId,
                    planId = id,
                    artifactName = name,
                    highestLeakLevel = highestLeakLevel?.name,
                    repoType = repoType?.name,
                    repoName = repoName,
                    status = status?.name,
                    startTime = startTime,
                    endTime = endTime,
                    pageNumber = pageNumber ?: 1,
                    pageSize = pageSize ?: 20
                )
            )
        )
    }

    @ApiOperation("文件/包关联的扫描方案列表")
    @GetMapping("/relation/artifact/{projectId}")
    fun artifactPlanList(
        @ApiParam(value = "projectId", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "repoName", required = true)
        @RequestParam repoName: String,
        @ApiParam(value = "repoType", required = true)
        @RequestParam repoType: RepositoryType,
        @ApiParam(value = "packageKey")
        @RequestParam packageKey: String?,
        @ApiParam(value = "version")
        @RequestParam version: String?,
        @ApiParam(value = "fullPath")
        @RequestParam fullPath: String?
    ): Response<List<ArtifactRelationPlan>?> {
        return ResponseBuilder.success(
            scanPlanService.artifactPlanList(
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
