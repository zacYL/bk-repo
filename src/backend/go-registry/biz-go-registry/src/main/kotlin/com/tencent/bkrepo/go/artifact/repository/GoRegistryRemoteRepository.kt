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

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.util.StreamUtils.readText
import com.tencent.bkrepo.common.api.util.UrlFormatter
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.go.constant.GO_MOD_KEY
import com.tencent.bkrepo.go.constant.README_KEY
import com.tencent.bkrepo.go.pojo.artifact.GoArtifactInfo
import com.tencent.bkrepo.go.pojo.artifact.GoModuleInfo
import com.tencent.bkrepo.go.pojo.artifact.GoVersionListInfo
import com.tencent.bkrepo.go.pojo.artifact.GoVersionMetadataInfo
import com.tencent.bkrepo.go.pojo.enum.GoFileType
import com.tencent.bkrepo.go.pojo.response.GoVersionMetadata
import com.tencent.bkrepo.go.service.GoPackageService
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class GoRegistryRemoteRepository(
    private val goPackageService: GoPackageService
) : RemoteRepository() {

    override fun createRemoteDownloadUrl(context: ArtifactContext): String {
        val configuration = context.getRemoteConfiguration()
        val artifactInfo = context.artifactInfo as GoArtifactInfo
        return UrlFormatter.format(configuration.url, artifactInfo.getRequestPath())
    }

    override fun query(context: ArtifactQueryContext): Any? {
        with(context) {
            getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
        }
        val (cacheNode, isExpired) = getCacheInfo(context) ?: return doRequest(context)
        if (isExpired) doRequest(context)?.let { return it }
        return loadArtifactResource(cacheNode, context)?.getSingleStream()?.use {
            readQueryResponse(context.artifactInfo as GoArtifactInfo, it)
        }
    }

    override fun onQueryResponse(context: ArtifactQueryContext, response: Response): Any? {
        val artifactFile = createTempFile(response.body!!)
        val result: Any = readQueryResponse(context.artifactInfo as GoArtifactInfo, artifactFile.getInputStream())
        cacheArtifactFile(context, artifactFile)
        return result
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
        }
        return getCacheInfo(context)?.run {
            nodeDownloadIntercept(context, first)
            loadArtifactResource(first, context)
        } ?: return doRequest(context) as ArtifactResource?
    }

    override fun onDownloadResponse(
        context: ArtifactDownloadContext,
        response: Response,
        useDisposition: Boolean,
        syncCache: Boolean
    ): ArtifactResource {
        with(context.artifactInfo as GoModuleInfo) {
            val artifactFile = createTempFile(response.body!!)
            val size = artifactFile.getSize()
            val artifactStream = artifactFile.getInputStream().artifactStream(Range.full(size))
            val (mod, readme) = if (type == GoFileType.ZIP && context.getRemoteConfiguration().cache.enabled) {
                 goPackageService.extractModAndReadme(this, artifactFile.getInputStream(), context.storageCredentials)
            } else Pair(null, null)
            val node = cacheArtifactFile(context, artifactFile)
            if (type == GoFileType.ZIP && node != null) {
                val extension = mutableMapOf<String, String>()
                mod?.let { extension[GO_MOD_KEY] = it }
                readme?.let { extension[README_KEY] = it }
                goPackageService.createVersion(
                    artifactInfo = this,
                    size = node.size,
                    sha256 = node.sha256!!,
                    md5 = node.md5!!,
                    extension = extension,
                )
            }
            val responseName = context.artifactInfo.getResponseName()
            val srcRepo = RepositoryIdentify(context.projectId, context.repoName)
            return ArtifactResource(artifactStream, responseName, srcRepo, node, ArtifactChannel.PROXY).apply {
                this.contentType = if (type == GoFileType.ZIP) MediaTypes.APPLICATION_ZIP else MediaTypes.TEXT_PLAIN
            }
        }
    }

    override fun remove(context: ArtifactRemoveContext) {
        goPackageService.remove(context.artifactInfo as GoArtifactInfo, context.userId)
    }

    override fun isExpired(cacheNode: NodeDetail, expiration: Long): Boolean {
        return if (expiration <= 0) true else super.isExpired(cacheNode, expiration)
    }

    override fun buildCacheNodeCreateRequest(context: ArtifactContext, artifactFile: ArtifactFile): NodeCreateRequest {
        val metadataList = (context.artifactInfo as GoArtifactInfo).generateMetadata() +
            MetadataModel(key = SOURCE_TYPE, value = ArtifactChannel.PROXY, system = true)
        return super.buildCacheNodeCreateRequest(context, artifactFile).copy(nodeMetadata = metadataList)
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        return goPackageService.buildDownloadRecord(context.artifactInfo as GoModuleInfo, context.userId)
    }

    private fun readQueryResponse(artifactInfo: GoArtifactInfo, inputStream: InputStream): Any {
        return when (artifactInfo) {
            is GoVersionListInfo -> inputStream.use { it.readText() }.split("\n")
            is GoVersionMetadataInfo -> inputStream.use { it.readJsonString<GoVersionMetadata>() }
            else -> throw UnsupportedOperationException()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoRegistryRemoteRepository::class.java)
    }
}
