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

package com.tencent.bkrepo.go.artifact.repository

import com.tencent.bkrepo.common.api.constant.HttpHeaders.USER_AGENT
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.artifact.exception.VersionConflictException
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.go.constant.GO_MOD_KEY
import com.tencent.bkrepo.go.constant.HEADER_CLI_LATEST
import com.tencent.bkrepo.go.constant.HEADER_OVERWRITE
import com.tencent.bkrepo.go.constant.HEADER_PUBLIC
import com.tencent.bkrepo.go.constant.HEADER_UPLOAD_VALIDATE
import com.tencent.bkrepo.go.constant.README_KEY
import com.tencent.bkrepo.go.pojo.artifact.GoArtifactInfo
import com.tencent.bkrepo.go.pojo.artifact.GoModuleInfo
import com.tencent.bkrepo.go.pojo.artifact.GoVersionListInfo
import com.tencent.bkrepo.go.pojo.artifact.GoVersionMetadataInfo
import com.tencent.bkrepo.go.pojo.enum.GoFileType
import com.tencent.bkrepo.go.pojo.response.GoVersionMetadata
import com.tencent.bkrepo.go.service.GoPackageService
import com.tencent.bkrepo.go.util.GoUtils
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class GoRegistryLocalRepository(
    private val goPackageService: GoPackageService
) : LocalRepository() {

    override fun query(context: ArtifactQueryContext): Any? {
        with(context) {
            context.getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
        }
        return when (val artifactInfo = context.artifactInfo) {
            is GoVersionListInfo -> {
                with(artifactInfo) {
                    packageService
                        .listAllVersion(projectId, repoName, getPackageKey(), VersionListOption())
                        .map { it.name }
                }
            }
            is GoVersionMetadataInfo -> {
                with(artifactInfo) {
                    val version = getArtifactVersion()
                    val versionInfo = if (version == null) {
                        packageService.findLatestBySemVer(projectId, repoName, getPackageKey())
                            ?: throw PackageNotFoundException(modulePath)
                    } else {
                        packageService.findVersionByName(projectId, repoName, getPackageKey(), version)
                            ?: throw VersionNotFoundException(version)
                    }
                    GoVersionMetadata(
                        version = versionInfo.name,
                        time = versionInfo.createdDate.atZone(ZoneId.systemDefault())
                            .withZoneSameInstant(ZoneOffset.UTC).format(DATA_TIME_FORMATTER)
                    )
                }
            }
            else -> throw UnsupportedOperationException()
        }
    }

    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        with(context) {
            context.getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
        }
        super.onDownloadBefore(context)
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            val artifactInfo = artifactInfo as GoModuleInfo
            var node = ArtifactContextHolder.getNodeDetail(artifactInfo)
            if (node == null && artifactInfo.type == GoFileType.MOD) {
                val zipFullPath = artifactInfo.getArtifactFullPathByType(GoFileType.ZIP)
                val zipNode = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, zipFullPath))
                storageManager.loadArtifactInputStream(zipNode, context.storageCredentials)?.use {
                    node = goPackageService.extractMod(artifactInfo, it, context.storageCredentials)
                }
            }
            if (node != null) nodeDownloadIntercept(context, node!!)
            val inputStream = storageManager.loadArtifactInputStream(node, storageCredentials) ?: return null
            val responseName = artifactInfo.getResponseName()
            val srcRepo = RepositoryIdentify(projectId, repoName)
            return ArtifactResource(inputStream, responseName, srcRepo, node, ArtifactChannel.LOCAL, useDisposition)
        }
    }

    override fun onUploadBefore(context: ArtifactUploadContext) {
        with(context.artifactInfo as GoModuleInfo) {
            // TODO: 重构上传拦截器后新增覆盖上传拦截器
            packageService.findVersionByName(projectId, repoName, getPackageKey(), getArtifactVersion())?.let {
                if (
                    context.request.getHeader(HEADER_OVERWRITE) != "true" &&
                    nodeService.checkExist(ArtifactInfo(projectId, repoName, getArtifactFullPath())) != false
                ) {
                    throw VersionConflictException(modulePath, getArtifactVersion())
                }
                uploadIntercept(context, it)
            }
            if (HeaderUtils.getHeader(HEADER_UPLOAD_VALIDATE) != "off") {
                validate(this, context.getArtifactFile())
            }
        }
        super.onUploadBefore(context)
    }

    override fun onUploadSuccess(context: ArtifactUploadContext) {
        with(context) {
            val artifactInfo = artifactInfo as GoModuleInfo
            if (artifactInfo.type == GoFileType.ZIP) {
                val (mod, readme) = goPackageService.extractModAndReadme(
                    artifactInfo = artifactInfo,
                    inputStream = getArtifactFile().getInputStream(),
                    credentials = storageCredentials
                )
                val extension = mutableMapOf<String, String>()
                mod?.let { extension[GO_MOD_KEY] = it }
                readme?.let { extension[README_KEY] = it }
                goPackageService.createVersion(
                    artifactInfo = artifactInfo,
                    size = getArtifactFile().getSize(),
                    sha256 = getArtifactSha256(),
                    md5 = getArtifactMd5(),
                    extension = extension
                )
            }
            if (repositoryDetail.public) {
                response.setHeader(HEADER_PUBLIC, "true")
            }
            response.status = HttpStatus.CREATED.value
            GoUtils.getNewerClientVersion(HttpContextHolder.getRequest().getHeader(USER_AGENT))?.let {
                response.setHeader(HEADER_CLI_LATEST, it)
            }
        }
        super.onUploadSuccess(context)
    }

    override fun remove(context: ArtifactRemoveContext) {
        goPackageService.remove(context.artifactInfo as GoArtifactInfo, context.userId)
    }

    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        return super.buildNodeCreateRequest(context).copy(
            nodeMetadata = (context.artifactInfo as GoArtifactInfo).generateMetadata(),
            overwrite = true
        )
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        return goPackageService.buildDownloadRecord(context.artifactInfo as GoModuleInfo, context.userId)
    }

    override fun getArtifactFullPaths(
        projectId: String,
        repoName: String,
        key: String,
        version: String,
        manifestPath: String?,
        artifactPath: String?
    ): List<String> {
        require(artifactPath != null)
        return listOf(artifactPath.substringBeforeLast('/'))
    }

    // TODO: 上传校验
    private fun validate(artifactInfo: GoModuleInfo, artifactFile: ArtifactFile) {
        return
    }

    companion object {
        private val DATA_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        private val logger = LoggerFactory.getLogger(GoRegistryLocalRepository::class.java)
    }
}
