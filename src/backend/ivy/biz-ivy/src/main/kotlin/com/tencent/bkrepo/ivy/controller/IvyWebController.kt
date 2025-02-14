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

package com.tencent.bkrepo.ivy.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.ivy.artifact.IvyArtifactInfo
import com.tencent.bkrepo.ivy.service.IvyService
import com.tencent.bkrepo.ivy.service.impl.IvyWebService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("Ivy 产品接口")
@RequestMapping("/ext")
@RestController
class IvyWebController(
    private val ivyService: IvyService,
    private val ivyWebService: IvyWebService,
) {

    @ApiOperation("ivy jar 包删除接口")
    @DeleteMapping(IvyArtifactInfo.IVY_EXT_PACKAGE_DELETE)
    fun deletePackage(
        @ArtifactPathVariable ivyArtifactInfo: IvyArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String
    ): Response<Void> {
        ivyWebService.deletePackage(ivyArtifactInfo, packageKey)
        return ResponseBuilder.success()
    }

    @ApiOperation("ivy jar 包版本删除接口")
    @DeleteMapping(IvyArtifactInfo.IVY_EXT_VERSION_DELETE)
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    fun deleteVersion(
        @ArtifactPathVariable ivyArtifactInfo: IvyArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "版本号", required = true)
        @RequestParam version: String
    ): Response<Void> {
        ivyWebService.deleteVersion(ivyArtifactInfo, packageKey, version)
        return ResponseBuilder.success()
    }

    @ApiOperation("ivy jar 版本详情接口")
    @GetMapping(IvyArtifactInfo.IVY_EXT_DETAIL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun artifactDetail(
        @ArtifactPathVariable ivyArtifactInfo: IvyArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "版本号", required = true)
        @RequestParam version: String
    ): Response<Any?> {
        return ResponseBuilder.success(ivyWebService.artifactDetail(ivyArtifactInfo, packageKey, version))
    }

}
