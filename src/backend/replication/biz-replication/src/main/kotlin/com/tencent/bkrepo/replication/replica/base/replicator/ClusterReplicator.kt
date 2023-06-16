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

import com.google.common.base.Throwables
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.artifact.cluster.ClusterInfo
import com.tencent.bkrepo.common.artifact.constant.RESERVED_KEY
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.storage.innercos.retry
import com.tencent.bkrepo.replication.config.ReplicationProperties
import com.tencent.bkrepo.replication.constant.DEFAULT_VERSION
import com.tencent.bkrepo.replication.constant.DELAY_IN_SECONDS
import com.tencent.bkrepo.replication.constant.PUSH_WITH_DEFAULT
import com.tencent.bkrepo.replication.constant.RETRY_COUNT
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.replica.base.context.FilePushContext
import com.tencent.bkrepo.replication.replica.base.context.ReplicaContext
import com.tencent.bkrepo.replication.replica.base.handler.ClusterArtifactReplicationHandler
import com.tencent.bkrepo.replication.replica.base.impl.internal.PackageNodeMappings
import com.tencent.bkrepo.replication.replica.base.impl.remote.exception.ArtifactPushException
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
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
import java.util.concurrent.TimeUnit

/**
 * 集群数据同步类
 * 独立集群 同步到 独立集群 的同步实现类
 */
@Component
class ClusterReplicator(
    private val localDataManager: LocalDataManager,
    private val artifactReplicationHandler: ClusterArtifactReplicationHandler,
    private val replicationProperties: ReplicationProperties
) : Replicator {

    @Value("\${spring.application.version:$DEFAULT_VERSION}")
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
                val node = try {
                    localDataManager.findNodeDetailInVersion(
                        projectId = localProjectId,
                        repoName = localRepoName,
                        fullPath = it
                    )
                } catch (e: NodeNotFoundException) {
                    logger.warn("Node $it not found in repo $localProjectId|$localRepoName")
                    throw e
                }
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
                createdBy = packageVersion.createdBy,
                operator = SYSTEM_USER
            )
            artifactReplicaClient!!.replicaPackageVersionCreatedRequest(request)
        }
        return true
    }

    override fun replicaFile(context: ReplicaContext, node: NodeInfo): Boolean {
        with(context) {
            var type: String = replicationProperties.pushType
            retry(times = RETRY_COUNT, delayInSeconds = DELAY_IN_SECONDS) { retry ->
                return buildNodeCreateRequest(this, node)?.let {
                    if (blobReplicaClient!!.check(it.sha256!!, remoteRepo?.storageCredentials?.key).data != true
                    ) {
                        // 1. 同步文件数据
                        logger.info(
                            "The file [${node.fullPath}] with sha256 [${node.sha256}] " +
                                "will be pushed to the remote server ${cluster.name}, try the $retry time!"
                        )
                        try {
                            artifactReplicationHandler.blobPush(
                                filePushContext = FilePushContext(
                                    context = context,
                                    name = it.fullPath,
                                    size = it.size,
                                    sha256 = it.sha256
                                ),
                                pushType = type
                            )
                        } catch (throwable: Throwable) {
                            logger.warn(
                                "File replica push error $throwable, trace is " +
                                    "${Throwables.getStackTraceAsString(throwable)}!"
                            )
                            // 当不支持分块上传时，降级为普通上传
                            // 兼容接口不存在时，会返回401
                            if (
                                throwable is ArtifactPushException &&
                                (throwable.code == HttpStatus.METHOD_NOT_ALLOWED.value ||
                                    throwable.code == HttpStatus.UNAUTHORIZED.value )
                            ) {
                                type = PUSH_WITH_DEFAULT
                            }
                            throw throwable
                        }
                    }
                    // 再次确认下文件是否已经可见(cfs可见性问题)
                    doubleCheck(context, it.sha256!!)
                    logger.info(
                        "The node [${node.fullPath}] will be pushed to the remote server!"
                    )
                    // 2. 同步节点信息
                    artifactReplicaClient!!.replicaNodeCreateRequest(it)
                    true
                } ?: false
            }
        }
    }

    private fun doubleCheck(context: ReplicaContext, sha256: String) {
        var count = 0
        while (count < FILE_EXIST_CHECK_RETRY_COUNT) {
            if (context.blobReplicaClient!!.check(sha256, context.remoteRepo?.storageCredentials?.key).data == true){
                break
            } else {
                TimeUnit.SECONDS.sleep(1)
                count++
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
                operator = SYSTEM_USER,
                createdBy = node.createdBy,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = node.lastModifiedBy,
                lastModifiedDate = LocalDateTime.now()
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterReplicator::class.java)
        private const val FILE_EXIST_CHECK_RETRY_COUNT = 10

        fun buildRemoteRepoCacheKey(clusterInfo: ClusterInfo, projectId: String, repoName: String): String {
            return "$projectId/$repoName/${clusterInfo.hashCode()}"
        }
    }
}
