/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.go.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.go.pojo.artifact.GoArtifactInfo
import com.tencent.bkrepo.go.pojo.response.PackageVersionInfo
import com.tencent.bkrepo.go.service.GoExtService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("UnusedParameter")
@RequestMapping("/ext", produces = [MediaTypes.APPLICATION_JSON])
@RestController
class GoExtController(
    private val goExtService: GoExtService
) {

    @ApiOperation("获取go registry域名地址")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/address")
    fun getRegistryDomain(): Response<String> {
        return ResponseBuilder.success(goExtService.getRegistryDomain())
    }

    @ApiOperation("删除go module所有版本")
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    @DeleteMapping("/package/delete/{projectId}/{repoName}")
    fun deletePackage(
        artifactInfo: GoArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String
    ): Response<Void> {
        goExtService.delete(artifactInfo)
        return ResponseBuilder.success()
    }

    @ApiOperation("删除go module指定版本")
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    @DeleteMapping("/version/delete/{projectId}/{repoName}")
    fun deleteVersion(
        artifactInfo: GoArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "版本号", required = true)
        @RequestParam version: String
    ): Response<Void> {
        goExtService.delete(artifactInfo)
        return ResponseBuilder.success()
    }

    @ApiOperation("查询go module版本详情")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping("/version/detail/{projectId}/{repoName}")
    fun getVersionDetail(
        artifactInfo: GoArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "版本号", required = true)
        @RequestParam version: String
    ): Response<PackageVersionInfo> {
        return ResponseBuilder.success(goExtService.getVersionDetail(artifactInfo))
    }

    @ApiOperation("下载客户端")
    @Principal(PrincipalType.GENERAL)
    @GetMapping("/cli/download/{os}/{arch}")
    fun downloadClient(
        @PathVariable os: String,
        @PathVariable arch: String
    ) {
        goExtService.downloadClient(os, arch)
    }

    @ApiOperation("客户端文件列表")
    @Principal(PrincipalType.ADMIN)
    @GetMapping("/cli/list")
    fun listClient(): Response<List<String>> {
        return ResponseBuilder.success(goExtService.listClient())
    }
}
