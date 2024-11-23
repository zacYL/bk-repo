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

import com.tencent.bkrepo.cocoapods.artifact.CocoapodsProperties
import com.tencent.bkrepo.cocoapods.constant.DOT_SPECS
import com.tencent.bkrepo.cocoapods.constant.SPECS
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.service.CocoapodsPackageService
import com.tencent.bkrepo.cocoapods.utils.DecompressUtil.getPodSpec
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateCachePath
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateFullPath
import com.tencent.bkrepo.common.api.constant.CharPool.SLASH
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.constant.ensurePrefix
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

@Component
class CocoapodsLocalRepository(
    private val cocoapodsProperties: CocoapodsProperties,
    private val cocoapodsPackageService: CocoapodsPackageService,
) : LocalRepository() {
    override fun onUploadBefore(context: ArtifactUploadContext) {
        //todo 校验文件
        super.onUploadBefore(context)
        with(context.artifactInfo as CocoapodsArtifactInfo) {
            packageClient
                .findVersionByName(projectId, repoName, PackageKeys.ofCocoapods(name), version).data
                ?.apply { uploadIntercept(context, this) }
        }
    }

    override fun onUploadSuccess(context: ArtifactUploadContext) {

        with(context) {
            val artifactInfo = artifactInfo as CocoapodsArtifactInfo
            //在.specs目录创建索引文件
            getArtifactFile().getInputStream().use {
                val tarFilePath = generateCachePath(artifactInfo, cocoapodsProperties.domain)
                val (fileName, podSpec) = it.getPodSpec(tarFilePath)
                ByteArrayOutputStream().use { bos ->
                    OutputStreamWriter(bos, Charsets.UTF_8).use { writer ->
                        writer.write(podSpec)
                    }
                    val specArtifact = ArtifactFileFactory.build(bos.toByteArray().inputStream())
                    val uploadContext = ArtifactUploadContext(specArtifact)
                    val specNode = buildNodeCreateRequest(uploadContext).run {
                        copy(fullPath = "$DOT_SPECS/${artifactInfo.name}/${artifactInfo.version}/${fileName}")
                    }
                    storageManager.storeArtifactFile(specNode, specArtifact, uploadContext.storageCredentials)
                }
            }
            //创建包版本
            cocoapodsPackageService.createVersion(artifactInfo, getArtifactFile().getSize())
        }
        super.onUploadSuccess(context)
    }

    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        with(context) {
            val fullPath = generateFullPath(artifactInfo as CocoapodsArtifactInfo)
            logger.info("File $fullPath will be stored in $projectId|$repoName")
            return NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                folder = false,
                fullPath = fullPath,
                size = getArtifactFile().getSize(),
                sha256 = getArtifactSha256(),
                md5 = getArtifactMd5(),
                operator = userId,
                overwrite = true
            )
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        if (context.artifactInfo !is CocoapodsArtifactInfo) {
            //下载index文件,将.specs目录下的文件压缩返回
            val prefix = SLASH + DOT_SPECS
            with(context) {
                val nodes = queryNodeDetailList(
                    projectId = projectId,
                    repoName = repoName,
                    prefix = prefix
                )
                val nodeMap = nodes.filterNot { it.folder }.associate {
                    val name = it.fullPath.removePrefix(prefix).ensurePrefix(SPECS)
                    name to run {
                        nodeClient.updateRecentlyUseDate(it.projectId, it.repoName, it.fullPath)
                        storageManager.loadArtifactInputStream(it, storageCredentials)
                            ?: throw ArtifactNotFoundException(it.fullPath)
                    }
                }
                return ArtifactResource(
                    artifactMap = nodeMap,
                    srcRepo = RepositoryIdentify(projectId, repoName),
                    useDisposition = true,
                    contentType = MediaTypes.APPLICATION_GZIP
                )
            }
        } else {
            //下载包文件
            return null
        }
    }

    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
        val artifactInfo = context.artifactInfo
        if (artifactInfo is CocoapodsArtifactInfo) {
            with(artifactInfo) {
                packageClient
                    .findVersionByName(projectId, repoName, PackageKeys.ofCocoapods(name), version).data
                    ?.apply { packageDownloadIntercept(context, this) }
            }
        }

    }

    private fun queryNodeDetailList(
        projectId: String,
        repoName: String,
        prefix: String,
    ): List<NodeDetail> {
        var pageNumber = 1
        val nodeDetailList = mutableListOf<NodeDetail>()
        val count = nodeClient.countFileNode(projectId, repoName, prefix).data ?: 0
        do {
            val option = NodeListOption(
                pageNumber = pageNumber,
                pageSize = 1000,
                includeFolder = true,
                includeMetadata = true,
                deep = true
            )
            val records = nodeClient.listNodePage(projectId, repoName, prefix, option).data?.records
            if (records.isNullOrEmpty()) {
                break
            }
            nodeDetailList.addAll(
                records.map { NodeDetail(it) }
            )
            pageNumber++
        } while (nodeDetailList.size < count)
        return nodeDetailList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsLocalRepository::class.java)
    }
}
