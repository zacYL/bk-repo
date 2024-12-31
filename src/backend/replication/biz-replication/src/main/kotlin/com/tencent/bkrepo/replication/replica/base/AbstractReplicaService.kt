/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.replication.replica.base

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.stream.event.supplier.EventSupplier
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.pojo.record.ExecutionResult
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.record.request.RecordDetailInitialRequest
import com.tencent.bkrepo.replication.pojo.request.ActionType
import com.tencent.bkrepo.replication.pojo.request.PackageVersionExistCheckRequest
import com.tencent.bkrepo.replication.pojo.request.ReplicaObjectType
import com.tencent.bkrepo.replication.pojo.task.objects.PackageConstraint
import com.tencent.bkrepo.replication.pojo.task.objects.PathConstraint
import com.tencent.bkrepo.replication.pojo.task.setting.ConflictStrategy
import com.tencent.bkrepo.replication.pojo.task.setting.ConflictStrategy.FAST_FAIL
import com.tencent.bkrepo.replication.pojo.task.setting.ConflictStrategy.SKIP
import com.tencent.bkrepo.replication.pojo.task.setting.ErrorStrategy
import com.tencent.bkrepo.replication.replica.base.context.ReplicaContext
import com.tencent.bkrepo.replication.replica.base.context.ReplicaExecutionContext
import com.tencent.bkrepo.replication.replica.event.ReplicaEvent
import com.tencent.bkrepo.replication.service.ReplicaRecordService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.UserAuthPathOption
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * 同步服务抽象类
 * 一次replica执行负责一个任务下的一个集群，在子线程中执行
 */
