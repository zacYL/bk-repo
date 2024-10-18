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

package com.tencent.bkrepo.replication.replica.event

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordInfo
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskDetail
import com.tencent.bkrepo.replication.replica.base.context.ReplicaExecutionContext
import com.tencent.bkrepo.replication.replica.base.executor.AbstractReplicaJobExecutor
import com.tencent.bkrepo.replication.service.ClusterNodeService
import com.tencent.bkrepo.replication.service.ReplicaRecordService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 基于事件消息的实时同步逻辑实现类
 */
@Suppress("TooGenericExceptionCaught")
@Component
class EventBasedReplicaJobExecutor(
    clusterNodeService: ClusterNodeService,
    localDataManager: LocalDataManager,
    replicaService: EventBasedReplicaService,
    private val replicaRecordService: ReplicaRecordService
) : AbstractReplicaJobExecutor(clusterNodeService, localDataManager, replicaService) {

    /**
     * 执行同步
     */
    fun execute(taskDetail: ReplicaTaskDetail, event: ArtifactEvent) {
        sendReplicaNotify(taskDetail, START)
        val task = taskDetail.task
        var status = ExecutionStatus.SUCCESS
        val taskRecord: ReplicaRecordInfo = replicaRecordService.findOrCreateLatestRecord(task.key)
        val fullResourceKey = when(event.type) {
                EventType.NODE_COPIED,
                EventType.NODE_MOVED -> {
                    val dstProjectId = event.data["dstProjectId"].toString()
                    val dstRepoName = event.data["dstRepoName"].toString()
                    val dstParentPath = event.data["dstFullPath"].toString()
                    val name = PathUtils.resolveName(event.resourceKey)
                    "$dstProjectId/$dstRepoName/$dstParentPath/$name"
                }
                else -> event.getFullResourceKey()
            }
        try {
            // 待同步的制品数量
            val count = 1L * task.remoteClusters.size
            // 初始化或更新同步进度缓存
            ReplicaExecutionContext.getArtifactCount(task.key)?.run {
                ReplicaExecutionContext.increaseArtifactCount(task.key, count)
            } ?: ReplicaExecutionContext.initProgress(task.key, count)
            val result = task.remoteClusters.map { submit(taskDetail, taskRecord, it, event) }.map { it.get() }
            val failedResults = result.filter { it.status == ExecutionStatus.FAILED }
            if (failedResults.isNotEmpty()) {
                status = ExecutionStatus.FAILED
            }
            logger.info("Replica $fullResourceKey completed.")
        } catch (exception: Exception) {
            status = ExecutionStatus.FAILED
            logger.error("Replica $fullResourceKey failed: $exception", exception)
        } finally {
            sendReplicaNotify(taskDetail, status.name)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EventBasedReplicaJobExecutor::class.java)
    }
}
