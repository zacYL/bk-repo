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

package com.tencent.bkrepo.npm.artifact.repository

import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_JSON_WITHOUT_CHARSET
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toCompactJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.migration.MigrateDetail
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_METADATA_ROOT
import com.tencent.bkrepo.npm.constants.PACKAGE_JSON
import com.tencent.bkrepo.npm.constants.REQUEST_URI
import com.tencent.bkrepo.npm.constants.TARBALL_FULL_PATH
import com.tencent.bkrepo.npm.exception.NpmBadRequestException
import com.tencent.bkrepo.npm.handler.NpmPackageHandler
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.NpmSearchInfoMap
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.service.NpmOperationService
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodesDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component

@Component
class NpmRemoteRepository(
    private val executor: ThreadPoolTaskExecutor,
    private val npmPackageHandler: NpmPackageHandler,
    private val npmOperationService: NpmOperationService
) : RemoteRepository() {

    override fun packageVersion(context: ArtifactContext?, node: NodeDetail?): PackageVersion? {
        requireNotNull(context)
        return npmOperationService.packageVersion(context)
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        downloadIntercept(context, null)
        return super.onDownload(context)
    }

    override fun onDownloadSuccess(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
        throughput: Throughput,
    ) {
        with(context) {
            val packageInfo = NpmUtils.parseNameAndVersionFromFullPath(artifactInfo.getArtifactFullPath())
            val versionMetadataFullPath = NpmUtils.getVersionPackageMetadataPath(packageInfo.first, packageInfo.second)
            val packageKey = NpmUtils.packageKeyByRepoType(packageInfo.first)
            val pkgVersion = packageClient.findVersionByName(projectId, repoName, packageKey, packageInfo.second).data
            if (pkgVersion == null) {
                // 存储package-version.json文件并创建版本信息
                val queryContext = ArtifactQueryContext(repo, artifactInfo)
                queryContext.putAttribute(NPM_FILE_FULL_PATH, versionMetadataFullPath)
                queryContext.putAttribute(REQUEST_URI, "/${packageInfo.first}/${packageInfo.second}")
                executor.execute {
                    query(queryContext)?.use {
                        it.readJsonString<NpmVersionMetadata>()
                    } ?: run {
                        // 如果package-version.json没有,则从package json中获取
                        getVersionMetadataFromPackage(context, packageInfo).also {
                            if (it != null) {
                                // 保存package-version.json文件
                                storageVersionMetadata(context, it, versionMetadataFullPath)
                            }
                        }
                    }?.let {
                        val size = artifactResource.getTotalSize()
                        npmPackageHandler.createVersion(userId, artifactInfo, it, size,
                            ohpm = repositoryDetail.type == RepositoryType.OHPM
                        )
                    }
                    super.onDownloadSuccess(this, artifactResource, throughput)
                }
            } else {
                super.onDownloadSuccess(this, artifactResource, throughput)
            }
        }
    }

    private fun storageVersionMetadata(
        context: ArtifactDownloadContext,
        metadata: NpmVersionMetadata,
        versionMetadataFullPath: String,
    ) {
        val specArtifact =
            ArtifactFileFactory.build(
                metadata.toJsonString().toByteArray().inputStream(),
                context.repositoryDetail.storageCredentials
            )
        val nodeCreateRequest = NodeCreateRequest(
            projectId = context.projectId,
            repoName = context.repoName,
            fullPath = versionMetadataFullPath,
            folder = false,
            size = specArtifact.getSize(),
            sha256 = specArtifact.getFileSha256(),
            md5 = specArtifact.getFileMd5(),
            overwrite = true,
            operator = context.userId
        )
        storageManager.storeArtifactFile(nodeCreateRequest, specArtifact, null)
    }

    private fun getVersionMetadataFromPackage(
        context: ArtifactDownloadContext,
        packageInfo: Pair<String, String>
    ): NpmVersionMetadata? {
        with(context) {
            val pkgMetadataFullPath = NpmUtils.getPackageMetadataPath(packageInfo.first)
            val originMetadataNode = nodeClient.getNodeDetail(projectId, repoName, pkgMetadataFullPath).data
                ?: throw NodeNotFoundException("$projectId/$repoName/$pkgMetadataFullPath")
            return storageManager.loadArtifactInputStream(
                originMetadataNode, repositoryDetail.storageCredentials
            ).use {
                JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java)
            }.versions.map[packageInfo.second]
        }
    }

    override fun upload(context: ArtifactUploadContext) {
        with(context) {
            val message = "Unable to upload npm package into a remote repository [$projectId/$repoName]"
            logger.warn(message)
            throw NpmBadRequestException(message)
        }
    }

    override fun query(context: ArtifactQueryContext): ArtifactInputStream? {
        val fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)!!
        if (ArtifactContextHolder.getUrlPath(this::javaClass.name)?.startsWith("/ext/") != true) {
            context.getFullPathInterceptors().forEach { it.intercept(context.projectId, fullPath) }
        }
        return getCacheArtifactResource(context)?.getSingleStream()
            ?: super.query(context) as ArtifactInputStream?
            ?: if (context.getStringAttribute(NPM_FILE_FULL_PATH)?.endsWith("/$PACKAGE_JSON") == true) {
                findCacheNodeDetail(context)?.let { loadArtifactResource(it, context) }?.getSingleStream()
            } else {
                null
            }
    }

    override fun checkQueryResponse(response: Response): Boolean {
        return super.checkQueryResponse(response) && run {
            val contentType = response.body()!!.contentType()
            contentType.toString().contains(APPLICATION_JSON_WITHOUT_CHARSET) || run {
                logger.warn("Content-Type($contentType) of response from [${response.request().url()}] is unsupported")
                false
            }
        }
    }

    // 仅package.json文件有必要在缓存过期后更新
    override fun getCacheArtifactResource(context: ArtifactContext): ArtifactResource? {
        return when (context) {
            is ArtifactDownloadContext -> findCacheNodeDetail(context)?.let { loadArtifactResource(it, context) }
            is ArtifactQueryContext -> {
                if (context.getStringAttribute(NPM_FILE_FULL_PATH)?.endsWith("/$PACKAGE_JSON") == false) {
                    findCacheNodeDetail(context)?.let { loadArtifactResource(it, context) }
                } else if (context.getRemoteConfiguration().cache.expiration > 0) {
                    super.getCacheArtifactResource(context)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    override fun findCacheNodeDetail(context: ArtifactContext): NodeDetail? {
        val fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)!!
        with(context) {
            return nodeClient.getNodeDetail(projectId, repoName, fullPath).data
        }
    }

    override fun createRemoteDownloadUrl(context: ArtifactContext): String {
        val configuration = context.getRemoteConfiguration()
        val requestURI = context.getStringAttribute(REQUEST_URI)
        val artifactUri = requestURI ?: context.artifactInfo.getArtifactFullPath()
        val queryString = context.request.queryString
        return UrlFormatter.format(configuration.url, artifactUri, queryString)
    }

    override fun onQueryResponse(context: ArtifactQueryContext, response: Response): ArtifactInputStream? {
        val tempFile = createTempFile(response.body()!!)
        val packageMetaData = tempFile.getInputStream().readJsonString<NpmPackageMetaData>()
        val artifactFile = ArtifactFileFactory.build(packageMetaData.toCompactJsonString().byteInputStream())
        val size = artifactFile.getSize()
        val stream = artifactFile.getInputStream().artifactStream(Range.full(size))
        cacheArtifactFile(context, artifactFile)
        return stream
    }

    override fun buildCacheNodeCreateRequest(context: ArtifactContext, artifactFile: ArtifactFile): NodeCreateRequest {
        return NodeCreateRequest(
                projectId = context.repositoryDetail.projectId,
                repoName = context.repositoryDetail.name,
                folder = false,
                fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)!!,
                size = artifactFile.getSize(),
                sha256 = artifactFile.getFileSha256(),
                md5 = artifactFile.getFileMd5(),
                overwrite = true,
                operator = context.userId,
                nodeMetadata = listOf(MetadataModel(SOURCE_TYPE, ArtifactChannel.PROXY))
        )
    }

    override fun onSearchResponse(context: ArtifactSearchContext, response: Response): List<NpmSearchInfoMap> {
        val npmSearchResponse =
            JsonUtils.objectMapper.readValue(response.body()!!.byteStream(), NpmSearchResponse::class.java)
        return npmSearchResponse.objects
    }

    override fun migrate(context: ArtifactMigrateContext): MigrateDetail {
        with(context) {
            val message = "Unable to migrate npm package info a remote repository [$projectId/$repoName]"
            logger.warn(message)
            throw NpmBadRequestException(message)
        }
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
    ): PackageDownloadRecord? {
        with(context) {
            val packageInfo = NpmUtils.parseNameAndVersionFromFullPath(artifactInfo.getArtifactFullPath())
            with(packageInfo) {
                return PackageDownloadRecord(
                    projectId,
                    repoName,
                    NpmUtils.packageKeyByRepoType(first, context.repo.type),
                    second,
                    userId
                )
            }
        }
    }

    override fun remove(context: ArtifactRemoveContext) {
        with(context) {
            val npmArtifactInfo = artifactInfo as NpmArtifactInfo
            val fullPaths = if (npmArtifactInfo.version == null) {
                listOf("$NPM_METADATA_ROOT/${npmArtifactInfo.packageName}", "/${npmArtifactInfo.packageName}")
            } else {
                listOf(
                    getStringAttribute(TARBALL_FULL_PATH)!!,
                    NpmUtils.getVersionPackageMetadataPath(npmArtifactInfo.packageName, npmArtifactInfo.version!!)
                )
            }
            nodeClient.deleteNodes(NodesDeleteRequest(projectId, repoName, fullPaths, userId))
            logger.info("delete artifact $fullPaths success in repo [$projectId/$repoName].")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NpmRemoteRepository::class.java)
    }
}
