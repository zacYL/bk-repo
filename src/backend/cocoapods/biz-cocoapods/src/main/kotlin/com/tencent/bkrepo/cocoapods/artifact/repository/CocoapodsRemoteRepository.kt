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

package com.tencent.bkrepo.cocoapods.artifact.repository

import com.tencent.bkrepo.cocoapods.dao.CocoapodsRemotePackageDao
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.service.CocoapodsPackageService
import com.tencent.bkrepo.cocoapods.utils.PathUtil.toDownloadUrl
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CocoapodsRemoteRepository(
    private val cocoapodsRemotePackageDao: CocoapodsRemotePackageDao,
    private val cocoapodsPackageService: CocoapodsPackageService,
) : RemoteRepository() {

    override fun createRemoteDownloadUrl(context: ArtifactContext): String {
        with(context.artifactInfo as CocoapodsArtifactInfo) {
            val remotePackage = cocoapodsRemotePackageDao.findOne(projectId, repoName, name, version)
                ?: throw PackageNotFoundException(name)
            return remotePackage.source.toDownloadUrl() ?: throw PackageNotFoundException(name)
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        val artifactResource = super.onDownload(context)
        artifactResource?.node?.let { nodeDownloadIntercept(context,it) }
        return artifactResource
    }

    override fun buildCacheNodeCreateRequest(context: ArtifactContext, artifactFile: ArtifactFile): NodeCreateRequest {
        val metadataList = (context.artifactInfo as CocoapodsArtifactInfo).generateMetadata() +
            MetadataModel(key = SOURCE_TYPE, value = ArtifactChannel.PROXY, system = true)
        return super.buildCacheNodeCreateRequest(context, artifactFile).copy(nodeMetadata = metadataList)
    }

    override fun onDownloadSuccess(context: ArtifactDownloadContext, artifactResource: ArtifactResource, throughput: Throughput) {
        val cocoapodsArtifactInfo = context.artifactInfo as CocoapodsArtifactInfo
        logger.info("Repo [${cocoapodsArtifactInfo.repoName}] Download artifact [${cocoapodsArtifactInfo.name}:${cocoapodsArtifactInfo.version}] successfully.")
        cocoapodsPackageService.createVersion(cocoapodsArtifactInfo, artifactResource.getTotalSize())
        super.onDownloadSuccess(context, artifactResource, throughput)
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
    ): PackageDownloadRecord? {
        val artifactInfo = context.artifactInfo
        return if (artifactInfo is CocoapodsArtifactInfo) {
            cocoapodsPackageService.buildDownloadRecord(artifactInfo, context.userId)
        } else null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsRemoteRepository::class.java)
    }
}
