/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.ivy.service.impl

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import com.tencent.bkrepo.common.metadata.service.packages.PackageService
import com.tencent.bkrepo.ivy.artifact.IvyArtifactInfo
import com.tencent.bkrepo.ivy.enum.IvyMessageCode.IVY_ARTIFACT_NOT_FOUND
import com.tencent.bkrepo.ivy.exception.IvyArtifactNotFoundException
import com.tencent.bkrepo.ivy.pojo.BasicInfo
import com.tencent.bkrepo.ivy.pojo.PackageVersionInfo
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import java.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class IvyWebService(
    private val ivyPackageService: IvyPackageService,
    private val packageService: PackageService,
    private val nodeService: NodeService
) {

    fun deletePackage(artifactInfo: IvyArtifactInfo, packageKey: String) {
        logger.info("ivy delete package...")
        with(artifactInfo) {
            val versions = packageService.listAllVersion(projectId, repoName, packageKey, VersionListOption())
            if (versions.isEmpty()) {
                logger.warn("[$projectId/$repoName/$packageKey] version not found")
            }
            versions.forEach {
                packageService.deleteVersion(projectId, repoName, packageKey, it.name)
            }
            packageService.deletePackage(projectId, repoName, packageKey)
            ivyPackageService.deletePackageFile(artifactInfo, versions)
        }
    }

    fun deleteVersion(artifactInfo: IvyArtifactInfo, packageKey: String, version: String) {
        logger.info("ivy delete version...")
        with(artifactInfo) {
            val packageVersion = packageService.findVersionByName(projectId, repoName, packageKey, version) ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw PackageNotFoundException(packageKey)
            }
            packageService.deleteVersion(projectId, repoName, packageKey, version)
            ivyPackageService.deleteVersionFile(artifactInfo, packageVersion)
            logger.info("delete artifactInfo:[$artifactInfo],packageVersion:[$packageVersion]")
        }
    }

    fun artifactDetail(
        ivyArtifactInfo: IvyArtifactInfo,
        packageKey: String,
        version: String,
    ): PackageVersionInfo {
        with(ivyArtifactInfo) {
            val packageVersion = packageService.findVersionByName(projectId, repoName, packageKey, version) ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw PackageNotFoundException(packageKey)
            }
            val nodeDetail =
                nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, packageVersion.contentPath!!)) ?: run {
                    logger.warn("node [${packageVersion.contentPath}] don't found.")
                    throw IvyArtifactNotFoundException(IVY_ARTIFACT_NOT_FOUND)
                }
            return PackageVersionInfo(buildBasicInfo(nodeDetail, packageVersion), packageVersion.packageMetadata)
        }
    }

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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IvyWebService::class.java)
    }
}
