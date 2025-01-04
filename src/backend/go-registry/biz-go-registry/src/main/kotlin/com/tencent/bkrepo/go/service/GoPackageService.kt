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

import com.tencent.bkrepo.common.artifact.constant.MD5
import com.tencent.bkrepo.common.artifact.constant.SHA256
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.go.constant.ARCHIVE
import com.tencent.bkrepo.go.constant.GO_MOD_SIZE_THRESHOLD
import com.tencent.bkrepo.go.constant.README_SIZE_THRESHOLD
import com.tencent.bkrepo.go.pojo.artifact.GoArtifactInfo
import com.tencent.bkrepo.go.pojo.artifact.GoModuleInfo
import com.tencent.bkrepo.go.pojo.enum.GoFileType
import com.tencent.bkrepo.go.util.DecompressUtil.readModAndReadmeContent
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class GoPackageService(
    private val storageManager: StorageManager,
    private val packageClient: PackageClient,
    private val nodeClient: NodeClient
) {

    fun createVersion(
        artifactInfo: GoModuleInfo,
        size: Long,
        sha256: String,
        md5: String,
        extension: Map<String, String> = emptyMap()
    ) {
        with(artifactInfo) {
            if (type != GoFileType.ZIP) return
            val versionExtension = mutableMapOf(SHA256 to sha256, MD5 to md5) + extension
            val version = getArtifactVersion()
            val request = PackageVersionCreateRequest(
                projectId = projectId,
                repoName = repoName,
                packageName = getArtifactName(),
                packageKey = PackageKeys.ofName(RepositoryType.GO, modulePath),
                packageType = PackageType.GO,
                versionName = version,
                size = size,
                artifactPath = getArtifactFullPath(),
                packageMetadata = listOf(),
                overwrite = true,
                createdBy = SecurityUtils.getUserId(),
                extension = versionExtension
            )
            packageClient.createVersion(request, HttpContextHolder.getClientAddress())
            logger.info("created version for go module [${artifactInfo.getModuleId()}]")
        }
    }

    fun buildDownloadRecord(artifactInfo: GoModuleInfo, userId: String): PackageDownloadRecord? {
        with(artifactInfo) {
            if (type != GoFileType.ZIP) return null
            return PackageDownloadRecord(
                projectId = projectId,
                repoName = repoName,
                packageKey = getPackageKey(),
                packageVersion = getArtifactVersion(),
                userId = userId
            )
        }
    }

    fun remove(artifactInfo: GoArtifactInfo, userId: String) {
        with(artifactInfo) {
            val version = getArtifactVersion()
            if (version == null) {
                packageClient.deletePackage(projectId, repoName, getPackageKey(), HttpContextHolder.getClientAddress())
            } else {
                packageClient.deleteVersion(
                    projectId,
                    repoName,
                    getPackageKey(),
                    version,
                    HttpContextHolder.getClientAddress()
                )
            }
            val request = NodeDeleteRequest(projectId, repoName, getArtifactRootPath(), userId)
            nodeClient.deleteNode(request)
        }
    }

    fun extractMod(
        artifactInfo: GoModuleInfo,
        inputStream: InputStream,
        credentials: StorageCredentials?
    ): NodeDetail? {
        return try {
            val fullPath = artifactInfo.getArtifactFullPathByType(GoFileType.MOD)
            val bytes = inputStream.readModAndReadmeContent(readReadme = false).first?.toByteArray()
            if (bytes != null) {
                val artifactFile = bytes.inputStream().use { ArtifactFileFactory.build(it) }
                val nodeCreateRequest = NodeCreateRequest(
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    fullPath = fullPath,
                    folder = false,
                    size = artifactFile.getSize(),
                    sha256 = artifactFile.getFileSha256(),
                    md5 = artifactFile.getFileMd5(),
                    overwrite = true,
                    operator = SecurityUtils.getUserId(),
                    nodeMetadata = artifactInfo.generateMetadata() + MetadataModel(SOURCE_TYPE, ARCHIVE, true)
                )
                storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, credentials)
            } else null
        } catch (e: Exception) {
            logger.error("An error occurred while extracting go.mod file of [${artifactInfo.getModuleId()}]:", e)
            null
        }
    }

    fun extractModAndReadme(
        artifactInfo: GoModuleInfo,
        inputStream: InputStream,
        credentials: StorageCredentials?,
        extractMod: Boolean = true,
        extractReadme: Boolean = true
    ): Pair<String?, String?> {
        return try {
            val userId = SecurityUtils.getUserId()
            val modFullPath = artifactInfo.getArtifactFullPathByType(GoFileType.MOD)
            val modExist = nodeClient.checkExist(artifactInfo.projectId, artifactInfo.repoName, modFullPath).data
            val (mod, readme) = inputStream.readModAndReadmeContent(extractMod, extractReadme)
            val modBytes = mod?.toByteArray()
            val readmeBytes = readme?.toByteArray()
            if (modBytes != null && modExist == false) {
                val modArtifactFile = modBytes.inputStream().use { ArtifactFileFactory.build(it) }
                val nodeCreateRequest = NodeCreateRequest(
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    fullPath = modFullPath,
                    folder = false,
                    size = modArtifactFile.getSize(),
                    sha256 = modArtifactFile.getFileSha256(),
                    md5 = modArtifactFile.getFileMd5(),
                    overwrite = false,
                    operator = userId,
                    nodeMetadata = artifactInfo.generateMetadata() + MetadataModel(SOURCE_TYPE, ARCHIVE, true)
                )
                storageManager.storeArtifactFile(nodeCreateRequest, modArtifactFile, credentials)
            }
            if (readmeBytes != null && readmeBytes.size > README_SIZE_THRESHOLD) {
                val readmeArtifactFile = readmeBytes.inputStream().use { ArtifactFileFactory.build(it) }
                val nodeCreateRequest = NodeCreateRequest(
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    fullPath = artifactInfo.getReadmeFullPath(),
                    folder = false,
                    size = readmeArtifactFile.getSize(),
                    sha256 = readmeArtifactFile.getFileSha256(),
                    md5 = readmeArtifactFile.getFileMd5(),
                    overwrite = true,
                    operator = userId,
                    nodeMetadata = artifactInfo.generateMetadata() + MetadataModel(SOURCE_TYPE, ARCHIVE, true)
                )
                storageManager.storeArtifactFile(nodeCreateRequest, readmeArtifactFile, credentials)
            }
            Pair(
                modBytes?.run { if (size <= GO_MOD_SIZE_THRESHOLD) toString(Charsets.UTF_8) else null },
                readmeBytes?.run { if (size <= README_SIZE_THRESHOLD) toString(Charsets.UTF_8) else null }
            )
        } catch (e: Exception) {
            logger.error("An error occurred while extracting go.mod/readme file of [${artifactInfo.getModuleId()}]:", e)
            Pair(null, null)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoPackageService::class.java)
    }
}
