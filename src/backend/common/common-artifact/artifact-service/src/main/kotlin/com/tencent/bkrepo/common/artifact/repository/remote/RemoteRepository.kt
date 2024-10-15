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

package com.tencent.bkrepo.common.artifact.repository.remote

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.artifact.util.WhitelistUtils
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.slf4j.LoggerFactory
import java.net.UnknownHostException
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 远程仓库抽象逻辑
 */
@Suppress("TooManyFunctions")
abstract class RemoteRepository : AbstractArtifactRepository() {

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        whitelistInterceptor(context)
        with(context) {
            getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
        }
        return getCacheArtifactResource(context) ?: doRequest(context) as ArtifactResource?
    }

    @Suppress("TooGenericExceptionCaught", "LoopWithTooManyJumpStatements")
    fun downloadRetry(request: Request, okHttpClient: OkHttpClient): Response? {
        var response: Response? = null
        outer@ for (i in 1..downloadRetryLimit) {
            try {
                val startTime = System.currentTimeMillis()
                response = okHttpClient.newCall(request).execute()
                val endTime = System.currentTimeMillis()
                logger.info(
                    "Remote download: download retry: $i, url: ${request.url()}," +
                        " cost: ${endTime - startTime}ms, code: ${response.code()}"
                )
                if (checkRetry(response)) { break@outer }
            } catch (unKnownHostException: UnknownHostException) {
                logger.error(
                    "Remote download: download retry: $i, url: ${request.url()}, " +
                        "error: ${unKnownHostException.message}"
                )
                break@outer
            } catch (ie: IllegalArgumentException) {
                logger.error("Remote download: download retry: $i, url: ${request.url()}, error: ${ie.message}")
                break@outer
            } catch (e: Exception) {
                logger.error("Remote download: request failed: $request", e)
            }
        }
        return response
    }

    /**
     * 校验远程请求是否具有重试的价值
     * 200、201、202
     */
    open fun checkRetry(response: Response): Boolean {
        if (response.isSuccessful) {
            return true
        }
        logger.warn(
            "Remote download: download failed: ${response.code()}, " +
                "url: ${response.request().url()}, body: ${response.body()?.string()}"
        )
        return false
    }

    override fun search(context: ArtifactSearchContext): List<Any> {
        val remoteConfiguration = context.getRemoteConfiguration()
        val httpClient = createHttpClient(remoteConfiguration)
        val downloadUri = createRemoteDownloadUrl(context)
        val request = Request.Builder().url(downloadUri).build()
        val response = httpClient.newCall(request).execute()
        return if (checkResponse(response)) {
            onSearchResponse(context, response)
        } else {
            emptyList()
        }
    }

    override fun query(context: ArtifactQueryContext): Any? {
        return doRequest(context)
    }

    protected open fun doRequest(context: ArtifactContext): Any? {
        val remoteConfiguration = context.getRemoteConfiguration()
        val httpClient = createHttpClient(remoteConfiguration)
        val url = createRemoteDownloadUrl(context)
        val request = Request.Builder().url(url).build()
        val repoId = context.artifactInfo.getRepoIdentify()
        logger.info("[$repoId]Request url: $url, network config: ${remoteConfiguration.network}")
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            logger.error("An error occurred while sending request $url", e)
            null
        }
        return response?.let {
            when (context) {
                is ArtifactDownloadContext -> if (checkResponse(it)) onDownloadResponse(context, it) else null
                is ArtifactQueryContext -> if (checkQueryResponse(it)) onQueryResponse(context, it) else null
                is ArtifactSearchContext -> if (checkResponse(it)) onSearchResponse(context, it) else emptyList()
                else -> MethodNotAllowedException()
            }
        }
    }

    /**
     *  根据远程仓库配置获取响应
     */
    fun getResponse(remoteConfiguration: RemoteConfiguration): Response {
        with(remoteConfiguration) {
            val httpClient = createHttpClient(remoteConfiguration)
            val request = Request.Builder().url(url)
                .removeHeader("User-Agent")
                .addHeader("User-Agent", "${UUID.randomUUID()}")
                .build()
            return httpClient.newCall(request).execute()
        }
    }

    /**
     * 尝试读取缓存的远程构件
     */
    open fun getCacheArtifactResource(context: ArtifactContext): ArtifactResource? {
        return getCacheInfo(context)?.takeIf { it.second }?.let { loadArtifactResource(it.first, context) }
    }

    /**
     * 获取缓存的远程构件节点及过期状态
     */
    protected fun getCacheInfo(context: ArtifactContext): Pair<NodeDetail, Boolean>? {
        val configuration = context.getRemoteConfiguration()
        if (!configuration.cache.enabled) return null

        val cacheNode = findCacheNodeDetail(context)
        return if (cacheNode == null || cacheNode.folder) null else {
            Pair(cacheNode, isExpired(cacheNode, configuration.cache.expiration))
        }
    }

    /**
     * 加载要返回的资源
     */
    open fun loadArtifactResource(cacheNode: NodeDetail, context: ArtifactContext): ArtifactResource? {
        return storageService.load(cacheNode.sha256!!, Range.full(cacheNode.size), context.storageCredentials)?.run {
            if (logger.isDebugEnabled) {
                logger.debug("Cached remote artifact[${context.artifactInfo}] is hit.")
            }
            val srcRepo = RepositoryIdentify(context.projectId, context.repoName)
            val responseName = context.artifactInfo.getResponseName()
            val useDisposition = if (context is ArtifactDownloadContext) context.useDisposition else false
            ArtifactResource(this, responseName, srcRepo, cacheNode, ArtifactChannel.PROXY, useDisposition)
        }
    }

    /**
     * 判断缓存节点[cacheNode]是否过期，[expiration]表示有效期，单位分钟
     */
    protected open fun isExpired(cacheNode: NodeDetail, expiration: Long): Boolean {
        if (expiration <= 0) {
            return false
        }
        val createdDate = LocalDateTime.parse(cacheNode.createdDate, DateTimeFormatter.ISO_DATE_TIME)
        return Duration.between(createdDate, LocalDateTime.now()).toMinutes() >= expiration
    }

    /**
     * 尝试获取缓存的远程构件节点
     */
    open fun findCacheNodeDetail(context: ArtifactContext): NodeDetail? {
        with(context) {
            return nodeClient.getNodeDetail(projectId, repoName, artifactInfo.getArtifactFullPath()).data
        }
    }

    /**
     * 将远程拉取的构件缓存本地
     */
    protected fun cacheArtifactFile(context: ArtifactContext, artifactFile: ArtifactFile): NodeDetail? {
        val configuration = context.getRemoteConfiguration()
        return if (configuration.cache.enabled) {
            val nodeCreateRequest = buildCacheNodeCreateRequest(context, artifactFile)
            storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, context.storageCredentials)
        } else {
            null
        }
    }

    /**
     * 远程下载响应回调
     */
    open fun onDownloadResponse(context: ArtifactDownloadContext, response: Response): ArtifactResource {
        val artifactFile = createTempFile(response.body()!!)
        val size = artifactFile.getSize()
        val artifactStream = artifactFile.getInputStream().artifactStream(Range.full(size))
        val node = cacheArtifactFile(context, artifactFile)
        val responseName = context.artifactInfo.getResponseName()
        val srcRepo = RepositoryIdentify(context.projectId, context.repoName)
        return ArtifactResource(artifactStream, responseName, srcRepo, node, ArtifactChannel.PROXY)
    }

    /**
     * 远程下载响应回调
     */
    open fun onSearchResponse(context: ArtifactSearchContext, response: Response): List<Any> {
        return emptyList()
    }

    /**
     * 远程下载响应回调
     */
    open fun onQueryResponse(context: ArtifactQueryContext, response: Response): Any? {
        return null
    }

    /**
     * 获取缓存节点创建请求
     */
    open fun buildCacheNodeCreateRequest(context: ArtifactContext, artifactFile: ArtifactFile): NodeCreateRequest {
        val nodeMetadata = if (WhitelistUtils.optionalType().contains(context.repositoryDetail.type)) {
            listOf(MetadataModel(SOURCE_TYPE, ArtifactChannel.PROXY))
        } else { null }
        return NodeCreateRequest(
            projectId = context.repositoryDetail.projectId,
            repoName = context.repositoryDetail.name,
            folder = false,
            fullPath = context.artifactInfo.getArtifactFullPath(),
            size = artifactFile.getSize(),
            sha256 = artifactFile.getFileSha256(),
            md5 = artifactFile.getFileMd5(),
            overwrite = true,
            operator = context.userId,
            nodeMetadata = nodeMetadata
        )
    }

    /**
     * 生成远程构件下载url
     */
    open fun createRemoteDownloadUrl(context: ArtifactContext): String {
        val configuration = context.getRemoteConfiguration()
        val artifactUri = context.artifactInfo.getArtifactName()
        val queryString = context.request.queryString
        return UrlFormatter.format(configuration.url, artifactUri, queryString)
    }

    /**
     * 创建http client
     */
    protected fun createHttpClient(configuration: RemoteConfiguration, addInterceptor: Boolean = true): OkHttpClient {
        return buildOkHttpClient(configuration, addInterceptor).build()
    }

    /**
     * 检查下载响应
     */
    protected fun checkResponse(response: Response): Boolean {
        if (!response.isSuccessful) {
            logger.warn("Download artifact from remote failed: [${response.code()}]")
            return false
        }
        return true
    }

    /**
     * 检查查询响应
     */
    open fun checkQueryResponse(response: Response): Boolean {
        if (!response.isSuccessful) {
            logger.warn("Query artifact info from remote failed: [${response.code()}]")
            return false
        }
        return true
    }

    /**
     * 创建临时文件并将响应体写入文件
     */
    protected fun createTempFile(body: ResponseBody): ArtifactFile {
        return ArtifactFileFactory.build(body.byteStream())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteRepository::class.java)
        private val resourceNotReach = intArrayOf(
            HttpStatus.NOT_FOUND.value,
            HttpStatus.UNAUTHORIZED.value,
            HttpStatus.PAYMENT_REQUIRED.value,
            HttpStatus.FORBIDDEN.value
        )
        private val serverStatusError = intArrayOf(
            HttpStatus.BAD_GATEWAY.value,
            HttpStatus.INTERNAL_SERVER_ERROR.value
        )
        const val downloadRetryLimit = 4
    }
}
