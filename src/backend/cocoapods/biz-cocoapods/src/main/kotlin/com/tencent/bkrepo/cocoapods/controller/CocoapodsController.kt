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
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo.Companion.DOWNLOAD_INDEX_URL
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo.Companion.DOWNLOAD_PACKAGE_URL
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo.Companion.TEST_REMOTE_URL
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo.Companion.UPLOAD_PACKAGE_URL
import com.tencent.bkrepo.cocoapods.service.CocoapodsIndexService
import com.tencent.bkrepo.cocoapods.service.CocoapodsUploadDownloadService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CocoapodsController(
    private val cocoapodsUploadDownloadService: CocoapodsUploadDownloadService,
    private val cocoapodsIndexService: CocoapodsIndexService
) {

    @GetMapping(TEST_REMOTE_URL)
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun repoInfo(@PathVariable projectId: String, @PathVariable repoName: String): ResponseEntity<Void> {
        return ResponseEntity.ok().build<Void>()
    }

    @PutMapping(UPLOAD_PACKAGE_URL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun upload(
    @ArtifactPathVariable cocoapodsArtifactInfo: CocoapodsArtifactInfo,
    artifactFile: ArtifactFile
    ): Response<Void> {
        cocoapodsUploadDownloadService.upload(cocoapodsArtifactInfo, artifactFile)
        return ResponseBuilder.success()
    }

    @GetMapping(DOWNLOAD_INDEX_URL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun downloadIndex(
        @ArtifactPathVariable artifactInfo: ArtifactInfo
    ) {
        cocoapodsIndexService.downloadSpecs(artifactInfo.projectId, artifactInfo.repoName)
    }

    @GetMapping(DOWNLOAD_PACKAGE_URL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun downloadPackage(
        @ArtifactPathVariable cocoapodsArtifactInfo: CocoapodsArtifactInfo
    ){
      cocoapodsUploadDownloadService.downloadPackage(cocoapodsArtifactInfo)
    }
}
