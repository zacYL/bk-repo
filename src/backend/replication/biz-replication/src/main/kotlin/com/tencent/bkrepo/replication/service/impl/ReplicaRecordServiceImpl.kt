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

package com.tencent.bkrepo.replication.service.impl

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.TimeUtils
import com.tencent.bkrepo.common.artifact.cluster.ClusterProperties
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.replication.dao.ReplicaRecordDao
import com.tencent.bkrepo.replication.dao.ReplicaRecordDetailDao
import com.tencent.bkrepo.replication.dao.ReplicaTaskDao
import com.tencent.bkrepo.replication.exception.ReplicationMessageCode
import com.tencent.bkrepo.replication.model.TReplicaRecord
import com.tencent.bkrepo.replication.model.TReplicaRecordDetail
import com.tencent.bkrepo.replication.model.TReplicaTask
import com.tencent.bkrepo.replication.pojo.record.ExecutionResult
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus.FAILED
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus.RUNNING
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus.SUCCESS
import com.tencent.bkrepo.replication.pojo.record.RecordOverview
import com.tencent.bkrepo.replication.pojo.record.ReplicaProgress
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordDetail
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordDetailListOption
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordInfo
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordListOption
import com.tencent.bkrepo.replication.pojo.record.ReplicaTaskRecordInfo
import com.tencent.bkrepo.replication.pojo.record.request.RecordDetailInitialRequest
import com.tencent.bkrepo.replication.pojo.record.request.RecordDetailSearchRequest
import com.tencent.bkrepo.replication.pojo.request.ReplicaType
import com.tencent.bkrepo.replication.pojo.task.ReplicaStatus
import com.tencent.bkrepo.replication.service.ReplicaRecordService
import com.tencent.bkrepo.replication.util.CronUtils
import com.tencent.bkrepo.replication.util.TaskRecordQueryHelper
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ReplicaRecordServiceImpl(
    private val replicaRecordDao: ReplicaRecordDao,
    private val replicaRecordDetailDao: ReplicaRecordDetailDao,
    private val replicaTaskDao: ReplicaTaskDao,
    private val clusterProperties: ClusterProperties
) : ReplicaRecordService {

    override fun startNewRecord(key: String): ReplicaRecordInfo {
        val initialRecord = initialRecord(key)
        val tReplicaTask = replicaTaskDao.findByKey(key)
            ?: throw ErrorCodeException(ReplicationMessageCode.REPLICA_TASK_NOT_FOUND, key)
        tReplicaTask.status = ReplicaStatus.REPLICATING
        tReplicaTask.lastExecutionTime = LocalDateTime.now()
        if (isCronJob(tReplicaTask)) {
            tReplicaTask.nextExecutionTime =
                CronUtils.getNextTriggerTime(tReplicaTask.setting.executionPlan.cronExpression.orEmpty())
        }
        tReplicaTask.lastExecutionStatus = RUNNING
        replicaTaskDao.save(tReplicaTask)
        return initialRecord
    }

    override fun initialRecord(taskKey: String): ReplicaRecordInfo {
        val record = TReplicaRecord(
            taskKey = taskKey,
            status = RUNNING,
            startTime = LocalDateTime.now()
        )
        return try {
            replicaRecordDao.insert(record).let { convert(it)!! }
        } catch (exception: DuplicateKeyException) {
            logger.warn("init record [$taskKey] error: [${exception.message}]")
            throw exception
        }
    }

    override fun completeRecord(recordId: String, status: ExecutionStatus, errorReason: String?) {
        val replicaRecordInfo = getRecordById(recordId)
            ?: throw ErrorCodeException(ReplicationMessageCode.REPLICA_TASK_NOT_FOUND, recordId)
        val record = with(replicaRecordInfo) {
            TReplicaRecord(
                id = id,
                taskKey = taskKey,
                status = status,
                startTime = startTime,
                endTime = LocalDateTime.now(),
                errorReason = errorReason
            )
        }
        val tReplicaTask = replicaTaskDao.findByKey(record.taskKey)
            ?: throw ErrorCodeException(ReplicationMessageCode.REPLICA_TASK_NOT_FOUND, record.taskKey)
        tReplicaTask.lastExecutionStatus = status
        tReplicaTask.status = if (isCronJob(tReplicaTask)) ReplicaStatus.WAITING else ReplicaStatus.COMPLETED
        replicaRecordDao.save(record)
        replicaTaskDao.save(tReplicaTask)
        logger.info("complete record [$recordId], status from [${replicaRecordInfo.status}] to [$status].")
    }

    override fun initialRecordDetail(request: RecordDetailInitialRequest): ReplicaRecordDetail {
        with(request) {
            val recordDetail = TReplicaRecordDetail(
                recordId = recordId,
                localCluster = clusterProperties.center.name.orEmpty(),
                remoteCluster = remoteCluster,
                localRepoName = localRepoName,
                repoType = repoType,
                packageConstraint = packageConstraint,
                pathConstraint = pathConstraint,
                artifactName = artifactName,
                version = version,
                conflictStrategy = conflictStrategy,
                status = RUNNING,
                progress = ReplicaProgress(),
                startTime = LocalDateTime.now()
            )
            return try {
                replicaRecordDetailDao.insert(recordDetail).let { convert(it)!! }
            } catch (exception: DuplicateKeyException) {
                logger.warn("init record detail [$recordId] error: [${exception.message}]")
                throw exception
            }
        }
    }

    override fun updateRecordDetailProgress(detailId: String, progress: ReplicaProgress) {
        val replicaRecordDetail = findRecordDetailById(detailId)
            ?: throw ErrorCodeException(ReplicationMessageCode.REPLICA_TASK_NOT_FOUND, detailId)
        replicaRecordDetail.progress.success = progress.success
        replicaRecordDetail.progress.skip = progress.skip
        replicaRecordDetail.progress.failed = progress.failed
        replicaRecordDetail.progress.totalSize = progress.totalSize
        replicaRecordDetailDao.save(replicaRecordDetail)
        logger.info("Update record detail [$detailId] success.")
    }

    override fun completeRecordDetail(detailId: String, result: ExecutionResult) {
        val replicaRecordDetail = getRecordDetailById(detailId)
            ?: throw ErrorCodeException(ReplicationMessageCode.REPLICA_TASK_NOT_FOUND, detailId)
        val recordDetail = with(replicaRecordDetail) {
            TReplicaRecordDetail(
                id = detailId,
                recordId = recordId,
                localCluster = localCluster,
                remoteCluster = remoteCluster,
                localRepoName = localRepoName,
                repoType = repoType,
                packageConstraint = packageConstraint,
                pathConstraint = pathConstraint,
                artifactName = artifactName,
                version = version,
                conflictStrategy = conflictStrategy,
                status = result.status,
                progress = result.progress!!,
                startTime = startTime,
                endTime = LocalDateTime.now(),
                errorReason = result.errorReason
            )
        }
        replicaRecordDetailDao.save(recordDetail)
    }

    override fun listRecordsByTaskKey(key: String): List<ReplicaRecordInfo> {
        return replicaRecordDao.listByTaskKey(key).map { convert(it)!! }
    }

    override fun listRecordsPage(key: String, option: ReplicaRecordListOption): Page<ReplicaRecordInfo> {
        val pageNumber = option.pageNumber
        val pageSize = option.pageSize
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val query = TaskRecordQueryHelper.recordListQuery(key, option)
        val totalRecords = replicaRecordDao.count(query)
        val records = replicaRecordDao.find(query.with(pageRequest)).map { convert(it)!! }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun deleteRecord(key: String, startTime: String, endTime: String): Long {
        var deleteCount = 0L
        val localStart = TimeUtils.toSystemZoneTime(startTime)
        val localEnd = TimeUtils.toSystemZoneTime(endTime)
        require(localStart.isBefore(localEnd)) {
            throw IllegalArgumentException("startTime must before endTime")
        }
        val criteria = Criteria.where(TReplicaRecord::taskKey.name).`is`(key)
            .and(TReplicaRecord::startTime.name).gte(localStart)
            .and(TReplicaRecord::endTime.name).lte(localEnd)
        replicaRecordDao.find(Query(criteria)).forEach {
            replicaRecordDetailDao.deleteByRecordId(it.id!!)
            replicaRecordDao.removeById(it.id!!)
            deleteCount++
        }
        return deleteCount
    }

    override fun listDetailsByRecordId(recordId: String): List<ReplicaRecordDetail> {
        return replicaRecordDetailDao.listByRecordId(recordId).map { convert(it)!! }
    }

    override fun getRecordAndTaskInfoByRecordId(recordId: String): ReplicaTaskRecordInfo {
        val replicaRecordInfo = getRecordById(recordId)
            ?: throw ErrorCodeException(ReplicationMessageCode.REPLICA_TASK_NOT_FOUND, recordId)
        val taskKey = replicaRecordInfo.taskKey
        val replicaTask = replicaTaskDao.findByKey(taskKey)
            ?: throw ErrorCodeException(ReplicationMessageCode.REPLICA_TASK_NOT_FOUND, taskKey)
        return ReplicaTaskRecordInfo(replicaTask.replicaObjectType, replicaTask.replicaType, replicaRecordInfo)
    }

    override fun getRecordById(id: String): ReplicaRecordInfo? {
        return convert(replicaRecordDao.findById(id))
    }

    override fun getRecordDetailById(id: String): ReplicaRecordDetail? {
        return convert(findRecordDetailById(id))
    }

    override fun deleteRecordDetailById(id: String) {
        replicaRecordDao.removeById(id)
    }

    private fun findRecordDetailById(id: String): TReplicaRecordDetail? {
        return replicaRecordDetailDao.findById(id)
    }

    override fun deleteByTaskKey(key: String) {
        replicaRecordDao.listByTaskKey(key).forEach {
            replicaRecordDetailDao.deleteByRecordId(it.id!!)
        }
        replicaRecordDao.deleteByTaskKey(key)
    }

    override fun listRecordDetailPage(
        recordId: String,
        option: ReplicaRecordDetailListOption
    ): Page<ReplicaRecordDetail> {
        val pageNumber = option.pageNumber
        val pageSize = option.pageSize
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val query = TaskRecordQueryHelper.recordDetailListQuery(recordId, option)
        val totalRecords = replicaRecordDetailDao.count(query)
        val records = replicaRecordDetailDao.find(query.with(pageRequest)).map { convert(it)!! }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun findOrCreateLatestRecord(key: String): ReplicaRecordInfo {
        val replicaRecordList = replicaRecordDao.listByTaskKey(key).map { convert(it) }
        if (replicaRecordList.isNotEmpty()) return replicaRecordList.first()!!
        return initialRecord(key)
    }

    override fun recordDetailOverview(recordId: String): RecordOverview {
        var success = 0L
        var fail = 0L
        val aggregation = Aggregation.newAggregation(
            TReplicaRecordDetail::class.java,
            Aggregation.match(Criteria.where(TReplicaRecordDetail::recordId.name).`is`(recordId)),
            Aggregation.group("\$${TReplicaRecordDetail::status.name}").count().`as`("count")
        )
        val result = replicaRecordDetailDao.aggregate(aggregation, Overview::class.java).mappedResults

        result.forEach {
            when (it._id) {
                SUCCESS -> success = it.count.toLong()
                FAILED -> fail = it.count.toLong()
                RUNNING -> TODO()
            }
        }
        val conflict = replicaRecordDetailDao.find(
            Query.query(
                Criteria.where(TReplicaRecordDetail::recordId.name).`is`(recordId)
                    .and(TReplicaRecordDetail::conflictStrategy.name).ne(null)
            )
        ).count().toLong()
        return RecordOverview(success + fail, success, fail, conflict)
    }

    override fun searchRecordDetail(searchRequest: RecordDetailSearchRequest): Map<ExecutionStatus, Long> {
        val resultMap = mutableMapOf(SUCCESS to 0L, FAILED to 0L)
        var pageNumber = DEFAULT_PAGE_NUMBER
        var records = listRecordsPage(
            searchRequest.taskKey,
            ReplicaRecordListOption(pageNumber, DEFAULT_PAGE_SIZE * 5)
        ).records
        while (records.isNotEmpty()) {
            records.forEach {
                val criteria = Criteria.where(TReplicaRecordDetail::recordId.name).`is`(it.id)
                    .and(TReplicaRecordDetail::artifactName.name).`is`(searchRequest.artifactName)
                searchRequest.version?.let {
                    criteria.and(TReplicaRecordDetail::version.name).`is`(searchRequest.version)
                }
                val aggregation = Aggregation.newAggregation(
                    TReplicaRecordDetail::class.java,
                    Aggregation.match(criteria),
                    Aggregation.group("\$${TReplicaRecordDetail::status.name}").count().`as`("count")
                )
                replicaRecordDetailDao.aggregate(
                    aggregation,
                    Overview::class.java
                ).mappedResults.forEach { overview ->
                    when (overview._id) {
                        SUCCESS -> resultMap[SUCCESS] = resultMap[SUCCESS]!!.plus(overview.count.toLong())
                        FAILED -> resultMap[FAILED] = resultMap[FAILED]!!.plus(overview.count.toLong())
                        RUNNING -> TODO()
                    }
                }
            }
            pageNumber++
            records = listRecordsPage(
                searchRequest.taskKey,
                ReplicaRecordListOption(pageNumber, DEFAULT_PAGE_SIZE * 5)
            ).records
        }
        return resultMap
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReplicaRecordServiceImpl::class.java)

        private fun isCronJob(tReplicaTask: TReplicaTask): Boolean {
            return !tReplicaTask.setting.executionPlan.cronExpression.isNullOrBlank() &&
                    tReplicaTask.replicaType == ReplicaType.SCHEDULED
        }

        private fun convert(tReplicaRecord: TReplicaRecord?): ReplicaRecordInfo? {
            return tReplicaRecord?.let {
                ReplicaRecordInfo(
                    id = it.id!!,
                    taskKey = it.taskKey,
                    status = it.status,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    errorReason = it.errorReason
                )
            }
        }

        private fun convert(tReplicaRecordDetail: TReplicaRecordDetail?): ReplicaRecordDetail? {
            return tReplicaRecordDetail?.let {
                ReplicaRecordDetail(
                    id = it.id.orEmpty(),
                    recordId = it.recordId,
                    localCluster = it.localCluster,
                    remoteCluster = it.remoteCluster,
                    localRepoName = it.localRepoName,
                    repoType = it.repoType,
                    packageConstraint = it.packageConstraint,
                    pathConstraint = it.pathConstraint,
                    artifactName = it.artifactName,
                    version = it.version,
                    conflictStrategy = it.conflictStrategy,
                    status = it.status,
                    progress = it.progress,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    errorReason = it.errorReason
                )
            }
        }
    }
}

data class Overview(
    val _id: ExecutionStatus,
    val count: Double
)
