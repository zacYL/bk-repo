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

package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.api.constant.HttpHeaders.USER_AGENT
import com.tencent.bkrepo.common.api.constant.ensurePrefix
import com.tencent.bkrepo.common.api.constant.ensureSuffix
import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.util.StreamUtils.readText
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.info.InfoService
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.pypi.artifact.PypiSimpleArtifactInfo
import com.tencent.bkrepo.pypi.artifact.xml.XmlConvertUtil
import com.tencent.bkrepo.pypi.constants.ARTIFACT_LIST
import com.tencent.bkrepo.pypi.constants.FIELD_NAME
import com.tencent.bkrepo.pypi.constants.FIELD_VERSION
import com.tencent.bkrepo.pypi.constants.METADATA
import com.tencent.bkrepo.pypi.constants.NAME
import com.tencent.bkrepo.pypi.constants.VERSION
import com.tencent.bkrepo.pypi.constants.XML_RPC_URI
import com.tencent.bkrepo.pypi.exception.PypiRemoteSearchException
import com.tencent.bkrepo.pypi.pojo.Basic
import com.tencent.bkrepo.pypi.pojo.PypiArtifactVersionData
import com.tencent.bkrepo.pypi.pojo.PypiMetadata
import com.tencent.bkrepo.pypi.util.DecompressUtil.getPypiMetadata
import com.tencent.bkrepo.pypi.util.XmlUtils.readXml
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PypiRemoteRepository(
    private val infoService: InfoService
) : RemoteRepository() {

    override fun createRemoteDownloadUrl(context: ArtifactContext): String {
        val url = context.getRemoteConfiguration().url
        return when (context) {
            is ArtifactDownloadContext -> {
                val artifactUri = context.artifactInfo.getArtifactFullPath()
                url.trimEnd('/').removeSuffix("/simple") + "/packages$artifactUri"
            }
            is ArtifactQueryContext -> {
                url.trimEnd('/').ensureSuffix("/simple") +
                        "/${context.artifactInfo.getArtifactName()}".ensureSuffix("/")
            }
            else -> throw MethodNotAllowedException()
        }
    }

    override fun query(context: ArtifactQueryContext): Any? {
        return when (val artifactInfo = context.artifactInfo) {
            is PypiSimpleArtifactInfo -> {
                context.getFullPathInterceptors()
                    .forEach { it.intercept(context.projectId, artifactInfo.getArtifactFullPath()) }
                val artifactResponse = getCacheInfo(context)?.let {
                    if (it.second) remoteQuery(context) ?: loadArtifactResource(it.first, context)
                    else loadArtifactResource(it.first, context)
                } ?: remoteQuery(context)
                artifactResponse?.getSingleStream()?.readText()
                    ?: throw NotFoundException(ArtifactMessageCode.ARTIFACT_DATA_NOT_FOUND)
            }
            else -> getVersionDetail(context)
        }
    }

    private fun remoteQuery(context: ArtifactQueryContext): ArtifactResource? {
        return try {
            val response = doRequest(context)
            if (checkQueryResponse(response)) {
                onQueryResponse(context, response)
            } else null
        } catch (ignore: Exception) {
            logger.warn("Failed to request or resolve response", ignore)
            null
        }
    }

    override fun onQueryResponse(context: ArtifactQueryContext, response: Response): ArtifactResource? {
        val artifactFile = createTempFile(response.body()!!)
        val size = artifactFile.getSize()
        val artifactStream = artifactFile.getInputStream().artifactStream(Range.full(size))
        val node = cacheArtifactFile(context, artifactFile)
        val responseName = context.artifactInfo.getResponseName()
        val srcRepo = RepositoryIdentify(context.projectId, context.repoName)
        return ArtifactResource(artifactStream, responseName, srcRepo, node, ArtifactChannel.PROXY)
    }

    override fun packageVersion(context: ArtifactContext?, node: NodeDetail?): Pair<String, PackageVersion>? {
        requireNotNull(context)
        requireNotNull(node)
        with(context) {
            val packageName = node.metadata[NAME]?.toString()
            val packageVersion = node.metadata[VERSION]?.toString()
            if (packageName == null || packageVersion == null) return null
            val packageKey = PackageKeys.ofPypi(packageName)
            return packageClient.findVersionByName(projectId, repoName, packageKey, packageVersion).data
                ?.let { Pair(packageKey, it) }
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
        }
        return getCacheInfo(context)?.run { loadArtifactResource(first, context) } ?: run {
            val response = doRequest(context)
            return if (checkResponse(response)) {
                onDownloadResponse(context, response)
            } else null
        }
    }

    override fun findCacheNodeDetail(context: ArtifactContext): NodeDetail? {
        return super.findCacheNodeDetail(context)?.also {
            if (context is ArtifactDownloadContext) {
                downloadIntercept(context, it)
            }
        }
    }

    /**
     */
    override fun search(context: ArtifactSearchContext): List<Any> {
        val xmlString = context.request.reader.readXml()
        val remoteConfiguration = context.getRemoteConfiguration()
        val okHttpClient: OkHttpClient = createHttpClient(remoteConfiguration)
        val body = RequestBody.create(MediaType.parse("text/xml"), xmlString)
        val build: Request = Request.Builder().url("${remoteConfiguration.url}$XML_RPC_URI")
            .addHeader("Connection", "keep-alive")
            .post(body)
            .build()
        val htmlContent: String = okHttpClient.newCall(build).execute().body()?.string()
            ?: throw PypiRemoteSearchException("search from ${remoteConfiguration.url} error")
        val methodResponse = XmlConvertUtil.xml2MethodResponse(htmlContent)
        return methodResponse.params.paramList[0].value.array?.data?.valueList ?: mutableListOf()
    }

    override fun onDownloadResponse(context: ArtifactDownloadContext, response: Response): ArtifactResource {
        val artifactFile = createTempFile(response.body()!!)
        val fileName = context.artifactInfo.getResponseName()
        val pypiMetadata = try {
            val metadataString = artifactFile.getInputStream().getPypiMetadata(fileName)
            resolveMetadata(metadataString)?.also { context.putAttribute(METADATA, it) }
        } catch (ignore: Exception) {
            logger.warn("Cannot resolve pypi package metadata of [$fileName]", ignore)
            null
        }
        val name = pypiMetadata?.name
        val version = pypiMetadata?.version
        if (name != null && version != null) {
            val packageKey = PackageKeys.ofPypi(name)
            packageClient.findVersionByName(context.projectId, context.repoName, packageKey, version).data?.let {
                packageDownloadIntercept(context, packageKey, it)
            }
        }
        val size = artifactFile.getSize()
        val artifactStream = artifactFile.getInputStream().artifactStream(Range.full(size))
        val node = cacheArtifactFile(context, artifactFile)
        return ArtifactResource(
            artifactStream,
            context.artifactInfo.getResponseName(),
            RepositoryIdentify(context.projectId, context.repoName),
            node,
            ArtifactChannel.PROXY,
            context.useDisposition
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun onDownloadSuccess(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
        throughput: Throughput
    ) {
        context.getAttribute<PypiMetadata>(METADATA)?.run {
            val fullPath = context.artifactInfo.getArtifactFullPath()
            val packageKey = PackageKeys.ofPypi(name)
            val existVersion = packageClient.findVersionByName(
                projectId = context.projectId,
                repoName = context.repoName,
                packageKey = packageKey,
                version = version
            ).data
            val packageMetadata = existVersion?.packageMetadata?.toMutableList() ?: mutableListOf()
            val artifactListMetadata = packageMetadata.find { it.key == ARTIFACT_LIST }
            val artifactList = artifactListMetadata?.value as? MutableList<String> ?: mutableListOf()
            artifactList.add(fullPath)
            if (artifactListMetadata == null) {
                packageMetadata.add(MetadataModel(key = ARTIFACT_LIST, value = artifactList))
            } else {
                artifactListMetadata.value = artifactList
            }
            packageClient.createVersion(
                PackageVersionCreateRequest(
                    projectId = context.projectId,
                    repoName = context.repoName,
                    packageName = name,
                    packageKey = packageKey,
                    packageType = PackageType.PYPI,
                    versionName = version,
                    size = artifactResource.getTotalSize(),
                    artifactPath = fullPath,
                    packageMetadata = packageMetadata,
                    overwrite = true,
                    createdBy = context.userId
                ),
                HttpContextHolder.getClientAddress()
            )
        }
        super.onDownloadSuccess(context, artifactResource, throughput)
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        with(context) {
            val node = artifactResource.node
                ?: nodeClient.getNodeDetail(projectId, repoName, artifactInfo.getArtifactFullPath()).data
            val metadata = node?.nodeMetadata
            var name: String? = null
            var version: String? = null
            metadata?.forEach {
                if (it.key == NAME) {
                    name = it.value.toString()
                } else if (it.key == VERSION) {
                    version = it.value.toString()
                }
            }
            return if (name != null && version != null) {
                val packageKey = PackageKeys.ofPypi(name!!)
                return PackageDownloadRecord(projectId, repoName, packageKey, version!!, userId)
            } else {
                null
            }
        }
    }

    override fun buildCacheNodeCreateRequest(context: ArtifactContext, artifactFile: ArtifactFile): NodeCreateRequest {
        val nodeCreateRequest = super.buildCacheNodeCreateRequest(context, artifactFile)
        val metadataList = nodeCreateRequest.nodeMetadata?.toMutableList() ?: mutableListOf()
        context.getAttribute<PypiMetadata>(METADATA)?.run {
            metadataList.add(MetadataModel(key = PypiMetadata::name.name, value = name))
            metadataList.add(MetadataModel(key = PypiMetadata::version.name, value = version))
            return nodeCreateRequest.copy(nodeMetadata = metadataList)
        }
        return nodeCreateRequest
    }

    override fun remove(context: ArtifactRemoveContext) {
        val packageKey = HttpContextHolder.getRequest().getParameter("packageKey")
        val version = HttpContextHolder.getRequest().getParameter("version")
        if (version.isNullOrBlank()) {
            // 删除包
            val versionList = packageClient.listAllVersion(
                projectId = context.projectId,
                repoName = context.repoName,
                packageKey = packageKey
            ).data.takeUnless { it.isNullOrEmpty() } ?: run {
                logger.warn("Remove pypi package: Cannot find any version of package [$packageKey]")
                return
            }
            versionList.forEach {
                deletePypiVersion(context, it, packageKey)
            }
        } else {
            // 删除版本
            val packageVersion = packageClient.findVersionByName(
                context.projectId,
                context.repoName,
                packageKey,
                version
            ).data ?: run {
                logger.warn("Remove pypi version: Cannot find version [$version] of package [$packageKey]")
                return
            }
            deletePypiVersion(context, packageVersion, packageKey)
        }
    }

    override fun doRequest(context: ArtifactContext): Response {
        val remoteConfiguration = context.getRemoteConfiguration()
        val httpClient = createHttpClient(remoteConfiguration)
        val downloadUrl = createRemoteDownloadUrl(context)
        val userAgent = HeaderUtils.getHeader(USER_AGENT)
            ?: "CPack${infoService.version()?.ensurePrefix("/") ?: ""}"
        val request = Request.Builder().url(downloadUrl).addHeader(USER_AGENT, userAgent).build()
        logger.info("request url: $downloadUrl, network config: ${remoteConfiguration.network}")
        return httpClient.newCall(request).execute()
    }

    override fun isExpired(cacheNode: NodeDetail, expiration: Long): Boolean {
        return if (expiration <= 0) true else super.isExpired(cacheNode, expiration)
    }

    @Suppress("UNCHECKED_CAST")
    private fun deletePypiVersion(
        context: ArtifactRemoveContext,
        packageVersion: PackageVersion,
        packageKey: String
    ) {
        with(context) {
            val artifactList = packageVersion.packageMetadata.find { it.key == ARTIFACT_LIST }?.value as? List<String>
            if (!artifactList.isNullOrEmpty()) {
                artifactList.forEach {
                    nodeClient.deleteNode(NodeDeleteRequest(projectId, repoName, it, userId))
                }
            } else {
                packageVersion.contentPath?.let {
                    nodeClient.deleteNode(NodeDeleteRequest(projectId, repoName, it, userId))
                }
            }
            packageClient.deleteVersion(
                projectId, repoName, packageKey, packageVersion.name, HttpContextHolder.getClientAddress()
            )
        }
    }

    private fun resolveMetadata(metadataString: String): PypiMetadata? {
        return PypiMetadata(
            name = getMetadataField(metadataString, FIELD_NAME) ?: return null,
            version = getMetadataField(metadataString, FIELD_VERSION) ?: return null
        )
    }

    private fun getMetadataField(metadataString: String, field: String): String? {
        return Regex("^$field: (.+)", RegexOption.MULTILINE).find(metadataString)?.groupValues?.getOrNull(1)
    }

    private fun getVersionDetail(context: ArtifactQueryContext): Any? {
        val packageKey = context.request.getParameter("packageKey")
        val version = context.request.getParameter("version")
        logger.info("Get version detail, packageKey: $packageKey, version: $version")
        val name = PackageKeys.resolvePypi(packageKey)
        val trueVersion = packageClient.findVersionByName(
            context.projectId,
            context.repoName,
            packageKey,
            version
        ).data
        val artifactPath = trueVersion?.contentPath ?: return null
        with(context.artifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, artifactPath).data ?: return null
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data
            val count = packageVersion?.downloads ?: 0
            val pypiArtifactBasic = Basic(
                name,
                version,
                node.size, node.fullPath,
                node.createdBy, node.createdDate,
                node.lastModifiedBy, node.lastModifiedDate,
                count,
                node.sha256,
                node.md5,
                null,
                null
            )
            return PypiArtifactVersionData(pypiArtifactBasic, packageVersion?.packageMetadata)
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PypiRemoteRepository::class.java)
    }
}
