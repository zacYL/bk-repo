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

package com.tencent.bkrepo.cocoapods.service

import com.tencent.bkrepo.cocoapods.exception.CocoapodsMessageCode
import com.tencent.bkrepo.cocoapods.exception.CocoapodsCommonException
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.cocoapods.utils.ObjectBuildUtil.buildBasicInfo
import com.tencent.bkrepo.cocoapods.utils.PathUtil
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CocoapodsWebService(
    private val nodeClient: NodeClient,
    private val packageClient: PackageClient,
    private val cocoapodsPackageService: CocoapodsPackageService,
) {
    //TODO 逻辑待优化。当前为遍历版本，取其中一个的contentPath解析出orgName然后删除整个路径
    fun deletePackage(artifactInfo: CocoapodsArtifactInfo, packageKey: String) {
        logger.info("cocoapods delete package...")
        with(artifactInfo) {
            packageClient.listAllVersion(projectId, repoName, packageKey).data?.forEach {
                packageClient.deleteVersion(projectId, repoName, packageKey, it.name)
                artifactInfo.orgName = PathUtil.getOrgNameByVersion(it);
            } ?: logger.warn("[$projectId/$repoName/$packageKey] version not found")
            packageClient.deletePackage(projectId, repoName, packageKey)
            name = PackageKeys.resolveCocoapods(packageKey)
            cocoapodsPackageService.deletePackageFile(artifactInfo)
        }
    }

    fun deleteVersion(artifactInfo: CocoapodsArtifactInfo, packageKey: String, version: String) {
        logger.info("cocoapods delete version...")
        with(artifactInfo) {
            //直接从packageKey解析制品包名称
            name = PackageKeys.resolveCocoapods(packageKey)
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw PackageNotFoundException(packageKey)
            }
            packageClient.deleteVersion(projectId, repoName, packageKey, version)
            cocoapodsPackageService.deleteVersionFile(artifactInfo, packageVersion)
            logger.info("delete artifactInfo:[$artifactInfo],packageVersion:[$packageVersion]")
        }
    }

    fun artifactDetail(
        cocoapodsArtifactInfo: CocoapodsArtifactInfo,
        packageKey: String,
        version: String,
    ): PackageVersionInfo {
        with(cocoapodsArtifactInfo) {
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw PackageNotFoundException(packageKey)
            }
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, packageVersion.contentPath!!).data ?: run {
                logger.warn("node [${packageVersion.contentPath}] don't found.")
                throw CocoapodsCommonException(CocoapodsMessageCode.COCOAPODS_PODSPEC_NOT_FOUND)
            }
            return PackageVersionInfo(buildBasicInfo(nodeDetail, packageVersion), packageVersion.packageMetadata)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsWebService::class.java)
    }
}
