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

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.conan.constant.CONAN_MANIFEST
import com.tencent.bkrepo.conan.constant.ConanMessageCode
import com.tencent.bkrepo.conan.constant.IGNORECASE
import com.tencent.bkrepo.conan.constant.PATTERN
import com.tencent.bkrepo.conan.constant.REQUEST_TYPE
import com.tencent.bkrepo.conan.constant.X_CHECKSUM_SHA1
import com.tencent.bkrepo.conan.exception.ConanException
import com.tencent.bkrepo.conan.exception.ConanFileNotFoundException
import com.tencent.bkrepo.conan.exception.ConanParameterInvalidException
import com.tencent.bkrepo.conan.listener.event.ConanPackageUploadEvent
import com.tencent.bkrepo.conan.listener.event.ConanRecipeUploadEvent
import com.tencent.bkrepo.conan.pojo.ConanSearchResult
import com.tencent.bkrepo.conan.pojo.RevisionInfo
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.pojo.enums.ConanRequestType
import com.tencent.bkrepo.conan.pojo.metadata.ConanMetadataRequest
import com.tencent.bkrepo.conan.service.ConanMetadataService
import com.tencent.bkrepo.conan.service.impl.CommonService
import com.tencent.bkrepo.conan.utils.ConanArtifactInfoUtil.convertToConanFileReference
import com.tencent.bkrepo.conan.utils.ConanPathUtils
import com.tencent.bkrepo.conan.utils.ConanPathUtils.buildConanFileName
import com.tencent.bkrepo.conan.utils.ConanPathUtils.buildPackagePath
import com.tencent.bkrepo.conan.utils.ConanPathUtils.generateFullPath
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildDownloadResponse
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildPackageVersionCreateRequest
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildRefStr
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.toConanFileReference
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.toMetadataList
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ConanLocalRepository : LocalRepository() {

    @Autowired
    lateinit var conanMetadataService: ConanMetadataService

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
            val conanFileReference = convertToConanFileReference(conanArtifactInfo)
            val path = PathUtils.normalizeFullPath(buildPackagePath(conanFileReference))
            commonService.getNodeDetail(projectId, repoName, path)
            return commonService.getLastRevision(projectId, repoName, conanFileReference)
                ?: throw ConanFileNotFoundException(
                    ConanMessageCode.CONAN_FILE_NOT_FOUND,
                    path,
                    getRepoIdentify()
                )
        }
    }

    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        with(context) {
            val tempArtifactInfo = context.artifactInfo as ConanArtifactInfo
            val fullPath = generateFullPath(tempArtifactInfo)
            logger.info("File $fullPath will be stored in $projectId|$repoName")
            val sha1 = HttpContextHolder.getRequest().getHeader(X_CHECKSUM_SHA1)?.toString()
            val conanFileReference = convertToConanFileReference(tempArtifactInfo)
            val metadata = mutableListOf<MetadataModel>()
            sha1?.let {
                metadata.add(MetadataModel(key = X_CHECKSUM_SHA1, value = sha1, system = true))
            }
            metadata.addAll(conanFileReference.toMetadataList())
            return NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                folder = false,
                fullPath = fullPath,
                size = getArtifactFile().getSize(),
                sha256 = getArtifactSha256(),
                md5 = getArtifactMd5(),
                operator = userId,
                overwrite = true,
                nodeMetadata = metadata
            )
        }
    }

    override fun onUploadBefore(context: ArtifactUploadContext) {
        super.onUploadBefore(context)
        with(context.artifactInfo as ConanArtifactInfo) {
            packageService
                .findVersionByName(projectId, repoName, PackageKeys.ofConan(buildRefStr(this)), version)
                ?.apply { uploadIntercept(context, this) }
        }
    }

    /**
     * 上传成功回调
     */
    override fun onUploadSuccess(context: ArtifactUploadContext) {
        super.onUploadSuccess(context)
        handleConanArtifactUpload(
            context.artifactInfo as ConanArtifactInfo, context.userId, context.getArtifactFile().getSize()
        )
    }


    fun handleConanArtifactUpload(artifactInfo: ConanArtifactInfo, userId: String, size: Long = 0) {
        val fullPath = generateFullPath(artifactInfo)
        if (fullPath.endsWith(CONAN_MANIFEST) && artifactInfo.packageId.isNullOrEmpty()) {
            //  package version size 为manifest文件大小
            createVersion(
                artifactInfo = artifactInfo,
                userId = userId,
                size = size
            )
            publishEvent(
                ConanRecipeUploadEvent(
                    ObjectBuildUtil.buildConanRecipeUpload(artifactInfo, userId)
                )
            )
        }
        if (fullPath.endsWith(CONAN_MANIFEST) && !artifactInfo.packageId.isNullOrEmpty()) {
            publishEvent(
                ConanPackageUploadEvent(
                    ObjectBuildUtil.buildConanPackageUpload(artifactInfo, userId)
                )
            )
        }
    }

    /**
     * 创建包版本
     */
    private fun createVersion(
        userId: String,
        artifactInfo: ConanArtifactInfo,
        size: Long,
        sourceType: ArtifactChannel? = null
    ) {
        val packageVersionCreateRequest = buildPackageVersionCreateRequest(
            userId = userId,
            artifactInfo = artifactInfo,
            size = size,
            sourceType = sourceType
        )
        packageService.createPackageVersion(packageVersionCreateRequest).apply {
            logger.info("user: [$userId] create package version [$packageVersionCreateRequest] success!")
        }
        val request = ConanMetadataRequest(
            projectId = artifactInfo.projectId,
            repoName = artifactInfo.repoName,
            name = artifactInfo.name,
            version = artifactInfo.version,
            user = artifactInfo.userName,
            channel = artifactInfo.channel,
            recipe = buildConanFileName(convertToConanFileReference(artifactInfo))
        )
        conanMetadataService.storeMetadata(request)
    }

    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
        with(context.artifactInfo as ConanArtifactInfo) {
            packageService.findVersionByName(projectId, repoName, PackageKeys.ofConan(buildRefStr(this)), version)
                ?.apply { packageDownloadIntercept(context, this) }
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context.artifactInfo as ConanArtifactInfo) {
            val fullPath = generateFullPath(this)
            context.getFullPathInterceptors().forEach { it.intercept(projectId, fullPath) }
            logger.info("File $fullPath will be downloaded in repo $projectId|$repoName")
            val node = nodeService.getNodeDetail(ArtifactInfo(context.projectId, context.repoName, fullPath))
            node?.let {
                context.artifactInfo.setArtifactMappingUri(node.fullPath)
                downloadIntercept(context, node)
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
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        val conanFileReference = convertToConanFileReference(
            context.artifactInfo as ConanArtifactInfo
        )
        val refStr = ConanPathUtils.buildReferenceWithoutVersion(conanFileReference)
        return PackageDownloadRecord(
            context.projectId, context.repoName, PackageKeys.ofConan(refStr), conanFileReference.version, context.userId
        )
    }

    override fun packageVersion(context: ArtifactContext?, node: NodeDetail?): PackageVersion? {
        with(context!!) {
            val conanFileReference = node?.nodeMetadata?.toConanFileReference() ?: return null
            val refStr = ConanPathUtils.buildReferenceWithoutVersion(conanFileReference)
            val packageKey = PackageKeys.ofConan(refStr)
            return packageService.findVersionByName(projectId, repoName, packageKey, conanFileReference.version)
        }
    }

    private fun searchRecipes(projectId: String, repoName: String): List<String> {
        val result = mutableListOf<String>()
        packageService.listAllPackageName(projectId, repoName).forEach { packageName ->
            packageService.listAllVersion(projectId, repoName, packageName, VersionListOption()).forEach { pv ->
                val conanInfo = pv.packageMetadata.toConanFileReference()?.let {
                    result.add(buildConanFileName(it))
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
