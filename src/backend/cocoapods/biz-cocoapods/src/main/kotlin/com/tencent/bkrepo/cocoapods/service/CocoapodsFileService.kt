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

import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.utils.PathUtil
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CocoapodsFileService(
    private val nodeClient: NodeClient,
) {
    fun deleteFile(artifactInfo: CocoapodsArtifactInfo) {
        with(artifactInfo) {
            //删除包文件:"$orgName/$name/$version/$fileName"
            var request = NodeDeleteRequest(projectId, repoName,
                PathUtil.generateFullPath(artifactInfo), SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            //删除specs文件:".specs/$name/$version/$name.podspec"
            request = NodeDeleteRequest(projectId, repoName,
                PathUtil.generateSpecsPath(artifactInfo), SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
        }
    }

    fun deleteVersionFile(artifactInfo: CocoapodsArtifactInfo, packageVersion: PackageVersion) {
        with(artifactInfo) {
            val filePath = packageVersion.contentPath?.substringBeforeLast("/")
            val specsPath = ".specs/${name}/${packageVersion.name}"
            var request = NodeDeleteRequest(projectId, repoName, filePath ?: "", SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            logger.info("delete filePath:[$filePath]")
            request = NodeDeleteRequest(projectId, repoName, specsPath ?: "", SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            logger.info("delete specsPath:[$specsPath]")
        }
    }

    fun deletePackageFile(artifactInfo: CocoapodsArtifactInfo) {
        logger.info("delete package file...")
        with(artifactInfo) {
            val packageFilePath = "/$orgName"
            val packageSpecsPath = ".specs/$name"
            var request = NodeDeleteRequest(projectId, repoName, packageFilePath ?: "", SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            logger.info("delete packageFilePath:[$packageFilePath]")
            request = NodeDeleteRequest(projectId, repoName, packageSpecsPath ?: "", SecurityUtils.getUserId())
            nodeClient.deleteNode(request)
            logger.info("delete packageSpecsPath:[$packageSpecsPath]")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsWebService::class.java)
    }
}
