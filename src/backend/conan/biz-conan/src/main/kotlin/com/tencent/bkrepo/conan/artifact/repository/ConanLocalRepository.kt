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

import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.conan.constant.ConanMessageCode
import com.tencent.bkrepo.conan.constant.IGNORECASE
import com.tencent.bkrepo.conan.constant.NAME
import com.tencent.bkrepo.conan.constant.PATTERN
import com.tencent.bkrepo.conan.constant.REQUEST_TYPE
import com.tencent.bkrepo.conan.constant.VERSION
import com.tencent.bkrepo.conan.exception.ConanException
import com.tencent.bkrepo.conan.exception.ConanFileNotFoundException
import com.tencent.bkrepo.conan.exception.ConanParameterInvalidException
import com.tencent.bkrepo.conan.listener.event.ConanArtifactUploadEvent
import com.tencent.bkrepo.conan.pojo.ConanSearchResult
import com.tencent.bkrepo.conan.pojo.RevisionInfo
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.pojo.enums.ConanRequestType
import com.tencent.bkrepo.conan.service.impl.CommonService
import com.tencent.bkrepo.conan.utils.ConanArtifactInfoUtil
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildDownloadRecordRequest
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildDownloadResponse
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.toConanFileReference
import com.tencent.bkrepo.conan.utils.PathUtils
import com.tencent.bkrepo.conan.utils.PathUtils.generateFullPath
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ConanLocalRepository : LocalRepository() {

    @Autowired
    lateinit var commonService: CommonService

    override fun query(context: ArtifactQueryContext): Any {
        context.getAttribute<ConanRequestType>(REQUEST_TYPE)?.let { requestType ->
            return when (requestType) {
                ConanRequestType.SEARCH -> searchResult(context)
                ConanRequestType.RECIPE_LATEST -> getRecipeLatestRevision(context.artifactInfo as ConanArtifactInfo)
                else -> throw ConanException("request path is not valid")
            }
        } ?: throw ConanException("request path is not valid")
    }

    private fun searchResult(context: ArtifactQueryContext): ConanSearchResult {
        val pattern = context.getAttribute<String>(PATTERN)
        val ignoreCase = context.getAttribute<Boolean>(IGNORECASE) ?: false
        val recipes = searchRecipes(context.projectId, context.repoName)
        val list = if (pattern.isNullOrEmpty()) {
            recipes
        } else {
            matchPattern(pattern, recipes, ignoreCase)
        }
        return ConanSearchResult(list)
    }

    fun getRecipeLatestRevision(conanArtifactInfo: ConanArtifactInfo): RevisionInfo {
        with(conanArtifactInfo) {
            val conanFileReference = ConanArtifactInfoUtil.convertToConanFileReference(conanArtifactInfo)
            commonService.checkNodeExist(projectId, repoName, PathUtils.buildReference(conanFileReference))
            return commonService.getLastRevision(projectId, repoName, conanFileReference)
                ?: throw ConanFileNotFoundException(
                    ConanMessageCode.CONAN_FILE_NOT_FOUND, PathUtils.buildReference(conanFileReference), getRepoIdentify()
                )
        }
    }

    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        with(context) {
            val fullPath = generateFullPath(context.artifactInfo as ConanArtifactInfo)
            logger.info("File $fullPath will be stored in $projectId|$repoName")
            return NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                folder = false,
                fullPath = fullPath,
                size = getArtifactFile().getSize(),
                sha256 = getArtifactSha256(),
                md5 = getArtifactMd5(),
                operator = userId,
                overwrite = true
            )
        }
    }

    override fun onUploadBefore(context: ArtifactUploadContext) {
        super.onUploadBefore(context)
        with(context.artifactInfo as ConanArtifactInfo) {
            packageClient
                .findVersionByName(projectId, repoName, PackageKeys.ofConan(name), version).data
                ?.apply { uploadIntercept(context, this) }
        }
    }

    /**
     * 上传成功回调
     */
    override fun onUploadSuccess(context: ArtifactUploadContext) {
        super.onUploadSuccess(context)
        SpringContextUtils.publishEvent(ConanArtifactUploadEvent(context.userId, context.artifactInfo as ConanArtifactInfo))
    }

    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
        with(context.artifactInfo as ConanArtifactInfo) {
            packageClient
                .findVersionByName(projectId, repoName, PackageKeys.ofConan(name), version).data
                ?.apply { packageDownloadIntercept(context, this) }
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context.artifactInfo as ConanArtifactInfo) {
            val fullPath = generateFullPath(this)
            context.getFullPathInterceptors().forEach { it.intercept(projectId, fullPath) }
            logger.info("File $fullPath will be downloaded in repo $projectId|$repoName")
            val node = nodeClient.getNodeDetail(context.projectId, context.repoName, fullPath).data
            node?.let {
                node.metadata[NAME]?.let { context.putAttribute(NAME, it) }
                node.metadata[VERSION]?.let { context.putAttribute(VERSION, it) }
            }
            val inputStream = storageManager.loadArtifactInputStream(node, context.storageCredentials)
            buildDownloadResponse()
            inputStream?.let {
                return ArtifactResource(
                    inputStream,
                    context.artifactInfo.getResponseName(),
                    RepositoryIdentify(context.projectId, context.repoName),
                    node,
                    ArtifactChannel.LOCAL,
                    context.useDisposition
                )
            }
            return null
        }
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
    ): PackageDownloadRecord? {
        return buildDownloadRecordRequest(context)
    }

    private fun searchRecipes(projectId: String, repoName: String): List<String> {
        val result = mutableListOf<String>()
        packageClient.listAllPackageNames(projectId, repoName).data.orEmpty().forEach { packageName ->
            packageClient.listAllVersion(projectId, repoName, packageName).data.orEmpty().forEach { pv ->
                val conanInfo = pv.packageMetadata.toConanFileReference()?.let {
                    result.add(PathUtils.buildConanFileName(it))
                }
            }
        }
        return result.sorted()
    }

    // 检查模式是否有效
    fun isValidPattern(pattern: String): Boolean {
        // 定义匹配包名称和版本号的正则表达式模式
        val nameVersionPattern = "[a-z0-9_*+.\\-]+(/[a-zA-Z0-9.*_]+)?"
        // 定义匹配用户和通道的正则表达式模式
        val userChannelPattern = "[a-zA-Z0-9_*]+"

        val fullPattern = "^$nameVersionPattern(@$userChannelPattern/$userChannelPattern)?$".toRegex()
        return pattern.matches(fullPattern)
    }

    fun matchPattern(pattern: String, pList: List<String>, ignoreCase: Boolean): List<String> {
        if (isValidPattern(pattern).not()) throw ConanParameterInvalidException("Unexpected query syntax")

        // 将通配符模式转换为正则表达式
        val regexPattern = pattern.replace("*", ".*")
        val regex = if (ignoreCase) {
            regexPattern.toRegex(RegexOption.IGNORE_CASE)
        } else {
            regexPattern.toRegex()
        }

        return if (pattern.contains("*")) {
            pList.filter { it.matches(regex) }
        } else {
            pList.filter { regex.containsMatchIn(it) }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConanLocalRepository::class.java)
    }
}
