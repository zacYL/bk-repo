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

package com.tencent.bkrepo.conan.service.impl

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.conan.constant.CHANNEL
import com.tencent.bkrepo.conan.constant.ConanMessageCode
import com.tencent.bkrepo.conan.constant.USERNAME
import com.tencent.bkrepo.conan.exception.ConanFileNotFoundException
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.conan.service.ConanDeleteService
import com.tencent.bkrepo.conan.service.ConanWebService
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil.buildBasicInfo
import com.tencent.bkrepo.conan.utils.PathUtils.extractNameFromReference
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.PackageMetadataClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ConanWebServiceImpl(
    private val nodeClient: NodeClient,
    private val packageClient: PackageClient,
    private val packageMetadataClient: PackageMetadataClient,
    private val conanDeleteService: ConanDeleteService,
) : ConanWebService {

    override fun artifactDetail(conanArtifactInfo: ConanArtifactInfo, packageKey: String, version: String): PackageVersionInfo? {
        with(conanArtifactInfo) {
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw PackageNotFoundException(packageKey)
            }
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, packageVersion.contentPath!!).data ?: run {
                logger.warn("node [${packageVersion.contentPath}] don't found.")
                throw ConanFileNotFoundException(ConanMessageCode.CONAN_RECIPE_NOT_FOUND, packageVersion.contentPath!!, "$projectId|$repoName")
            }
            return PackageVersionInfo(buildBasicInfo(nodeDetail, packageVersion), packageVersion.packageMetadata)
        }
    }

    override fun deletePackage(artifactInfo: ConanArtifactInfo, packageKey: String) {
        logger.info("conan delete package...")
        val refStr = PackageKeys.resolveConan(packageKey)
        with(artifactInfo) {
            packageClient.listAllVersion(projectId, repoName, packageKey).data?.forEach { it ->
                packageClient.deleteVersion(projectId, repoName, packageKey, it.name)
                val copyArtifactInfo = artifactInfo.copy() as ConanArtifactInfo
                copyArtifactInfo.apply {
                    this.name = extractNameFromReference(refStr)
                    this.userName = it.metadata[USERNAME]?.toString() ?: StringPool.UNDERSCORE
                    this.channel = it.metadata[CHANNEL]?.toString() ?: StringPool.UNDERSCORE
                    version = it.name
                }
                logger.info("delete package artifactInfo [$copyArtifactInfo]")
                conanDeleteService.removeConanFile(copyArtifactInfo)
            } ?: logger.warn("[$projectId/$repoName/$packageKey] version not found")
        }
    }

    override fun deleteVersion(artifactInfo: ConanArtifactInfo, packageKey: String, version: String) {
        logger.info("conan delete version...")
        val refStr = PackageKeys.resolveConan(packageKey)
        with(artifactInfo) {
            packageMetadataClient.listMetadata(projectId,repoName,packageKey,version).data?.let {
                apply {
                    this.name = extractNameFromReference(refStr)
                    this.userName = it[USERNAME]?.toString() ?: StringPool.UNDERSCORE
                    this.channel = it[CHANNEL]?.toString() ?: StringPool.UNDERSCORE
                    this.version = version
                }
                logger.info("delete version artifactInfo [$artifactInfo]")
                conanDeleteService.removeConanFile(artifactInfo)
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ConanWebServiceImpl::class.java)

    }
}