@Suppress("TooGenericExceptionCaught")
abstract class AbstractReplicaService(
    private val replicaRecordService: ReplicaRecordService,
    private val localDataManager: LocalDataManager,
    private val permissionManager: PermissionManager,
) : ReplicaService {

    @Autowired
    private lateinit var eventSupplier: EventSupplier

    private val logger = LoggerFactory.getLogger(AbstractReplicaService::class.java)

    /**
     * 同步项目和仓库
     */
    protected fun replicaProjectAndRepo(context: ReplicaContext) {
        with(context) {
            try {
                replicator.replicaProject(this)
                replicator.replicaRepo(this)
            } catch (e: ErrorCodeException) {
                status = ExecutionStatus.FAILED
                val request = RecordDetailInitialRequest(
                    recordId = taskRecord.id,
                    remoteCluster = remoteCluster.name,
                    localRepoName = localRepoName,
                    repoType = localRepoType,
                    artifactName = "/",
                    status = ExecutionStatus.FAILED,
                    errorReason = when (e.status) {
                        HttpStatus.NOT_FOUND -> "project[$remoteProjectId] not found"
                        HttpStatus.CONFLICT -> "repo[$remoteProjectId/$remoteRepoName] conflict"
                        else -> null
                    }
                )
                logger.warn("Fail to create project or repo: ${request.errorReason}")
                replicaRecordService.initialRecordDetail(request)
                throw e
            }
        }
    }

    /**
     * 同步整个仓库数据
     */
    protected fun replicaByRepo(replicaContext: ReplicaContext) {
        if (replicaContext.taskObject.repoType == RepositoryType.GENERIC) {
            // 同步generic节点
            val root = localDataManager.findNodeDetail(
                projectId = replicaContext.localProjectId,
                repoName = replicaContext.localRepoName,
                fullPath = PathUtils.ROOT
            ).nodeInfo
            replicaByPath(replicaContext, root)
            return
        }
        // 同步包
        val option = PackageListOption(pageNumber = 1, pageSize = PAGE_SIZE)
        var packages = localDataManager.listPackagePage(
            projectId = replicaContext.localProjectId,
            repoName = replicaContext.localRepoName,
            option = option
        )
        while (packages.isNotEmpty()) {
            packages.forEach {
                replicaByPackage(replicaContext, it)
            }
            option.pageNumber += 1
            packages = localDataManager.listPackagePage(
                projectId = replicaContext.localProjectId,
                repoName = replicaContext.localRepoName,
                option = option
            )
        }
    }

    /**
     * 同步指定包的数据
     */
    protected fun replicaByPackageConstraint(replicaContext: ReplicaContext, constraint: PackageConstraint) {
        // 查询本地包信息
        val packageSummary = localDataManager.findPackageByKey(
            projectId = replicaContext.localProjectId,
            repoName = replicaContext.taskObject.localRepoName,
            packageKey = constraint.packageKey!!
        )
        replicaByPackage(replicaContext, packageSummary, constraint.versions)
    }

    /**
     * 同步指定路径的数据
     */
    protected fun replicaByPathConstraint(replicaContext: ReplicaContext, constraint: PathConstraint) {
        val nodeInfo = localDataManager.findNodeDetail(
            projectId = replicaContext.localProjectId,
            repoName = replicaContext.localRepoName,
            fullPath = constraint.path!!
        ).nodeInfo
        replicaByPath(replicaContext, nodeInfo)
    }

    /**
     * 同步删除节点
     */
    protected fun deleteByPathConstraint(replicaContext: ReplicaContext, constraint: PathConstraint) {
        val replicaExecutionContext = initialExecutionContext(
            context = replicaContext,
            artifactName = constraint.path!!,
            actionType = ActionType.DELETE
        )
        with(replicaExecutionContext) {
            try {
                replicaContext.replicator.deleteNode(replicaContext, constraint.path!!)
            } catch (throwable: Throwable) {
                setErrorStatus(this, throwable)
            } finally {
                updateTaskProgressCache(replicaExecutionContext.taskKey)
                completeRecordDetail(replicaExecutionContext)
            }
        }
    }

    /**
     * 同步路径
     * 采用广度优先遍历
     */
    private fun replicaByPath(replicaContext: ReplicaContext, node: NodeInfo) {
        with(replicaContext) {

            val userAuthPath = permissionManager.getUserAuthPathCache(
                UserAuthPathOption(
                    replicaContext.task.createdBy,
                    node.projectId,
                    listOf(node.repoName),
                    PermissionAction.READ
                )
            )[node.repoName]

            check(!userAuthPath.isNullOrEmpty()) { "node fullPath[${node.fullPath}] no read permission" }

            check(userAuthPath.any() { node.fullPath.startsWith(PathUtils.toFullPath(it)) }) { "node fullPath[${node.fullPath}] no read permission" }

            if (!node.folder) {
                // 外部集群仓库没有project/repoName
                if (remoteProjectId.isNullOrBlank() || remoteRepoName.isNullOrBlank()) {
                    logger.warn("remoteProjectId or remoteRepoName is empty, replica end")
                    return
                }
                // 存在冲突：记录冲突策略
                val conflictStrategy = if (
                    artifactReplicaClient!!.checkNodeExist(remoteProjectId, remoteRepoName, node.fullPath).data == true
                ) {
                    task.setting.conflictStrategy
                } else null
                // 初始化分发记录详情 & 记录 artifactName
                val replicaExecutionContext = initialExecutionContext(
                    context = replicaContext,
                    artifactName = node.fullPath,
                    conflictStrategy = conflictStrategy,
                    actionType = getActionType(this)
                )
                replicaFile(replicaExecutionContext, node)
                return
            }
            // 查询子节点
            localDataManager.listNode(
                projectId = localProjectId,
                repoName = localRepoName,
                fullPath = node.fullPath
            ).forEach {
                replicaByPath(this, it)
            }
        }
    }

    /**
     * 同步节点
     */
    private fun replicaFile(context: ReplicaExecutionContext, node: NodeInfo) {
        with(context) {
            try {
                val fullPath = "${node.projectId}/${node.repoName}${node.fullPath}"
                when (context.detail.conflictStrategy) {
                    SKIP -> return
                    FAST_FAIL -> throw IllegalArgumentException("File[$fullPath] conflict.")
                    else -> {
                        // not conflict or overwrite
                        replicaContext.replicator.replicaFile(replicaContext, node)
                    }
                }
                return
            } catch (throwable: Throwable) {
                setErrorStatus(this, throwable)
                if (replicaContext.task.setting.errorStrategy == ErrorStrategy.FAST_FAIL) {
                    throw throwable
                }
            } finally {
                updateTaskProgressCache(context.taskKey)
                // 记录每一个制品的同步记录详情，如果不记录，则删除初始化记录
                if (replicaContext.task.replicaObjectType == ReplicaObjectType.REPOSITORY
                    && replicaContext.task.notRecord
                ) {
                    replicaRecordService.deleteRecordDetailById(detail.id)
                } else {
                    completeRecordDetail(context)
                }
            }
        }
    }

    /**
     * 根据[packageSummary]和版本列表[versionNames]执行同步
     */
    private fun replicaByPackage(
        replicaContext: ReplicaContext,
        packageSummary: PackageSummary,
        versionNames: List<String>? = null
    ) {
        with(replicaContext) {
            replicator.replicaPackage(this, packageSummary)
            var versions = versionNames?.map {
                localDataManager.findPackageVersion(
                    projectId = localProjectId,
                    repoName = localRepoName,
                    packageKey = packageSummary.key,
                    version = it
                )
            } ?: localDataManager.listAllVersion(
                projectId = localProjectId,
                repoName = localRepoName,
                packageKey = packageSummary.key,
                option = VersionListOption()
            )
            // version 排序，保证分发后的顺序一致
            versions = versions.sortedBy { it.createdDate }
            versions.forEach {
                // 外部集群仓库没有project/repoName
                if (remoteProjectId.isNullOrBlank() || remoteRepoName.isNullOrBlank()) {
                    logger.warn("remoteProjectId or remoteRepoName is empty, replica end")
                    return
                }
                val checkRequest = PackageVersionExistCheckRequest(
                    projectId = remoteProjectId,
                    repoName = remoteRepoName,
                    packageKey = packageSummary.key,
                    versionName = it.name
                )
                // 存在冲突：记录冲突策略
                val conflictStrategy =
                    if (artifactReplicaClient!!.checkPackageVersionExist(checkRequest).data == true) {
                        task.setting.conflictStrategy
                    } else null
                val replicaExecutionContext = initialExecutionContext(
                    context = replicaContext,
                    artifactName = packageSummary.name,
                    version = it.name,
                    conflictStrategy = conflictStrategy
                )
                replicaPackageVersion(replicaExecutionContext, packageSummary, it)
            }
        }
    }

    /**
     * 同步版本
     */
    private fun replicaPackageVersion(
        context: ReplicaExecutionContext,
        packageSummary: PackageSummary,
        version: PackageVersion
    ) {
        with(context) {
            val fullPath = "${packageSummary.name}-${version.name}"
            try {
                when (context.detail.conflictStrategy) {
                    SKIP -> return
                    FAST_FAIL -> throw IllegalArgumentException("File[$fullPath] conflict.")
                    else -> {
                        // not conflict or overwrite
                        replicator.replicaPackageVersion(replicaContext, packageSummary, version)

                        //仅针对Cocoapods制品同步，发送消息给Cocoapods服务，通知处理包文件索引
                        if (packageSummary.type == PackageType.COCOAPODS) {
                            //构建消息对象，传入的参数是有关同步接收方节点的信息
                            val event = ReplicaEvent(
                                projectId = replicaContext.remoteProjectId!!,
                                repoName = replicaContext.remoteRepoName!!,
                                fullPath = version.contentPath!!,
                                clusterUrl = replicaContext.cluster.url.substring(0, replicaContext.cluster.url.indexOf("/replication")),
                                repoType = RepositoryType.COCOAPODS,
                                packageType = PackageType.COCOAPODS,
                                userId = SecurityUtils.getUserId()
                            )
                            logger.info("send cocoapods replica event[$event]")
                            eventSupplier.delegateToSupplier(
                                event = event,
                                topic = BINDING_OUT_NAME,
                                key = event.getFullResourceKey()
                            )
                        }
                    }
                }
                return
            } catch (throwable: Throwable) {
                setErrorStatus(this, throwable)
                if (replicaContext.task.setting.errorStrategy == ErrorStrategy.FAST_FAIL) {
                    throw throwable
                }
            } finally {
                updateTaskProgressCache(context.taskKey)
                // 记录每一个制品的同步记录详情，如果不记录，则删除初始化记录
                if (replicaContext.task.replicaObjectType == ReplicaObjectType.REPOSITORY
                    && replicaContext.task.notRecord
                ) {
                    replicaRecordService.deleteRecordDetailById(detail.id)
                } else {
                    completeRecordDetail(context)
                }
            }
        }
    }

    /**
     * 初始化执行过程context
     */
    private fun initialExecutionContext(
        context: ReplicaContext,
        packageConstraint: PackageConstraint? = null,
        pathConstraint: PathConstraint? = null,
        artifactName: String,
        version: String? = null,
        conflictStrategy: ConflictStrategy? = null,
        actionType: ActionType? = null
    ): ReplicaExecutionContext {
        // 创建详情
        val request = RecordDetailInitialRequest(
            recordId = context.taskRecord.id,
            remoteCluster = context.remoteCluster.name,
            localRepoName = context.localRepoName,
            repoType = context.localRepoType,
            packageConstraint = packageConstraint,
            pathConstraint = pathConstraint,
            artifactName = artifactName,
            version = version,
            conflictStrategy = conflictStrategy,
            actionType = actionType
        )
        val recordDetail = replicaRecordService.initialRecordDetail(request)
        return ReplicaExecutionContext(context, recordDetail)
    }

    /**
     * 设置状态为失败状态
     */
    private fun setErrorStatus(context: ReplicaExecutionContext, throwable: Throwable) {
        context.status = ExecutionStatus.FAILED
        context.appendErrorReason(throwable.message.orEmpty())
        context.replicaContext.status = ExecutionStatus.FAILED
    }

    /**
     * 持久化同步进度
     */
    private fun completeRecordDetail(context: ReplicaExecutionContext) {
        with(context) {
            val result = ExecutionResult(
                status = status,
                progress = progress,
                errorReason = buildErrorReason()
            )
            replicaRecordService.completeRecordDetail(detail.id, result)
        }
    }

    private fun updateTaskProgressCache(taskKey: String) {
        val currentProgress = ReplicaExecutionContext.increaseProgress(taskKey)
        val artifactCount = ReplicaExecutionContext.getArtifactCount(taskKey)
        if (currentProgress != null && artifactCount != null && currentProgress >= artifactCount) {
            ReplicaExecutionContext.removeProgress(taskKey)
        }
    }

    private fun getActionType(context: ReplicaContext): ActionType? {
        with(context) {
            return if (!eventExists()) null else {
                when (event.type) {
                    EventType.NODE_MOVED -> ActionType.MOVE
                    EventType.NODE_COPIED -> ActionType.COPY
                    else -> null
                }
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 1000
        private const val BINDING_OUT_NAME = "artifactEvent-out-0"
    }
}
