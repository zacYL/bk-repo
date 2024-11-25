/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.cocoapods.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.cocoapods.constant.USER_API_PREFIX
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.service.CocoapodsWebService
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("Cocoapods 产品接口")
@RequestMapping(USER_API_PREFIX)
@RestController
class CocoapodsWebController(
    val cocoapodsWebService: CocoapodsWebService
) {

    @ApiOperation("包删除接口")
    @DeleteMapping("/package/delete/{projectId}/{repoName}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    fun deletePackage(
        @ArtifactPathVariable cocoapodsArtifactInfo: CocoapodsArtifactInfo,
        @RequestParam packageKey: String
    ): Response<Void> {
        cocoapodsWebService.deletePackage(cocoapodsArtifactInfo,packageKey);
        return ResponseBuilder.success()
    }

    @ApiOperation("包版本删除接口")
    @DeleteMapping("/version/delete/{projectId}/{repoName}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    fun deleteVersion(
        @ArtifactPathVariable cocoapodsArtifactInfo: CocoapodsArtifactInfo,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ): Response<Void> {
        cocoapodsWebService.deleteVersion(cocoapodsArtifactInfo, packageKey, version)
        return ResponseBuilder.success()
    }

    @ApiOperation("版本详情接口")
    @GetMapping("/version/detail/{projectId}/{repoName}")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun artifactDetail(
        @ArtifactPathVariable cocoapodsArtifactInfo: CocoapodsArtifactInfo,
        @RequestParam packageKey: String,
        @RequestParam version: String
    ) = ResponseBuilder.success(cocoapodsWebService.artifactDetail(cocoapodsArtifactInfo, packageKey, version))

}
