/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.maven.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.pojo.request.PackageVersionMoveCopyRequest
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.artifact.MavenDeleteArtifactInfo
import com.tencent.bkrepo.maven.pojo.MavenArtifactVersionData
import com.tencent.bkrepo.maven.pojo.MavenDependency
import com.tencent.bkrepo.maven.pojo.MavenPlugin
import com.tencent.bkrepo.maven.pojo.MavenVersionDependentsRelation
import com.tencent.bkrepo.maven.pojo.response.MavenGAVCResponse
import com.tencent.bkrepo.maven.service.MavenExtService
import com.tencent.bkrepo.maven.service.MavenService
import io.swagger.annotations.Api
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
import javax.validation.constraints.Min

@Api("Maven 产品接口")
@RequestMapping("/ext")
@RestController
class MavenWebController(
    private val mavenService: MavenService,
    private val mavenExtService: MavenExtService
) {

    @ApiOperation("maven jar 包删除接口")
    @DeleteMapping(MavenArtifactInfo.MAVEN_EXT_PACKAGE_DELETE)
    fun deletePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable mavenArtifactInfo: MavenDeleteArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String
    ): Response<Void> {
        mavenService.deletePackage(userId, mavenArtifactInfo)
        return ResponseBuilder.success()
    }

    @ApiOperation("maven jar 包版本删除接口")
    @DeleteMapping(MavenArtifactInfo.MAVEN_EXT_VERSION_DELETE)
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    fun deleteVersion(
        @RequestAttribute userId: String,
        @ArtifactPathVariable mavenArtifactInfo: MavenDeleteArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "版本号", required = true)
        @RequestParam version: String
    ): Response<Void> {
        mavenService.deleteVersion(userId, mavenArtifactInfo)
        return ResponseBuilder.success()
    }

    @ApiOperation("maven jar 版本详情接口")
    @GetMapping(MavenArtifactInfo.MAVEN_EXT_DETAIL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun artifactDetail(
        @RequestAttribute userId: String,
        @ArtifactPathVariable mavenArtifactInfo: MavenArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "版本号", required = true)
        @RequestParam version: String
    ): Response<MavenArtifactVersionData> {
        return ResponseBuilder.success(mavenService.getVersionDetail(userId, mavenArtifactInfo))
    }

    @ApiOperation("maven gavc 搜索接口")
    @GetMapping("/search/gavc/{projectId}/{pageNumber}/{pageSize}")
    fun gavc(
        @PathVariable projectId: String,
        @PathVariable pageNumber: Int,
        @PathVariable pageSize: Int,
        @RequestParam g: String?,
        @RequestParam a: String?,
        @RequestParam v: String?,
        @RequestParam c: String?,
        @RequestParam repos: String?
    ): Response<Page<MavenGAVCResponse.UriResult>> {
        return mavenExtService.gavc(projectId, pageNumber, pageSize, g, a, v, c, repos)
    }

    @ApiOperation("查询包的依赖项")
    @GetMapping("/dependencies/{projectId}/{repoName}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun dependencies(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String,
        @RequestParam @Min(1) pageNumber: Int? = 1,
        @RequestParam @Min(1) pageSize: Int? = 20
    ): Response<Page<MavenDependency>> {
        return mavenExtService.dependencies(
            projectId,
            repoName,
            packageKey,
            version,
            pageNumber ?: 1,
            pageSize ?: 20
        )
    }

    @ApiOperation("查询仓库中依赖该制品的制品")
    @GetMapping("/dependencies/reverse/{projectId}/{repoName}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun dependenciesReverse(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String,
        @RequestParam @Min(1) pageNumber: Int? = 1,
        @RequestParam @Min(1) pageSize: Int? = 20
    ): Response<Page<MavenVersionDependentsRelation>> {
        return mavenExtService.dependenciesReverse(
            projectId,
            repoName,
            packageKey,
            version,
            pageNumber ?: PAGE_NUMBER,
            pageSize ?: PAGE_SIZE
        )
    }

    @ApiOperation("查询包依赖的插件")
    @GetMapping("/plugins/{projectId}/{repoName}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun plugins(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageKey: String,
        @RequestParam version: String,
        @RequestParam @Min(1) pageNumber: Int? = 1,
        @RequestParam @Min(1) pageSize: Int? = 20
    ): Response<Page<MavenPlugin>> {
        return mavenExtService.plugins(
            projectId,
            repoName,
            packageKey,
            version,
            pageNumber ?: PAGE_NUMBER,
            pageSize ?: PAGE_SIZE
        )
    }

    @ApiOperation("移动包版本")
    @PostMapping("/version/move")
    fun moveVersion(
        @RequestBody request: PackageVersionMoveCopyRequest,
    ): Response<Void> {
        mavenService.moveCopyVersion(request, true)
        return ResponseBuilder.success()
    }

    @ApiOperation("复制包版本")
    @PostMapping("/version/copy")
    fun copyVersion(
        @RequestBody request: PackageVersionMoveCopyRequest,
    ): Response<Void> {
        mavenService.moveCopyVersion(request, false)
        return ResponseBuilder.success()
    }

    companion object {
        private const val PAGE_NUMBER = 1
        private const val PAGE_SIZE = 20
    }
}
