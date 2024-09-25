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

package com.tencent.bkrepo.go.service

import com.tencent.bkrepo.common.api.constant.HttpHeaders.USER_AGENT
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.info.InfoService
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.go.constant.HEADER_CLI_LATEST
import com.tencent.bkrepo.go.constant.HEADER_VERSION
import com.tencent.bkrepo.go.constant.LATEST
import com.tencent.bkrepo.go.exception.GoVersionListNotFoundException
import com.tencent.bkrepo.go.exception.GoVersionMetadataNotFoundException
import com.tencent.bkrepo.go.pojo.artifact.GoModuleInfo
import com.tencent.bkrepo.go.pojo.artifact.GoVersionListInfo
import com.tencent.bkrepo.go.pojo.artifact.GoVersionMetadataInfo
import com.tencent.bkrepo.go.pojo.response.GoRegistrySummary
import com.tencent.bkrepo.go.pojo.response.GoVersionMetadata
import com.tencent.bkrepo.go.util.GoUtils
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GoResourceService(
    private val packageClient: PackageClient,
    private val repositoryClient: RepositoryClient,
    private val infoService: InfoService
) : ArtifactService() {

    fun info(artifactInfo: DefaultArtifactInfo): GoRegistrySummary {
        with(artifactInfo) {
            val response = HttpContextHolder.getResponse()
            response.setHeader(HEADER_VERSION, infoService.version() ?: "1.0.0")
            GoUtils.getNewerClientVersion(HttpContextHolder.getRequest().getHeader(USER_AGENT))?.let {
                response.setHeader(HEADER_CLI_LATEST, it)
            }
            val count = packageClient.getPackageCount(projectId, repoName).data ?: 0
            val repoInfo = repositoryClient.getRepoDetail(projectId, repoName).data
                ?: throw RepoNotFoundException(artifactInfo.getRepoIdentify())
            return GoRegistrySummary(projectId, repoName, RepositoryType.GO, repoInfo.category, repoInfo.public, count)
        }
    }

    fun getVersionInfo(artifactInfo: GoVersionMetadataInfo): GoVersionMetadata {
        with(artifactInfo) {
            val version = getArtifactVersion() ?: LATEST
            logger.info("user[${SecurityUtils.getPrincipal()}] query version metadata of [${getModuleId()}]")
            return repository.query(ArtifactQueryContext()) as GoVersionMetadata?
                ?: throw GoVersionMetadataNotFoundException(modulePath, version, artifactInfo.getRepoIdentify())
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun listVersions(artifactInfo: GoVersionListInfo): String {
        with(artifactInfo) {
            logger.info("user[${SecurityUtils.getPrincipal()}] query version list of [${getModuleId()}]")
            return (repository.query(ArtifactQueryContext()) as List<String>?)?.joinToString("\n")
                ?: throw GoVersionListNotFoundException(modulePath, getRepoIdentify())
        }
    }

    fun download(artifactInfo: GoModuleInfo) {
        logger.info("user[${SecurityUtils.getPrincipal()}] download file [${artifactInfo.getArtifactFullPath()}]")
        repository.download(ArtifactDownloadContext())
    }

    fun upload(artifactInfo: GoModuleInfo, artifactFile: ArtifactFile) {
        repository.upload(ArtifactUploadContext(artifactFile))
        logger.info(
            "user[${SecurityUtils.getPrincipal()}] publish module[${artifactInfo.getModuleId()}]" +
                    " to ${artifactInfo.getRepoIdentify()} successfully"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoResourceService::class.java)
    }
}
