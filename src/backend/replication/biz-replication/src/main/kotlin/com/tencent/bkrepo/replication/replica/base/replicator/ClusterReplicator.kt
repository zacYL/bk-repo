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

package com.tencent.bkrepo.replication.replica.base.replicator

import com.tencent.bkrepo.common.artifact.constant.RESERVED_KEY
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.stream.rateLimit
import com.tencent.bkrepo.common.storage.innercos.retry
import com.tencent.bkrepo.replication.config.ReplicationProperties
import com.tencent.bkrepo.replication.constant.DEFAULT_VERSION
import com.tencent.bkrepo.replication.constant.DELAY_IN_SECONDS
import com.tencent.bkrepo.replication.constant.RETRY_COUNT
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.replica.base.context.ReplicaContext
import com.tencent.bkrepo.replication.replica.base.impl.internal.PackageNodeMappings
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 集群间数据同步类
 * 独立集群 同步到 独立集群 的同步实现类
 */
@Component
class ClusterReplicator(
    private val localDataManager: LocalDataManager,
    private val replicationProperties: ReplicationProperties
) : Replicator {

    @Value("\${spring.application.version}")
    private var version: String = DEFAULT_VERSION

    override fun checkVersion(context: ReplicaContext) {
        with(context) {
            val remoteVersion = artifactReplicaClient!!.version().data.orEmpty()
            if (version != remoteVersion) {
                logger.warn("Local cluster's version[$version] is different from remote cluster[$remoteVersion].")
            }
        }
    }

    override fun replicaProject(context: ReplicaContext) {
        with(context) {
            // 外部集群仓库没有project/repoName
            if (remoteProjectId.isNullOrBlank()) return
            val localProject = localDataManager.findProjectById(localProjectId)
            val combineName = "${localProject.displayName}---$remoteProjectId"
            val displayName = if (combineName.length > 100) combineName.substring(0, 100) else combineName
            val request = ProjectCreateRequest(
                name = remoteProjectId,
                displayName = displayName,
                description = localProject.description,
                operator = localProject.createdBy
            )
            artifactReplicaClient!!.replicaProjectCreateRequest(request)
        }
    }

    override fun replicaRepo(context: ReplicaContext) {
        with(context) {
            // 外部集群仓库没有project/repoName
            if (remoteProjectId.isNullOrBlank() || remoteRepoName.isNullOrBlank()) return
            val localRepo = localDataManager.findRepoByName(localProjectId, localRepoName, localRepoType.name)
            // 兼容仓库拆分
            val configuration = if (localRepo.category == RepositoryCategory.LOCAL &&
                localRepo.configuration is CompositeConfiguration) {
                val compositeConfiguration = localRepo.configuration as CompositeConfiguration
                LocalConfiguration(compositeConfiguration.webHook, compositeConfiguration.cleanStrategy).apply {
                    this.settings = compositeConfiguration.settings
                }
            } else {
                localRepo.configuration
            }
            val request = RepoCreateRequest(
                projectId = remoteProjectId,
                name = remoteRepoName,
                type = remoteRepoType,
                category = localRepo.category,
                public = localRepo.public,
                description = localRepo.description,
                configuration = configuration,
                operator = localRepo.createdBy
            )
            context.remoteRepo = artifactReplicaClient!!.replicaRepoCreateRequest(request).data!!
        }
    }

    override fun replicaPackage(context: ReplicaContext, packageSummary: PackageSummary) {
        // do nothing
    }

    override fun replicaPackageVersion(
        context: ReplicaContext,
        packageSummary: PackageSummary,
        packageVersion: PackageVersion
    ): Boolean {
        with(context) {
            // 外部集群仓库没有project/repoName
            if (remoteProjectId.isNullOrBlank() || remoteRepoName.isNullOrBlank()) return true
            // 文件数据
            PackageNodeMappings.map(
                packageSummary = packageSummary,
                packageVersion = packageVersion,
                type = localRepoType
            ).forEach {
                val node = localDataManager.findNodeDetail(
                    projectId = localProjectId,
                    repoName = localRepoName,
                    fullPath = it
                )
                replicaFile(context, node.nodeInfo)
            }
            // filter system reserve metadata
            val packageMetadata = packageVersion.packageMetadata.filter {
                it.key !in RESERVED_KEY
            } as MutableList<MetadataModel>
            packageMetadata.add(MetadataModel(SOURCE_TYPE, ArtifactChannel.REPLICATION, system = true, display = true))
            val manifestPath = if (packageSummary.type == PackageType.DOCKER) packageVersion.manifestPath else null
            // 包数据
            val request = PackageVersionCreateRequest(
                projectId = remoteProjectId,
                repoName = remoteRepoName,
                packageName = packageSummary.name,
                packageKey = packageSummary.key,
                packageType = packageSummary.type,
                packageDescription = packageSummary.description,
                versionName = packageVersion.name,
                size = packageVersion.size,
                manifestPath = manifestPath,
                artifactPath = packageVersion.contentPath,
                stageTag = packageVersion.stageTag,
                packageMetadata = packageMetadata,
                extension = packageVersion.extension,
                overwrite = true,
                createdBy = packageVersion.createdBy
            )
            artifactReplicaClient!!.replicaPackageVersionCreatedRequest(request)
        }
        return true
    }

    override fun replicaFile(context: ReplicaContext, node: NodeInfo): Boolean {
        with(context) {
            retry(times = RETRY_COUNT, delayInSeconds = DELAY_IN_SECONDS) { retry ->
                return buildNodeCreateRequest(this, node)?.let {
                    val artifactInputStream = localDataManager.getBlobData(it.sha256!!, it.size!!, localRepo)
                    val rateLimitInputStream = artifactInputStream.rateLimit(localDataManager.getRateLimit().toBytes())
                    // 1. 同步文件数据
                    logger.info("The file [${node.fullPath}] with sha256 [${node.sha256}] " +
                        "will be pushed to the remote server ${cluster.name},try the $retry time!")
                    pushBlob(
                        inputStream = rateLimitInputStream,
                        size = it.size!!,
                        sha256 = it.sha256.orEmpty(),
                        storageKey = remoteRepo?.storageCredentials?.key
                    )
                    // 2. 同步节点信息
                    artifactReplicaClient!!.replicaNodeCreateRequest(it)
                    true
                } ?: false
            }
        }
    }

    override fun replicaDir(context: ReplicaContext, node: NodeInfo) {
        with(context) {
            buildNodeCreateRequest(this, node)?.let {
                artifactReplicaClient!!.replicaNodeCreateRequest(it)
            }
        }
    }

    private fun buildNodeCreateRequest(context: ReplicaContext, node: NodeInfo): NodeCreateRequest? {
        with(context) {
            // 外部集群仓库没有project/repoName
            if (remoteProjectId.isNullOrBlank() || remoteRepoName.isNullOrBlank()) return null
            // 查询元数据
            val metadata = if (task.setting.includeMetadata) {
                // filter system reserve metadata
                node.nodeMetadata?.filter {
                    it.key !in RESERVED_KEY
                }
            } else {
                emptyList()
            }
            return NodeCreateRequest(
                projectId = remoteProjectId,
                repoName = remoteRepoName,
                fullPath = node.fullPath,
                folder = node.folder,
                overwrite = true,
                size = node.size,
                sha256 = node.sha256!!,
                md5 = node.md5!!,
                nodeMetadata = metadata,
                operator = node.createdBy,
                createdBy = node.createdBy,
                createdDate = LocalDateTime.parse(node.createdDate, DateTimeFormatter.ISO_DATE_TIME),
                lastModifiedBy = node.lastModifiedBy,
                lastModifiedDate = LocalDateTime.parse(node.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterReplicator::class.java)
    }
}
