package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.service.packages.PackageRepairService
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Principal(PrincipalType.ADMIN)
@RequestMapping("/api/repair")
class UserPackageRepairController(
    private val packageRepairService: PackageRepairService
) {

    @ApiOperation("修改历史版本")
    @GetMapping("/package/history-version")
    fun repairHistoryVersion(): Response<Void> {
        packageRepairService.repairHistoryVersion()
        return ResponseBuilder.success()
    }

    @ApiOperation("修正包的版本数")
    @PutMapping("/package/version-count")
    fun repairVersionCount(): Response<Void> {
        packageRepairService.repairVersionCount()
        return ResponseBuilder.success()
    }

    @ApiOperation("分发来源的docker版本信息补充manifestPath")
    @Principal(PrincipalType.ADMIN)
    @PutMapping("/version/docker/manifest-path")
    fun repairDockerManifestPath(): Response<Void> {
        packageRepairService.repairDockerManifestPath()
        return ResponseBuilder.success()
    }

    @ApiOperation("修正npm版本artifactPath")
    @PutMapping("/version/npm-artifact-path")
    fun repairNpmArtifactPath(): Response<Map<String, Long>> {
        val resultMap = packageRepairService.repairNpmArtifactPath()
        return ResponseBuilder.success(resultMap)
    }
}
