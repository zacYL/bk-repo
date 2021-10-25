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

package com.tencent.bkrepo.repository.service.log.impl

import com.tencent.bkrepo.auth.constant.BK_SOFTWARE
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.dao.OperateLogDao
import com.tencent.bkrepo.repository.model.TOperateLog
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricRequest
import com.tencent.bkrepo.repository.pojo.log.OperateLogResponse
import com.tencent.bkrepo.repository.pojo.metric.CountResult
import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.service.bksoftware.DayMetricService
import com.tencent.bkrepo.repository.service.log.OperateLogService
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * OperateLogService 实现类
 */
@Service
class OperateLogServiceImpl(
    private val operateLogDao: OperateLogDao,
    private val dayMetricService: DayMetricService
) : OperateLogService {

    @Async
    override fun saveEventAsync(event: ArtifactEvent, address: String) {
        val log = TOperateLog(
            type = event.type,
            resourceKey = event.resourceKey,
            projectId = event.projectId,
            repoName = event.repoName,
            description = event.data,
            userId = event.userId,
            clientAddress = address
        )
        val operateType = transferEventType(event.type)
        if (packageEvent.contains(event.type) && operateType != null) {
            dayMetricService.add(
                DayMetricRequest(
                day = LocalDate.now(),
                projectId = event.projectId,
                repoName = event.repoName,
                type = operateType
            )
            )
        }
        operateLogDao.save(log)
    }

    override fun page(
        type: ResourceType?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<OperateLogResponse> {
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val query = buildOperateLogPageQuery(type, projectId, repoName, operator, startTime, endTime)
        val totalRecords = operateLogDao.count(query)
        val records = operateLogDao.find(query.with(pageRequest)).map { convert(it) }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun uploads(projectId: String?, repoName: String?, lastWeek: Boolean?): Long {
        val criteria = Criteria.where(TOperateLog::type.name)
            .`in`(listOf(EventType.VERSION_CREATED, EventType.VERSION_UPDATED))
        projectId?.let { criteria.and(TOperateLog::projectId.name).`is`(projectId) }
        repoName?.let { criteria.and(TOperateLog::repoName.name).`is`(repoName) }
        if (lastWeek == true) {
            criteria.and(TOperateLog::createdDate.name).gte(getLatestWeekStart())
        }
        val aggregation = Aggregation.newAggregation(
            TOperateLog::class.java,
            Aggregation.match(criteria),
            Aggregation.group().count().`as`("count"),
            Aggregation.project("_id", "count")
        )
        val result = operateLogDao.aggregate(aggregation, CountResult::class.java).mappedResults
        return if (result.isEmpty()) 0 else result[0].count
    }

    override fun downloads(projectId: String?, repoName: String?, lastWeek: Boolean?): Long {
        val criteria = Criteria.where(TOperateLog::type.name)
            .`is`(EventType.VERSION_DOWNLOAD)
        projectId?.let { criteria.and(TOperateLog::projectId.name).`is`(projectId) }
        repoName?.let { criteria.and(TOperateLog::repoName.name).`is`(repoName) }
        if (lastWeek == true) {
            criteria.and(TOperateLog::createdDate.name).gte(getLatestWeekStart())
        }
        val aggregation = Aggregation.newAggregation(
            TOperateLog::class.java,
            Aggregation.match(criteria),
            Aggregation.group().count().`as`("count"),
            Aggregation.project("_id", "count")
        )
        val result = operateLogDao.aggregate(aggregation, CountResult::class.java).mappedResults
        return if (result.isEmpty()) 0 else result[0].count
    }

    private fun getLatestWeekStart(): LocalDateTime {
        val today = LocalDate.now()
        val week = today.dayOfWeek
        val weekStart = today.minusDays(week.value.toLong())
        return weekStart.atStartOfDay(ZoneId.of("UTC")).toLocalDateTime()
    }

    override fun uploadsByDay(projectId: String?, repoName: String?, days: Long?): Long {
        TODO("Not yet implemented")
    }

    private fun getEventList(resourceType: ResourceType): List<EventType> {
        return when (resourceType) {
            ResourceType.PROJECT -> projectEvent
            ResourceType.REPO -> repositoryEvent
            ResourceType.PACKAGE -> packageEvent
            ResourceType.ADMIN -> adminEvent
            ResourceType.METADATA -> metadataEvent
            else -> listOf()
        }
    }

    private fun convert(tOperateLog: TOperateLog): OperateLogResponse {
        val content = if (packageEvent.contains(tOperateLog.type)) {
            val packageName = tOperateLog.description["packageName"] as? String
            val version = tOperateLog.description["packageVersion"] as? String
            val repoType = tOperateLog.description["packageType"] as? String
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId,
                repoType = repoType,
                resKey = "${tOperateLog.repoName}::$packageName::$version"
            )
        } else if (repositoryEvent.contains(tOperateLog.type)) {
            val repoType = tOperateLog.description["repoType"] as? String
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId,
                repoType = repoType,
                resKey = tOperateLog.repoName!!
            )
        } else if (adminEvent.contains(tOperateLog.type)) {
            val list = tOperateLog.resourceKey.readJsonString<List<String>>()
            OperateLogResponse.Content(
                resKey = list.joinToString("::")
            )
        } else {
            OperateLogResponse.Content(resKey = "")
        }

        return OperateLogResponse(
            createdDate = tOperateLog.createdDate,
            operate = tOperateLog.type.nick,
            userId = tOperateLog.userId,
            clientAddress = tOperateLog.clientAddress,
            result = true,
            content = content
        )
    }

    private fun buildOperateLogPageQuery(
        type: ResourceType?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?
    ): Query {
        val criteria = if (type != null) {
            Criteria.where(TOperateLog::type.name).`in`(getEventList(type))
        } else {
            Criteria.where(TOperateLog::type.name).nin(nodeEvent)
        }

        if (projectId == null) {
            criteria.and(TOperateLog::projectId.name).`in`(BK_SOFTWARE, null)
        } else {
            criteria.and(TOperateLog::projectId.name).`in`(projectId, null)
        }

        repoName?.let { criteria.and(TOperateLog::repoName.name).`is`(repoName) }

        operator?.let { criteria.and(TOperateLog::userId.name).`is`(operator) }
        if (startTime != null && endTime != null) {
            val localStart = LocalDateTime.parse(startTime, formatter)
            val localEnd = LocalDateTime.parse(endTime, formatter)
            criteria.and(TOperateLog::createdDate.name).gte(localStart).lte(localEnd)
        }
        if (startTime != null && endTime == null) {
            val localStart = LocalDateTime.parse(startTime, formatter)
            criteria.and(TOperateLog::createdDate.name).gte(localStart)
        }
        if (startTime == null && endTime != null) {
            val localEnd = LocalDateTime.parse(endTime, formatter)
            criteria.and(TOperateLog::createdDate.name).lte(localEnd)
        }

        return Query(criteria).with(Sort.by(TOperateLog::createdDate.name).descending())
    }

    private fun transferEventType(eventType: EventType): OperateType? {
        return when (eventType) {
            EventType.VERSION_DOWNLOAD -> OperateType.DOWNLOAD
            EventType.VERSION_CREATED -> OperateType.CREATE
            EventType.VERSION_UPDATED -> OperateType.UPDATE
            EventType.VERSION_DELETED -> OperateType.DELETE
            EventType.VERSION_STAGED -> OperateType.STAGE
            else -> null
        }
    }

    companion object {
        private val projectEvent = listOf(EventType.PROJECT_CREATED)
        private val repositoryEvent = listOf(EventType.REPO_CREATED, EventType.REPO_UPDATED, EventType.REPO_DELETED)
        private val packageEvent = listOf(
            EventType.VERSION_CREATED, EventType.VERSION_DELETED,
            EventType.VERSION_DOWNLOAD, EventType.VERSION_UPDATED, EventType.VERSION_STAGED
        )
        private val nodeEvent = listOf(
            EventType.NODE_CREATED, EventType.NODE_DELETED, EventType.NODE_MOVED,
            EventType.NODE_RENAMED, EventType.NODE_COPIED
        )
        private val adminEvent = listOf(EventType.ADMIN_ADD, EventType.ADMIN_DELETE)
        private val metadataEvent = listOf(EventType.METADATA_SAVED, EventType.METADATA_DELETED)
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz")
    }
}
