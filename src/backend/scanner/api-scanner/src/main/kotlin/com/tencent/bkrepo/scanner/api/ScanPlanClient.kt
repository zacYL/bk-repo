package com.tencent.bkrepo.scanner.api

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.scanner.SCANNER_SERVICE_NAME
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api("扫描方案接口")
@Primary
@FeignClient(SCANNER_SERVICE_NAME, contextId = "ScanPlanClient")
@RequestMapping("/service/plan")
interface ScanPlanClient {

    @ApiOperation("制品关联扫描方案状态")
    @GetMapping("/artifact/status/{projectId}")
    fun artifactPlanStatus(
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
    ): Response<String?>

}
