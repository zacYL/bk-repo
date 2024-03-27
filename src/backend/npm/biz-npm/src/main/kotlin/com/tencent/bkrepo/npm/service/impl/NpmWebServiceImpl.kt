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

package com.tencent.bkrepo.npm.service.impl

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.exception.NpmArtifactNotFoundException
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.NpmDomainInfo
import com.tencent.bkrepo.npm.pojo.user.BasicInfo
import com.tencent.bkrepo.npm.pojo.user.DependenciesInfo
import com.tencent.bkrepo.npm.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.npm.pojo.user.VersionDependenciesInfo
import com.tencent.bkrepo.npm.service.NpmClientService
import com.tencent.bkrepo.npm.service.NpmWebService
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.PackageDependentsClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class NpmWebServiceImpl : NpmWebService, AbstractNpmService() {

    @Autowired
    private lateinit var packageDependentsClient: PackageDependentsClient

    @Autowired
    private lateinit var npmClientService: NpmClientService

    @Transactional(rollbackFor = [Throwable::class])
    override fun detailVersion(artifactInfo: NpmArtifactInfo, packageKey: String, version: String): PackageVersionInfo {
        with(artifactInfo) {
            val name = PackageKeys.resolveNpm(packageKey)
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw VersionNotFoundException(name)
            }
            val fullPath = packageVersion.contentPath!!
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run {
                logger.warn("node [$fullPath] don't found.")
                throw NpmArtifactNotFoundException("node [$fullPath] don't found.")
            }
            val versionMetadata = try {
                npmClientService.packageVersionInfo(artifactInfo, name, version)
            } catch (e: NpmArtifactNotFoundException) {
                loadPackageMetadata(ArtifactContext())?.versions?.map?.get(version)
                    ?: throw NpmArtifactNotFoundException("version metadata of [$name/$version] not found")
            }
            return PackageVersionInfo(
                basic = buildBasicInfo(nodeDetail, packageVersion, versionMetadata.readme.orEmpty()),
                metadata = packageVersion.packageMetadata,
                dependencyInfo = queryVersionDependenciesInfo(artifactInfo, packageKey, versionMetadata)
            )
        }
    }

    private fun queryVersionDependenciesInfo(
        artifactInfo: NpmArtifactInfo,
        packageKey: String,
        versionMetadata: NpmVersionMetadata
    ): VersionDependenciesInfo {
        val packageDependents = packageDependentsClient.queryDependents(
            artifactInfo.projectId,
            artifactInfo.repoName, packageKey
        ).data.orEmpty()
        val dependenciesList = parseDependencies(versionMetadata)
        val devDependenciesList = parseDevDependencies(versionMetadata)
        return VersionDependenciesInfo(dependenciesList, devDependenciesList, packageDependents)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deletePackage(artifactInfo: NpmArtifactInfo) = npmClientService.deletePackage(artifactInfo)

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteVersion(artifactInfo: NpmArtifactInfo) {
        with(artifactInfo) {
            val packageKey = PackageKeys.ofNpm(packageName)
            val packageInfo = packageClient.findPackageByKey(projectId, repoName, packageKey).data
                ?: throw PackageNotFoundException(packageKey)
            // 如果删除最后一个版本直接删除整个包
            if (packageInfo.versions == 1L) {
                deletePackage(NpmArtifactInfo(projectId, repoName, packageName, null))
            } else {
                npmClientService.deleteVersion(artifactInfo)
                val repoCategory = ArtifactContextHolder.getRepoDetail()?.category
                if (repoCategory == RepositoryCategory.COMPOSITE || repoCategory == RepositoryCategory.LOCAL) {
                    val packageMetadata = loadPackageMetadata(ArtifactContext())
                        ?: throw NpmArtifactNotFoundException(
                            "failed to load metadata of [$this] after deleting version"
                        )
                    updatePackageWithDeleteVersion(this, packageMetadata)
                }
            }
        }
    }

    override fun getRegistryDomain(): NpmDomainInfo {
        return NpmDomainInfo(UrlFormatter.formatHost(npmProperties.domain))
    }

    fun updatePackageWithDeleteVersion(
        artifactInfo: NpmArtifactInfo,
        packageMetaData: NpmPackageMetaData
    ) {
        with(artifactInfo) {
            require(version != null)
            val latest = NpmUtils.getLatestVersionFormDistTags(packageMetaData.distTags)
            // 删除versions里面对应的版本
            packageMetaData.versions.map.remove(version)
            packageMetaData.time.getMap().remove(version)
            val iterator = packageMetaData.distTags.getMap().entries.iterator()
            while (iterator.hasNext()) {
                if (version == iterator.next().value) {
                    iterator.remove()
                }
            }
            if (version == latest) {
                val newLatest = findNewLatest(this)
                packageMetaData.distTags.set(LATEST, newLatest)
            }
            reUploadPackageJsonFile(artifactInfo, packageMetaData)
        }
    }

    private fun findNewLatest(artifactInfo: NpmArtifactInfo): String {
        return with(artifactInfo) {
            packageClient.findPackageByKey(projectId, repoName, PackageKeys.ofNpm(packageName)).data?.latest
                ?: run {
                    val message =
                        "delete version by web operator to find new latest version failed with package [$packageName]"
                    logger.error(message)
                    throw NpmArtifactNotFoundException(message)
                }
        }
    }

    fun reUploadPackageJsonFile(artifactInfo: NpmArtifactInfo, packageMetaData: NpmPackageMetaData) {
        with(artifactInfo) {
            val fullPath = NpmUtils.getPackageMetadataPath(packageMetaData.name!!)
            val inputStream = JsonUtils.objectMapper.writeValueAsString(packageMetaData).byteInputStream()
            val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
            val context = ArtifactUploadContext(artifactFile)
            context.putAttribute(NPM_FILE_FULL_PATH, fullPath)

            repository.upload(context).also {
                logger.info(
                    "user [${context.userId}] upload npm package metadata file [$fullPath] " +
                            "to repo [$projectId/$repoName] success."
                )
            }
            artifactFile.delete()
        }
    }

    private fun parseDependencies(versionMetadata: NpmVersionMetadata): MutableList<DependenciesInfo> {
        val dependenciesList: MutableList<DependenciesInfo> = mutableListOf()
        if (versionMetadata.dependencies != null) {
            versionMetadata.dependencies!!.entries.forEach { (key, value) ->
                dependenciesList.add(
                    DependenciesInfo(
                        key,
                        value.toString()
                    )
                )
            }
        }
        return dependenciesList
    }

    private fun parseDevDependencies(versionMetadata: NpmVersionMetadata): MutableList<DependenciesInfo> {
        val devDependenciesList: MutableList<DependenciesInfo> = mutableListOf()
        if (versionMetadata.devDependencies != null) {
            versionMetadata.devDependencies!!.entries.forEach { (key, value) ->
                devDependenciesList.add(
                    DependenciesInfo(
                        key,
                        value.toString()
                    )
                )
            }
        }
        return devDependenciesList
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(NpmWebServiceImpl::class.java)

        fun buildBasicInfo(nodeDetail: NodeDetail, packageVersion: PackageVersion, readmeInfo: String?): BasicInfo {
            with(nodeDetail) {
                return BasicInfo(
                    packageVersion.name,
                    fullPath,
                    size,
                    sha256!!,
                    md5!!,
                    packageVersion.stageTag,
                    projectId,
                    repoName,
                    packageVersion.downloads,
                    createdBy,
                    packageVersion.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy,
                    packageVersion.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    readme = readmeInfo
                )
            }
        }
    }
}
