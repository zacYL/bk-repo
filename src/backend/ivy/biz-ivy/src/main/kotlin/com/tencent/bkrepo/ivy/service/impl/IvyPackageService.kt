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

import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.ivy.artifact.IvyArtifactInfo
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_All_ARTIFACT_FULL_PATH
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_IVY_FULL_PATH
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.node.service.NodesDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class IvyPackageService(
    private val packageClient: PackageClient,
    private val nodeClient: NodeClient
) {
    fun deleteVersionFile(artifactInfo: IvyArtifactInfo, packageVersion: PackageVersion) {
        with(artifactInfo) {
            val versionArtifactFullPath = getVersionArtifactFullPath(artifactInfo, packageVersion)
            if (versionArtifactFullPath.isNotEmpty()) {
                nodeClient.deleteNodes(
                    NodesDeleteRequest(
                        projectId = projectId,
                        repoName = repoName,
                        fullPaths = versionArtifactFullPath,
                        operator = SecurityUtils.getUserId(),
                        isFolder = false
                    )
                )
            }
        }
    }

    fun deletePackageFile(artifactInfo: IvyArtifactInfo, versions: List<PackageVersion>?) {
        with(artifactInfo) {
            val allVersionArtifactFullPath = versions?.flatMap { version ->
                getVersionArtifactFullPath(artifactInfo, version)
            } ?: emptyList()

            if (allVersionArtifactFullPath.isNotEmpty()) {
                nodeClient.deleteNodes(
                    NodesDeleteRequest(
                        projectId = projectId,
                        repoName = repoName,
                        fullPaths = allVersionArtifactFullPath,
                        operator = SecurityUtils.getUserId(),
                        isFolder = false
                    )
                )
            }
        }
    }

    private fun getVersionArtifactFullPath(artifactInfo: IvyArtifactInfo, version: PackageVersion): List<String> {
        val allArtifactFullPath =
            (version.packageMetadata.firstOrNull() { it.key == METADATA_KEY_All_ARTIFACT_FULL_PATH }?.value) as MutableList<String>?
        val ivyFullPath =
            (version.packageMetadata.firstOrNull() { it.key == METADATA_KEY_IVY_FULL_PATH }?.value) as String
        allArtifactFullPath?.add(ivyFullPath)
        allArtifactFullPath?.addAll(artifactInfo.artifactsToSummaryPath(allArtifactFullPath))

        return allArtifactFullPath ?: emptyList()
    }


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IvyPackageService::class.java)
    }
}
