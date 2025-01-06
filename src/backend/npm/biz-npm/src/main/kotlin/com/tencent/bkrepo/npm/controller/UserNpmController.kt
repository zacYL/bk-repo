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

package com.tencent.bkrepo.npm.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.USER_API_PREFIX
import com.tencent.bkrepo.npm.pojo.NpmDomainInfo
import com.tencent.bkrepo.npm.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.npm.service.NpmClientService
import com.tencent.bkrepo.npm.service.NpmWebService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Api("npm 用户接口")
@RequestMapping(USER_API_PREFIX)
@RestController
class UserNpmController(
    private val npmWebService: NpmWebService,
    private val npmClientService: NpmClientService
) {

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @ApiOperation("查询包的版本详情")
    @GetMapping("/version/detail/{projectId}/{repoName}")
    fun detailVersion(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "包版本", required = true)
        @RequestParam version: String
    ): Response<PackageVersionInfo> {
        return ResponseBuilder.success(npmWebService.detailVersion(artifactInfo, packageKey, version))
    }

    @Permission(ResourceType.REPO, PermissionAction.DELETE)
    @ApiOperation("删除仓库下的包")
    @DeleteMapping("/package/delete/{projectId}/{repoName}")
    fun deletePackage(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @ApiParam(value = "包名称", required = true)
        @RequestParam packageKey: String
    ): Response<Void> {
        npmWebService.deletePackage(artifactInfo)
        return ResponseBuilder.success()
    }

    @Permission(ResourceType.REPO, PermissionAction.DELETE)
    @ApiOperation("删除仓库下的包版本")
    @DeleteMapping("/version/delete/{projectId}/{repoName}")
    fun deleteVersion(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @ApiParam(value = "包名称", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "包版本", required = true)
        @RequestParam version: String
    ): Response<Void> {
        npmWebService.deleteVersion(artifactInfo)
        return ResponseBuilder.success()
    }

    @ApiOperation("获取npm域名地址")
    @GetMapping("/address")
    fun getRegistryDomain(): Response<NpmDomainInfo> {
        return ResponseBuilder.success(npmWebService.getRegistryDomain())
    }

    @PostMapping("/upload/{projectId}/{repoName}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(

        @PathVariable
        projectId: String,

        @PathVariable
        repoName: String,

        @RequestPart(value = "file")
        file: MultipartFile

    ): Response<Boolean> {
        npmClientService.upload(projectId, repoName, file)
        return ResponseBuilder.success()
    }

}
