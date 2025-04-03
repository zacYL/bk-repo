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

import com.tencent.bkrepo.common.api.constant.HttpHeaders.CONTENT_DISPOSITION
import com.tencent.bkrepo.common.api.constant.HttpHeaders.CONTENT_TYPE
import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_OCTET_STREAM
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.util.StreamUtils.readText
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.CONTENT_DISPOSITION_TEMPLATE
import com.tencent.bkrepo.common.artifact.constant.MD5
import com.tencent.bkrepo.common.artifact.constant.SHA256
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.BasicInfo
import com.tencent.bkrepo.common.artifact.pojo.RegistryDomainInfo
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactExtService
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.go.constant.CLIENT_ARCHIVE_PATH
import com.tencent.bkrepo.go.constant.CLIENT_NAME
import com.tencent.bkrepo.go.constant.GO_MOD_KEY
import com.tencent.bkrepo.go.constant.GoProperties
import com.tencent.bkrepo.go.constant.README_KEY
import com.tencent.bkrepo.go.pojo.artifact.GoArtifactInfo
import com.tencent.bkrepo.go.pojo.response.GoPackageVersionInfo
import com.tencent.bkrepo.go.util.GoUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

@Service
class GoExtService(
    private val goProperties: GoProperties,
    private val storageManager: StorageManager
) : ArtifactExtService() {

    fun getRegistryDomain() = goProperties.domain

    override fun getVersionDetail(userId: String, artifactInfo: ArtifactInfo): GoPackageVersionInfo {
        with(artifactInfo as GoArtifactInfo) {
            val version = getArtifactVersion()!!
            val packageVersion = packageService.findVersionByName(projectId, repoName, getPackageKey(), version)
                ?: throw VersionNotFoundException(version)
            val basic = BasicInfo(
                version = version,
                fullPath = packageVersion.contentPath!!,
                size = packageVersion.size,
                sha256 = packageVersion.extension[SHA256]?.toString().orEmpty(),
                md5 = packageVersion.extension[MD5]?.toString().orEmpty(),
                stageTag = packageVersion.stageTag,
                projectId = projectId,
                repoName = repoName,
                downloadCount = packageVersion.downloads,
                createdBy = packageVersion.createdBy,
                createdDate = packageVersion.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastModifiedBy = packageVersion.lastModifiedBy,
                lastModifiedDate = packageVersion.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
            )
            val credentials = repositoryService.getRepoDetail(projectId, repoName)?.storageCredentials
            val mod = packageVersion.extension[GO_MOD_KEY]?.toString() ?: run {
                val modFullPath = "${getArtifactRootPath()}/$version.mod"
                val modNode = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, modFullPath))
                storageManager.loadArtifactInputStream(modNode, credentials)?.use { it.readText() }
            }
            val readme = packageVersion.extension[README_KEY]?.toString() ?: run {
                val readmeFullPath = artifactInfo.getReadmeFullPath()
                val readmeNode = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, readmeFullPath))
                storageManager.loadArtifactInputStream(readmeNode, credentials)?.use { it.readText() }
            }
            return GoPackageVersionInfo(basic, packageVersion.packageMetadata, mod, readme)
        }
    }

    override fun deletePackage(userId: String, artifactInfo: ArtifactInfo) = delete(artifactInfo as GoArtifactInfo)

    override fun deleteVersion(userId: String, artifactInfo: ArtifactInfo) = delete(artifactInfo as GoArtifactInfo)
    override fun getRegistryDomain(repositoryType: String): RegistryDomainInfo {
        return RegistryDomainInfo(goProperties.domain)
    }

    override fun buildVersionDeleteArtifactInfo(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): GoArtifactInfo = GoArtifactInfo(
        projectId = projectId,
        repoName = repoName,
        modulePath = PackageKeys.resolveName(packageKey),
        version = version
    )

    @Suppress("NestedBlockDepth")
    fun downloadClient(os: String, arch: String) {
        val latest = GoUtils.getLatestClientVersion(os, arch)
        val name = "$CLIENT_NAME-$os-$arch-v$latest"
        val ais = this.javaClass.classLoader.getResourceAsStream(CLIENT_ARCHIVE_PATH)?.let { ZipInputStream(it) }
            ?: throw ArtifactNotFoundException(CLIENT_NAME)
        val response = HttpContextHolder.getResponse()
        ais.use {
            while (true) {
                val zipEntry = it.nextEntry ?: throw ArtifactNotFoundException(CLIENT_NAME)
                if (zipEntry.name.substringAfter(StringPool.SLASH).equals(name, ignoreCase = true)) {
                    val clientName = if (os.toLowerCase() == "windows") "$CLIENT_NAME.exe" else CLIENT_NAME
                    response.setHeader(CONTENT_DISPOSITION, CONTENT_DISPOSITION_TEMPLATE.format(clientName, clientName))
                    response.setHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM)
                    it.copyTo(response.outputStream)
                    break
                }
            }
        }
    }

    fun listClient() = GoUtils.listClientFile()

    private fun delete(artifactInfo: GoArtifactInfo) {
        repository.remove(ArtifactRemoveContext())
        logger.info("user[${SecurityUtils.getPrincipal()}] delete module[${artifactInfo.getModuleId()}] successfully")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoExtService::class.java)
    }
}
