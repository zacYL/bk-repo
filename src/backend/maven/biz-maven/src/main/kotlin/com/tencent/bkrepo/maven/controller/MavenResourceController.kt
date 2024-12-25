/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.maven.controller

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.pojo.request.MavenWebDeployRequest
import com.tencent.bkrepo.maven.service.MavenService
import com.tencent.bkrepo.maven.util.DeployUtils.validate
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class MavenResourceController(
    private val mavenService: MavenService
) {
    @PutMapping(MavenArtifactInfo.MAVEN_MAPPING_URI, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deploy(
        @ArtifactPathVariable mavenArtifactInfo: MavenArtifactInfo,
        file: ArtifactFile
    ) {
        return mavenService.deploy(mavenArtifactInfo, file)
    }

    @PostMapping("/deploy/{projectId}/{repoName}")
    fun verifyDeploy(
        mavenArtifactInfo: MavenArtifactInfo,
        @RequestBody request: MavenWebDeployRequest,
    ) {
        request.validate()
        mavenService.verifyDeploy(mavenArtifactInfo, request)
    }

    @GetMapping(MavenArtifactInfo.MAVEN_MAPPING_URI, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun dependency(@ArtifactPathVariable mavenArtifactInfo: MavenArtifactInfo) {
        mavenService.dependency(mavenArtifactInfo)
    }

    @DeleteMapping(MavenArtifactInfo.MAVEN_MAPPING_URI, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteDependency(@ArtifactPathVariable mavenArtifactInfo: MavenArtifactInfo) {
        mavenService.deleteDependency(mavenArtifactInfo)
    }

    @PostMapping("/pom_gav", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun extractGavFromPom(@RequestPart(value = "file") file: MultipartFile) = ResponseBuilder.success(mavenService.extractGavFromPom(file))

    @PostMapping("/jar_gav", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun extractGavFromJar(@RequestPart(value = "file") file: MultipartFile) = ResponseBuilder.success(mavenService.extractGavFromJar(file))
}
