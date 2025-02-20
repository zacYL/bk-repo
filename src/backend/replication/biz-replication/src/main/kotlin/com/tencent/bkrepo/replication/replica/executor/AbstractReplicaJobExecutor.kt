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

package com.tencent.bkrepo.replication.replica.executor

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.notify.pojo.enums.PlatformSmallBell
import com.tencent.bkrepo.common.notify.service.PlatformNotify
import com.tencent.bkrepo.common.service.otel.util.AsyncUtils.trace
import com.tencent.bkrepo.replication.config.ReplicationProperties
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeName
import com.tencent.bkrepo.replication.pojo.record.ExecutionResult
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.record.ReplicaOverview
import com.tencent.bkrepo.replication.pojo.record.ReplicaProgress
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordInfo
import com.tencent.bkrepo.replication.pojo.record.ResultsSummary
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskDetail
import com.tencent.bkrepo.replication.replica.type.ReplicaService
import com.tencent.bkrepo.replication.replica.context.ReplicaContext
import com.tencent.bkrepo.replication.service.ClusterNodeService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import org.springframework.beans.factory.annotation.Autowired

/**
 * 同步任务抽象实现类
 */
open class AbstractReplicaJobExecutor(
    private val clusterNodeService: ClusterNodeService,
    private val localDataManager: LocalDataManager,
    private val replicaService: ReplicaService,
    private val replicationProperties: ReplicationProperties
) {

    private val threadPoolExecutor: ThreadPoolExecutor = ReplicaThreadPoolExecutor.instance
    
    @Autowired
    lateinit var devopsConf: DevopsConf
    @Autowired
    lateinit var platformNotify: PlatformNotify

    /**
     * 提交任务到线程池执行
     * @param taskDetail 任务详情
     * @param taskRecord 执行记录
     * @param clusterNodeName 远程集群
     * @param event 事件
     */
    protected fun submit(
        taskDetail: ReplicaTaskDetail,
        taskRecord: ReplicaRecordInfo,
        clusterNodeName: ClusterNodeName,
        event: ArtifactEvent? = null
    ): Future<ExecutionResult> {
        return threadPoolExecutor.submit<ExecutionResult>(
            Callable {
                var replicaProgress = ReplicaProgress()
                try {
                    val clusterNode = clusterNodeService.getByClusterId(clusterNodeName.id)
                    require(clusterNode != null) { "Cluster[${clusterNodeName.id}] does not exist." }
                    var status = ExecutionStatus.SUCCESS
                    var message: String? = null
                    taskDetail.objects.map { taskObject ->
                        val localRepo = localDataManager.findRepoByName(
                            taskDetail.task.projectId,
                            taskObject.localRepoName,
                            taskObject.repoType.toString()
                        )
                        val context = ReplicaContext(
                            taskDetail = taskDetail,
                            taskObject = taskObject,
                            taskRecord = taskRecord,
                            localRepo = localRepo,
                            remoteCluster = clusterNode,
                            replicationProperties = replicationProperties
                        )
                        event?.let { context.event = it }
                        replicaService.replica(context)
                        replicaProgress = replicaProgress.plus(context.replicaProgress)
                        if (context.status == ExecutionStatus.FAILED) {
                            status = context.status
                            message = context.errorMessage
                        }
                    }
                    ExecutionResult(status = status, errorReason = message, progress = replicaProgress)
                } catch (exception: Throwable) {
                    logger.error("${taskDetail.task.name}/$clusterNodeName] replica exception:${exception}")
                    ExecutionResult.fail("${clusterNodeName.name}:${exception.message}\n", replicaProgress)
                }
            }.trace()
        )
    }

    /**
     * 以Task维度，汇总线程执行结果
     */
    protected fun getResultsSummary(results: List<ExecutionResult>): ResultsSummary {
        val replicaOverview = ReplicaOverview()
        var status = ExecutionStatus.SUCCESS
        var errorReason = ""
        results.forEach { result ->
            if (result.status == ExecutionStatus.FAILED) {
                status = ExecutionStatus.FAILED
                errorReason = "部分数据同步失败 "
                errorReason += result.errorReason ?: ""
            }
            result.progress?.let { progress ->
                replicaOverview.success += (progress.success + progress.skip)
                replicaOverview.failed += progress.failed
                replicaOverview.conflict += progress.conflict
            }
        }
        return ResultsSummary(replicaOverview, errorReason, status)
    }
    
    
    /**
     * 发送制品分发开始、完成、失败的消息通知
     */
    protected fun sendReplicaNotify(taskDetail: ReplicaTaskDetail?, status: String) {
        if (taskDetail == null) {
            logger.info("replica task not found, skip notify...")
            return
        }
        threadPoolExecutor.submit {
            with(taskDetail) {
                val templateCode = when (status) {
                    SUCCESS -> PlatformSmallBell.ARTIFACT_REPLICA_FINISH_TEMPLATE
                    FAILED -> PlatformSmallBell.ARTIFACT_REPLICA_FAIL_TEMPLATE
                    START -> PlatformSmallBell.ARTIFACT_REPLICA_START_TEMPLATE
                    else -> return@submit
                }
                val receivers = setOf(task.createdBy, task.lastModifiedBy)
                val bodyParams = mapOf(
                    "name" to task.name,
                    "replicaType" to task.replicaObjectType.name,
                    "remoteClusters" to task.remoteClusters.joinToString(",") { it.name },
                    "localRepo" to objects.map { it.localRepoName }.distinct().joinToString (","),
                    "time" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "url" to "${devopsConf.devopsHost}/console/repository/${task.projectId}/planManage"
                )
                platformNotify.sendPlatformNotify(task.projectId, templateCode, receivers, bodyParams)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractReplicaJobExecutor::class.java)
        const val SUCCESS = "SUCCESS"
        const val FAILED = "FAILED"
        const val START = "START"
    }
}
