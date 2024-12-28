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

package com.tencent.bkrepo.cocoapods.event.consumer

import com.tencent.bkrepo.cocoapods.constant.LOCK_PREFIX
import com.tencent.bkrepo.cocoapods.pool.EventHandlerThreadPoolExecutor
import com.tencent.bkrepo.cocoapods.service.CocoapodsReplicaService
import com.tencent.bkrepo.cocoapods.service.CocoapodsSpecsService
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.repo.RepoCreatedEvent
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.lock.service.LockOperation
import com.tencent.bkrepo.replication.pojo.record.ExecutionResult
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

@Component
class RemoteEventJobExecutor(
    private val lockOperation: LockOperation,
    private val cocoapodsSpecsService: CocoapodsSpecsService,
    private val repositoryClient: RepositoryClient,
    private val cocoapodsReplicaService: CocoapodsReplicaService,
) {
    private val threadPoolExecutor: ThreadPoolExecutor = EventHandlerThreadPoolExecutor.instance

    fun execute(event: ArtifactEvent) {
        try {
            logger.info("Will start to handle event $event")
            with(event) {
                val action: () -> Unit = when (type) {
                    EventType.REPO_CREATED -> {
                        {
                            val lockKey = "$LOCK_PREFIX:init_specs:$projectId:$repoName"
                            lockAction(lockKey) {
                                repoCreateHandle(this)
                            }
                        }
                    }

                    EventType.COCOAPODS_REPLICA -> {
                        {
                            replicaHandle(this)
                        }
                    }

                    else -> {
                        return
                    }
                }
                submit(action)
                logger.info("Helm Remote event ${getFullResourceKey()} completed.")
            }
        } catch (exception: Exception) {
            logger.warn("Helm Remote event ${event.getFullResourceKey()}} failed: $exception")
        }
    }


    /**
     * 提交任务到线程池执行
     * @param action 执行函数
     */
    protected fun submit(
        action: () -> Unit,
    ): Future<ExecutionResult> {
        return threadPoolExecutor.submit<ExecutionResult> {
            try {
                val status = ExecutionStatus.SUCCESS
                action()
                ExecutionResult(status)
            } catch (exception: Throwable) {
                logger.warn("Error occurred while executing the task: $exception")
                ExecutionResult.fail(exception.message)
            }
        }
    }

    fun <T> lockAction(lockKey: String, action: () -> T): T {
        val lock = lockOperation.getLock(lockKey)
        return if (lockOperation.getSpinLock(lockKey, lock)) {
            LockOperation.logger.info("Lock for key $lockKey has been acquired.")
            try {
                action()
            } finally {
                lockOperation.close(lockKey, lock)
            }
        } else {
            action()
        }
    }

    private fun replicaHandle(event: ArtifactEvent) {
        cocoapodsReplicaService.resolveIndexFile(event)
    }

    private fun repoCreateHandle(event: ArtifactEvent) {
        logger.info("repo create event: $event")
        with(event) {
            require(data[RepoCreatedEvent::repoType.name] == RepositoryType.COCOAPODS.name) { return }
            repositoryClient.getRepoInfo(projectId, repoName).data?.let { repoInfo ->
                when (repoInfo.category) {
                    RepositoryCategory.LOCAL -> {
                        cocoapodsSpecsService.initSpecs(projectId, repoName)
                    }

                    RepositoryCategory.REMOTE -> {
                        cocoapodsSpecsService.initRemoteSpecs(projectId, repoInfo)
                    }

                    else -> TODO()
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteEventJobExecutor::class.java)
    }
}
