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

package com.tencent.bkrepo.conan.utils

import com.tencent.bkrepo.common.api.constant.HttpHeaders.CONTENT_TYPE
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.conan.constant.CHANNEL
import com.tencent.bkrepo.conan.constant.DEFAULT_REVISION_V1
import com.tencent.bkrepo.conan.constant.NAME
import com.tencent.bkrepo.conan.constant.PACKAGE_REVISION
import com.tencent.bkrepo.conan.constant.REVISION
import com.tencent.bkrepo.conan.constant.USERNAME
import com.tencent.bkrepo.conan.constant.VERSION
import com.tencent.bkrepo.conan.constant.X_CONAN_SERVER_CAPABILITIES
import com.tencent.bkrepo.conan.controller.ConanCommonController.Companion.capabilities
import com.tencent.bkrepo.conan.pojo.ConanFileReference
import com.tencent.bkrepo.conan.pojo.ConanPackageUploadRequest
import com.tencent.bkrepo.conan.pojo.ConanRecipeDeleteRequest
import com.tencent.bkrepo.conan.pojo.ConanRecipeUploadRequest
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.pojo.user.BasicInfo
import com.tencent.bkrepo.conan.utils.ConanArtifactInfoUtil.buildConanFileReference
import com.tencent.bkrepo.conan.utils.ConanArtifactInfoUtil.convertToConanFileReference
import com.tencent.bkrepo.conan.utils.ConanArtifactInfoUtil.convertToPackageReference
import com.tencent.bkrepo.conan.utils.PathUtils.buildPackageReference
import com.tencent.bkrepo.conan.utils.PathUtils.buildReference
import com.tencent.bkrepo.conan.utils.PathUtils.buildReferenceWithoutVersion
import com.tencent.bkrepo.conan.utils.PathUtils.getPackageRevisionsFile
import com.tencent.bkrepo.conan.utils.PathUtils.getRecipeRevisionsFile
import com.tencent.bkrepo.conan.utils.TimeFormatUtil.convertToUtcTime
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletResponse

object ObjectBuildUtil {

    private val logger = LoggerFactory.getLogger(ObjectBuildUtil::class.java)

    fun buildPackageVersionCreateRequest(
        artifactInfo: ConanArtifactInfo,
        size: Long,
        sourceType: ArtifactChannel? = null,
        userId: String,
    ): PackageVersionCreateRequest {
        with(artifactInfo) {
            return PackageVersionCreateRequest(
                projectId = projectId,
                repoName = repoName,
                packageName = name,
                packageKey = PackageKeys.ofConan(buildRefStr(artifactInfo)),
                packageType = PackageType.CONAN,
                versionName = version,
                size = size,
                manifestPath = null,
                artifactPath = getArtifactFullPath(),
                stageTag = null,
                packageMetadata = addPackageMetadata(artifactInfo, sourceType),
                createdBy = userId,
                overwrite = true
            )
        }
    }

    fun buildRefStr(artifactInfo: ConanArtifactInfo): String {
        // conan key中的name由 实际name+username+channel组成
        val conanFileReference = convertToConanFileReference(artifactInfo)
        val refStr = buildReferenceWithoutVersion(conanFileReference)
        return refStr
    }

    fun buildPackageVersionUpdateRequest(
        artifactInfo: ConanArtifactInfo,
        size: Long,
    ): PackageVersionUpdateRequest {
        with(artifactInfo) {
            return PackageVersionUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = PackageKeys.ofConan(buildRefStr(artifactInfo)),
                versionName = version,
                size = size
            )
        }
    }

    private fun ConanFileReference.toPackageMetadataList(): List<MetadataModel> {
        return listOf(
            MetadataModel(NAME, name),
            MetadataModel(VERSION, version),
            MetadataModel(USERNAME, userName),
            MetadataModel(CHANNEL, channel),
            MetadataModel(REVISION, revision.orEmpty())
        )
    }

    fun List<MetadataModel>.toConanFileReference(): ConanFileReference? {
        val map = this.filter { it.system }.associate { it.key to it.value.toString() }

        return try {
            ConanFileReference(
                name = map[NAME]!!,
                version = map[VERSION]!!,
                userName = map[USERNAME]!!,
                channel = map[CHANNEL]!!,
                revision = map[REVISION],
                pRevision = map[PACKAGE_REVISION]
            )
        } catch (e: Exception) {
            logger.warn("Convert to ConanFileReference failed, map: $map")
            null
        }
    }

    private fun addPackageMetadata(
        artifactInfo: ConanArtifactInfo,
        sourceType: ArtifactChannel? = null,
        packageMetadata: List<MetadataModel>? = null
    ): List<MetadataModel> {
        val result = mutableListOf<MetadataModel>()
        sourceType?.let {
            result.add(MetadataModel(SOURCE_TYPE, sourceType))
        }
        convertToConanFileReference(artifactInfo, artifactInfo.revision, artifactInfo.pRevision)
            .toPackageMetadataList()
            .apply { result.addAll(this) }
        packageMetadata?.filterNot { it.system }?.let { result.addAll(it) }
        return result
    }

