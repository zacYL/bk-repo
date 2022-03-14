package com.tencent.bkrepo.scanner.controller.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.scanner.pojo.ArtifactCountInfo
import com.tencent.bkrepo.scanner.pojo.ArtifactLeakInfo
import com.tencent.bkrepo.scanner.pojo.enums.LeakType
import com.tencent.bkrepo.scanner.pojo.request.AtomScanRequest
import com.tencent.bkrepo.scanner.pojo.request.BatchScanRequest
import com.tencent.bkrepo.scanner.pojo.request.SingleScanRequest
import com.tencent.bkrepo.scanner.service.ScanService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/scan")
class UserScanController(
    private val scanService: ScanService
) {

    @ApiOperation("批量扫描")
    @PostMapping("/batch")
    fun batchScan(
        @RequestAttribute userId: String,
        @RequestBody request: BatchScanRequest
    ): Response<Boolean> {
        return ResponseBuilder.success(scanService.batchScan(userId, request))
    }

    @ApiOperation("自动扫描")
    @PostMapping("/atom")
    fun atomScan(
        @RequestAttribute userId: String,
        @RequestBody request: AtomScanRequest
    ): Response<Boolean> {
        scanService.atomScan(userId, request)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("单个制品扫描")
    @PostMapping("/single")
    fun singleScan(
        @RequestAttribute userId: String,
        @RequestBody request: SingleScanRequest
    ): Response<Boolean> {
        return ResponseBuilder.success(scanService.singleScan(userId, request))
    }

    @ApiOperation("中止制品扫描")
    @PostMapping("/{projectId}/stop")
    fun stopScan(
        @RequestAttribute userId: String,
        @ApiParam(value = "projectId")
        @PathVariable projectId: String,
        @ApiParam(value = "记录id")
        @RequestParam recordId: String
    ): Response<Boolean> {
        return ResponseBuilder.success(scanService.stopScan(userId, projectId, recordId))
    }

    @ApiOperation("制品详情--统计数据")
    @GetMapping("/artifact/count/{projectId}/{recordId}")
    fun artifactCount(
        @ApiParam(value = "projectId")
        @PathVariable projectId: String,
        @ApiParam(value = "记录id")
        @PathVariable recordId: String
    ): Response<ArtifactCountInfo> {
        return ResponseBuilder.success(scanService.artifactCount(projectId, recordId))
    }

    @ApiOperation("制品详情--漏洞数据")
    @GetMapping("/artifact/leak/{projectId}/{recordId}")
    fun artifactLeak(
        @ApiParam(value = "projectId")
        @PathVariable projectId: String,
        @ApiParam(value = "扫描记录id")
        @PathVariable recordId: String,
        @ApiParam("cveId", required = false)
        @RequestParam cveId: String?,
        @ApiParam("漏洞等级", required = false)
        @RequestParam leakType: LeakType?,
        @ApiParam("页数", required = false, defaultValue = "1")
        @RequestParam pageNumber: Int?,
        @ApiParam("每页数量", required = false, defaultValue = "20")
        @RequestParam pageSize: Int?
    ): Response<Page<ArtifactLeakInfo?>> {
        return ResponseBuilder.success(
            scanService.artifactLeak(
                projectId = projectId,
                recordId = recordId,
                cveId = cveId,
                leakType = leakType,
                pageNumber = pageNumber ?: 1,
                pageSize = pageSize ?: 20
            )
        )
    }

}
