package com.tencent.bkrepo.conan.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.service.ConanDeleteService
import com.tencent.bkrepo.conan.service.ConanWebService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("Conan 产品接口")
@RequestMapping("/ext")
@RestController
class ConanWebController(
    private val conanWebService: ConanWebService,
    private val conanDeleteService: ConanDeleteService
) {

    @ApiOperation("包删除接口")
    @DeleteMapping("/package/delete/{projectId}/{repoName}")
    fun deletePackage(
        @ArtifactPathVariable conanArtifactInfo: ConanArtifactInfo,
        @RequestParam packageKey: String
    ): Response<Void> {
//        conanDeleteService.removePackage()
        return ResponseBuilder.success()
    }

    @ApiOperation("包版本删除接口")
    @DeleteMapping("/version/delete/{projectId}/{repoName}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    fun deleteVersion(
        @ArtifactPathVariable conanArtifactInfo: ConanArtifactInfo,
        @RequestParam packageKey: String,
        @RequestParam version: String?
    ): Response<Void> {

        return ResponseBuilder.success()
    }

    @ApiOperation("版本详情接口")
    @GetMapping("/version/detail/{projectId}/{repoName}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun artifactDetail(
        @ArtifactPathVariable conanArtifactInfo: ConanArtifactInfo,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ) = ResponseBuilder.success(conanWebService.artifactDetail(conanArtifactInfo, packageKey, version))

}