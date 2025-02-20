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

import com.tencent.bkrepo.cocoapods.constant.DOT_SPECS
import com.tencent.bkrepo.cocoapods.event.consumer.RemoteEventJobExecutor
import com.tencent.bkrepo.cocoapods.exception.CocoapodsMessageCode
import com.tencent.bkrepo.cocoapods.utils.DecompressUtil.buildEmptySpecGzOps
import com.tencent.bkrepo.cocoapods.utils.ObjectBuildUtil
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateIndexTarPath
import com.tencent.bkrepo.common.api.constant.CharPool.SLASH
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_GZIP
import com.tencent.bkrepo.common.api.constant.ensurePrefix
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResourceWriterContext
import com.tencent.bkrepo.common.artifact.util.http.HttpHeaderUtils.encodeDisposition
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder.getResponse
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import java.io.IOException
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CocoapodsIndexService(
    private val nodeService: NodeService,
    private val repositoryService: RepositoryService,
    private val storageManager: StorageManager,
    private val cocoapodsRepoService: CocoapodsRepoService,
    private val cocoapodsSpecsService: CocoapodsSpecsService,
    private val remoteEventJobExecutor: RemoteEventJobExecutor,
    private val artifactResourceWriterContext: ArtifactResourceWriterContext,
) {

    /**
     * 更新远程仓库的podspec 索引
     */
    fun updateRemoteSpecs(projectId: String, repoName: String) {
        val indexGenerating =
            cocoapodsRepoService.getStringSetting(projectId, repoName, CocoapodsSpecsService.INDEX_GENERATING)
        if (CocoapodsSpecsService.INDEX_GENERATING_VALUE_DOING == indexGenerating) {
            throw ErrorCodeException(CocoapodsMessageCode.COCOAPODS_INDEX_UPDATING_ERROR)
        }
        remoteEventJobExecutor.execute(
            ObjectBuildUtil.buildCreatedEvent(
                projectId,
                repoName,
                SecurityUtils.getUserId()
            )
        )
    }

    fun downloadSpecs(projectId: String, repoName: String) {
        val repoDetail = repositoryService.getRepoDetail(projectId, repoName)
            ?: throw RepoNotFoundException(repoName)
        val resource = when (repoDetail.category) {
            RepositoryCategory.LOCAL -> {
                //下载index文件,将.specs目录下的文件压缩返回
                val prefix = SLASH + DOT_SPECS
                val nodes = queryNodeDetailList(
                    projectId = projectId,
                    repoName = repoName,
                    prefix = prefix
                )
                val nodeMap = nodes.filterNot { it.folder }.associate {
                    val name =
                        it.fullPath.removePrefix(prefix).ensurePrefix(com.tencent.bkrepo.cocoapods.constant.SPECS)
                    name to run {
                        nodeService.updateRecentlyUseDate(it.projectId, it.repoName, it.fullPath)
                        storageManager.loadArtifactInputStream(it, repoDetail.storageCredentials)
                            ?: throw ArtifactNotFoundException(it.fullPath)
                    }
                }
                if (nodeMap.isEmpty()) {
                    returnEmptySpec()
                    return
                }
                ArtifactResource(
                    artifactMap = nodeMap,
                    srcRepo = RepositoryIdentify(projectId, repoName),
                    useDisposition = true,
                ).apply { contentType = APPLICATION_GZIP }
            }

            RepositoryCategory.REMOTE -> {
                //下载index文件
                if (cocoapodsSpecsService.indexExist(projectId, repoName).not()) {
                    logger.warn("repo $repoName index file not exist")
                    remoteEventJobExecutor.execute(
                        ObjectBuildUtil.buildCreatedEvent(
                            projectId,
                            repoName,
                            SecurityUtils.getUserId()
                        )
                    )
                    returnEmptySpec()
                    return
                }
                val node = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, generateIndexTarPath()))
                    ?: throw NodeNotFoundException(generateIndexTarPath())
                val inputStream = storageManager.loadArtifactInputStream(node, repoDetail.storageCredentials)
                    ?: throw ArtifactNotFoundException(generateIndexTarPath())
                ArtifactResource(
                    inputStream = inputStream,
                    artifactName = generateIndexTarPath(),
                    srcRepo = RepositoryIdentify(projectId, repoName),
                ).apply { contentType = APPLICATION_GZIP }
            }

            else -> throw RepoNotFoundException(repoName)
        }
        artifactResourceWriterContext.getWriter(resource).write(resource)
    }

    private fun returnEmptySpec() {
        val response = getResponse()

        try {
            response.contentType = APPLICATION_GZIP
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, encodeDisposition("response.gz"))
            val gzipOutputStream = buildEmptySpecGzOps(response)
            gzipOutputStream.finish()
        } catch (e: IOException) {
            logger.error("Error occurred while creating archive.", e)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create archive.")
        }
    }

    private fun queryNodeDetailList(
        projectId: String,
        repoName: String,
        prefix: String,
    ): List<NodeDetail> {
        var pageNumber = 1
        val nodeDetailList = mutableListOf<NodeDetail>()
        val count = nodeService.countFileNode(ArtifactInfo(projectId, repoName, prefix))
        do {
            val option = NodeListOption(
                pageNumber = pageNumber,
                pageSize = 1000,
                includeFolder = true,
                includeMetadata = true,
                deep = true
            )
            val records = nodeService.listNodePage(ArtifactInfo(projectId, repoName, prefix), option).records
            if (records.isEmpty()) {
                break
            }
            nodeDetailList.addAll(
                records.map { NodeDetail(it) }
            )
            pageNumber++
        } while (nodeDetailList.size < count)
        return nodeDetailList
    }

    fun initRemoteSpecs(projectId: String, repoName: String) {
        TODO("Not yet implemented")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
