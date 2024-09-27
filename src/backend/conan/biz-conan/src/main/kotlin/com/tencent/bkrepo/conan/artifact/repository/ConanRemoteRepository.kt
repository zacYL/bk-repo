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

package com.tencent.bkrepo.conan.artifact.repository

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.conan.constant.CONAN_URL_V2
import com.tencent.bkrepo.conan.constant.ConanMessageCode
import com.tencent.bkrepo.conan.constant.IGNORECASE
import com.tencent.bkrepo.conan.constant.PATTERN
import com.tencent.bkrepo.conan.constant.REQUEST_TYPE
import com.tencent.bkrepo.conan.exception.ConanException
import com.tencent.bkrepo.conan.exception.ConanFileNotFoundException
import com.tencent.bkrepo.conan.listener.event.ConanArtifactUploadEvent
import com.tencent.bkrepo.conan.pojo.ConanSearchResult
import com.tencent.bkrepo.conan.pojo.RevisionInfo
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.pojo.enums.ConanRequestType
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildDownloadRecordRequest
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildRefStr
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.lang3.ObjectUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ConanRemoteRepository : RemoteRepository() {

    @Autowired
    lateinit var conanLocalRepository: ConanLocalRepository

    override fun query(context: ArtifactQueryContext): Any {
        context.getAttribute<ConanRequestType>(REQUEST_TYPE)?.let { requestType ->
            return try {
                when (requestType) {
                    ConanRequestType.SEARCH -> searchResult(context)
                    ConanRequestType.RECIPE_LATEST -> getRecipeLatestRevision(context)
                    else -> throw ConanException("request path is not valid")
                }
            } catch (e: Exception) {
                //代理失败，则使用本地缓存
                logger.warn("request remote failed, use local cache, error: ${e.message}")
                return conanLocalRepository.query(context)
            }
        } ?: throw ConanException("request path is not valid")
    }

    private fun searchResult(context: ArtifactQueryContext): ConanSearchResult {
        return doRequest(context, ConanSearchResult::class.java, createRemoteSearchUrl(context, "/search"))
            ?: throw ConanFileNotFoundException(ConanMessageCode.CONAN_FILE_NOT_FOUND)
    }

    private fun getRecipeLatestRevision(context: ArtifactQueryContext): RevisionInfo {
        return doRequest(context, RevisionInfo::class.java, createRemoteDownloadUrl(context))
            ?: throw ConanFileNotFoundException(ConanMessageCode.CONAN_FILE_NOT_FOUND)
    }

    private fun <T> doRequest(context: ArtifactContext, clazz: Class<T>, path: String): T? {
        val response = getHttpClient(context.repositoryDetail.configuration as RemoteConfiguration)
            .newCall(Request.Builder().url(path).build())
            .execute()
        return if (response.isSuccessful) {
            JsonUtils.objectMapper.readValue(
                response.body()?.byteStream(), clazz
            )
        } else {
            logger.error("Conan remote repository query failed, $path")
            null
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        val conanArtifactInfo = context.artifactInfo as ConanArtifactInfo
        val exist = with(conanArtifactInfo) {
            context.getFullPathInterceptors().forEach { it.intercept(projectId, getArtifactFullPath()) }
            nodeClient.checkExist(projectId, repoName, getArtifactFullPath()).data
        }
        val artifactResource = super.onDownload(context)
        if (true != exist && artifactResource != null) {
            SpringContextUtils.publishEvent(ConanArtifactUploadEvent(context.userId, conanArtifactInfo))
        }
        return artifactResource
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
    ): PackageDownloadRecord? {
        return buildDownloadRecordRequest(context)
    }

    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
        with(context.artifactInfo as ConanArtifactInfo) {
            packageClient
                .findVersionByName(projectId, repoName, PackageKeys.ofConan(buildRefStr(this)), version).data
                ?.apply { packageDownloadIntercept(context, this) }
        }
    }

    fun createRemoteSearchUrl(context: ArtifactContext, path: String): String {
        val configuration = context.getRemoteConfiguration()
        val queryString = context.request.queryString ?: queryString(context)
        return UrlFormatter.format(configuration.url, "$CONAN_URL_V2$path", queryString)
    }

    private fun queryString(context: ArtifactContext): String {
        val pattern = context.getAttribute<String>(PATTERN)
        val ignoreCase = context.getAttribute<Boolean>(IGNORECASE) ?: true
        return if (ObjectUtils.isNotEmpty(pattern)) {
            "q=$pattern&ignorecase=$ignoreCase"
        } else {
            ""
        }
    }

    override fun createRemoteDownloadUrl(context: ArtifactContext): String {
        val configuration = context.getRemoteConfiguration()
        val requestPath = context.request.requestURL.toString()
        val startIndex = requestPath.indexOf(CONAN_URL_V2)
        val trimmedPath = requestPath.substring(startIndex)
        val queryString = context.request.queryString
        return UrlFormatter.format(configuration.url, trimmedPath, queryString)
    }

    fun getHttpClient(configuration: RemoteConfiguration, addInterceptor: Boolean = true): OkHttpClient {
        return super.createHttpClient(configuration, addInterceptor)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