//    fun buildPackageUpdateRequest(
//        artifactInfo: ConanArtifactInfo
//    ): PackageUpdateRequest {
//        with(artifactInfo) {
//            return PackageUpdateRequest(
//                projectId = projectId,
//                repoName = repoName,
//                name = name,
//                packageKey = PackageKeys.ofConan(name),
//                versionTag = null,
//                extension = mapOf("appVersion" to version)
//            )
//        }
//    }

    fun buildDownloadResponse(
        response: HttpServletResponse = HttpContextHolder.getResponse(),
        contentType: String = MediaTypes.APPLICATION_JSON_WITHOUT_CHARSET
    ) {
        response.addHeader(X_CONAN_SERVER_CAPABILITIES, capabilities.joinToString(","))
        response.addHeader(CONTENT_TYPE, contentType)
    }

    fun buildConanRecipeUpload(
        artifactInfo: ConanArtifactInfo,
        userId: String
    ): ConanRecipeUploadRequest {
        with(artifactInfo) {
            val conanFileReference = convertToConanFileReference(this)
            val revPath = getRecipeRevisionsFile(conanFileReference)
            val refStr = buildReference(conanFileReference)
            return ConanRecipeUploadRequest(
                projectId = projectId,
                repoName = repoName,
                revPath = revPath,
                refStr = refStr,
                operator = userId,
                revision = revision ?: DEFAULT_REVISION_V1,
                dateStr = convertToUtcTime(LocalDateTime.now())
            )
        }
    }

    fun buildConanPackageUpload(
        artifactInfo: ConanArtifactInfo,
        userId: String
    ): ConanPackageUploadRequest {
        with(artifactInfo) {
            val packageReference = convertToPackageReference(this)
            val revPath = getRecipeRevisionsFile(packageReference.conRef)
            val refStr = buildReference(packageReference.conRef)
            val pRevPath = getPackageRevisionsFile(packageReference)
            val pRefStr = buildPackageReference(packageReference)
            return ConanPackageUploadRequest(
                projectId = projectId,
                repoName = repoName,
                revPath = revPath,
                refStr = refStr,
                operator = userId,
                revision = revision ?: DEFAULT_REVISION_V1,
                dateStr = convertToUtcTime(LocalDateTime.now()),
                pRefStr = pRefStr,
                pRevPath = pRevPath,
                pRevision = pRevision ?: DEFAULT_REVISION_V1,
            )
        }
    }

    fun buildConanRecipeDeleteRequest(
        artifactInfo: ConanArtifactInfo,
        userId: String
    ): ConanRecipeDeleteRequest {
        with(artifactInfo) {
            val conRef = this.buildConanFileReference()
            val revPath = getRecipeRevisionsFile(conRef)
            val refStr = buildReference(conRef)
            return ConanRecipeDeleteRequest(
                projectId = projectId,
                repoName = repoName,
                revPath = revPath,
                refStr = refStr,
                operator = userId,
                revision = revision ?: DEFAULT_REVISION_V1,
            )
        }
    }

//    fun buildConanPackageDeleteRequest(
//        artifactInfo: ConanArtifactInfo,
//        userId: String
//    ): ConanPackageDeleteRequest {
//        with(artifactInfo) {
//            val packageReference = convertToPackageReference(this)
//            val revPath = getRecipeRevisionsFile(packageReference.conRef)
//            val refStr = buildReference(packageReference.conRef)
//            val pRevPath = getPackageRevisionsFile(packageReference)
//            val pRefStr = buildPackageReference(packageReference)
//            return ConanPackageDeleteRequest(
//                projectId = projectId,
//                repoName = repoName,
//                revPath = revPath,
//                refStr = refStr,
//                operator = userId,
//                revision = revision ?: DEFAULT_REVISION_V1,
//                pRefStr = pRefStr,
//                pRevPath = pRevPath
//            )
//        }
//    }

    fun buildBasicInfo(nodeDetail: NodeDetail, packageVersion: PackageVersion): BasicInfo {
        with(nodeDetail) {
            return BasicInfo(
                version = packageVersion.name,
                fullPath = fullPath,
                size = size,
                sha256 = sha256.orEmpty(),
                md5 = md5.orEmpty(),
                stageTag = packageVersion.stageTag,
                projectId = projectId,
                repoName = repoName,
                downloadCount = packageVersion.downloads,
                createdBy = createdBy,
                createdDate = packageVersion.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastModifiedBy = lastModifiedBy,
                lastModifiedDate = packageVersion.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    fun buildDownloadRecordRequest(context: ArtifactDownloadContext): PackageDownloadRecord? {
        with(context) {
            val conanArtifactInfo = artifactInfo as ConanArtifactInfo
            val fullPath = PathUtils.generateFullPath(conanArtifactInfo)
            return if (PathUtils.isConanFile(fullPath)) {
                PackageDownloadRecord(
                    projectId = projectId,
                    repoName = repoName,
                    packageKey = PackageKeys.ofConan(buildRefStr(conanArtifactInfo)),
                    packageVersion = conanArtifactInfo.version,
                    userId = userId
                )
            } else null
        }
    }
}
