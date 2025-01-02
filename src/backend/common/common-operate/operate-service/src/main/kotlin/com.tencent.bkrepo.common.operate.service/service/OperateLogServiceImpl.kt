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

package com.tencent.bkrepo.common.operate.service.service

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.EscapeUtils
import com.tencent.bkrepo.common.api.util.TimeUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.operate.api.OperateLogService
import com.tencent.bkrepo.common.operate.api.pojo.OpLogListOption
import com.tencent.bkrepo.common.operate.api.pojo.OperateLog
import com.tencent.bkrepo.common.operate.api.pojo.OperateLogResponse
import com.tencent.bkrepo.common.operate.api.pojo.event.EventCreateRequest
import com.tencent.bkrepo.common.operate.service.config.OperateProperties
import com.tencent.bkrepo.common.operate.service.dao.OperateLogDao
import com.tencent.bkrepo.common.operate.service.dao.OperateLogMigrateDao
import com.tencent.bkrepo.common.operate.service.model.TOperateLog
import com.tencent.bkrepo.common.operate.service.model.TOperateLog.Companion.DESCRIPTION_KEY_FAIL_REASON
import com.tencent.bkrepo.common.operate.service.model.TOperateLogMig
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Async
import org.springframework.util.AntPathMatcher
import java.time.LocalDateTime

/**
 * OperateLogService 实现类
 */

open class OperateLogServiceImpl(
    private val operateProperties: OperateProperties,
    private val operateLogDao: OperateLogDao,
    private val permissionManager: PermissionManager,
    private val operatorLogMigrateDao: OperateLogMigrateDao
) : OperateLogService {

    @Async
    override fun saveEventAsync(event: ArtifactEvent, address: String) {
        if (notNeedRecord(event.type.name, event.projectId, event.repoName)) {
            return
        }
        val log = TOperateLog(
            type = event.type,
            resourceKey = event.resourceKey,
            projectId = event.projectId,
            repoName = event.repoName,
            description = event.data,
            userId = event.userId,
            clientAddress = address
        )
        operateLogDao.insert(log)
    }

    override fun save(operateLog: OperateLog) {
        with(operateLog) {
            if (notNeedRecord(this.type.name, projectId, repoName)) {
                return
            }
            operateLogDao.insert(convert(operateLog))
        }
    }

    override fun save(operateLogs: Collection<OperateLog>) {
        val logs = ArrayList<TOperateLog>(operateLogs.size)
        for (operateLog in operateLogs) {
            if (notNeedRecord(operateLog.type.name, operateLog.projectId, operateLog.repoName)) {
                continue
            }
            logs.add(convert(operateLog))
        }

        if (logs.isNotEmpty()) {
            operateLogDao.insert(logs)
        }
    }

    @Async
    override fun saveAsync(operateLog: OperateLog) {
        save(operateLog)
    }

    @Async
    override fun saveAsync(operateLogs: Collection<OperateLog>) {
        save(operateLogs)
    }

    @Async
    override fun saveEventsAsync(eventList: List<ArtifactEvent>, address: String) {
        val logs = mutableListOf<TOperateLog>()
        eventList.forEach {
            if (notNeedRecord(it.type.name, it.projectId, it.repoName)) {
                return@forEach
            }
            logs.add(
                TOperateLog(
                    type = it.type,
                    resourceKey = it.resourceKey,
                    projectId = it.projectId,
                    repoName = it.repoName,
                    description = it.data,
                    userId = it.userId,
                    clientAddress = address
                )
            )
        }
        if (logs.isNotEmpty()) {
            operateLogDao.insert(logs)
        }
    }

    override fun listPage(option: OpLogListOption): Page<OperateLog> {
        try {
            permissionManager.checkPrincipal(SecurityUtils.getUserId(), PrincipalType.ADMIN)
        } catch (e: PermissionException) {
            permissionManager.checkProjectPermission(PermissionAction.MANAGE, option.projectId)
        }
        with(option) {
            val escapeValue = EscapeUtils.escapeRegexExceptWildcard(resourceKey)
            val regexPattern = escapeValue.replace("*", ".*")
            val criteria = where(TOperateLog::projectId).isEqualTo(projectId)
                .and(TOperateLog::repoName).isEqualTo(repoName)
                .and(TOperateLog::type).isEqualTo(eventType)
                .and(TOperateLog::createdDate).gte(startTime).lte(endTime)
                .and(TOperateLog::resourceKey).regex("^$regexPattern")
                .apply {
                    userId?.run { and(TOperateLog::userId).isEqualTo(userId) }
                    sha256?.run { and("${TOperateLog::description.name}.sha256").isEqualTo(sha256) }
                    pipelineId?.run { and("${TOperateLog::description.name}.pipelineId").isEqualTo(pipelineId) }
                    buildId?.run { and("${TOperateLog::description.name}.buildId").isEqualTo(buildId) }
                }
            val query = Query(criteria)
            val totalCount = operateLogDao.count(query)
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val sort = Sort.by(Sort.Direction.valueOf(direction.toString()), TOperateLog::createdDate.name)
            val records = operateLogDao.find(query.with(pageRequest).with(sort)).map { transfer(it) }
            return Pages.ofResponse(pageRequest, totalCount, records)
        }
    }

    override fun page(
        type: String?,
        eventType: List<EventType>?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<OperateLogResponse?> {
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        if (!eventType.isNullOrEmpty()) {
            require(eventTypes().containsAll(eventType)) {
                throw ParameterInvalidException("event type [$eventType] not support")
            }
        }
        if (type != null && !eventType.isNullOrEmpty()) {
            require(getEventList(type).containsAll(eventType)) {
                throw ParameterInvalidException("event type [$eventType] not support for [$type]")
            }
        }
        val query = buildOperateLogPageQuery(type, eventType, projectId, repoName, operator, startTime, endTime)
        val totalRecords = operateLogDao.count(query)
        val records = operateLogDao.find(query.with(pageRequest)).map { convert(it) }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun eventTypes(): List<EventType> {
        return mutableListOf<EventType>().apply {
            addAll(repositoryEvent)
            addAll(packageEvent)
            addAll(projectEvent)
            addAll(metadataEvent)
            addAll(vulRuleEvent)
            add(EventType.REPOSITORY_CLEAN)
        }
    }

    @Async
    override fun saveEventRequest(request: EventCreateRequest) {
        val log = TOperateLog(
            type = request.type,
            resourceKey = request.resourceKey,
            projectId = request.projectId,
            repoName = request.repoName,
            description = request.data,
            userId = request.userId,
            clientAddress = request.address
        )
        operateLogDao.save(log)
    }

    override fun opLogsMigrate() {
        while (true) {
            val logs = operatorLogMigrateDao.find(
                Query(Criteria.where(TOperateLogMig::projectId.name).ne(null)).limit(2)
            ).onEach { log ->
                val operateLog = TOperateLog(
                    type = log.type,
                    resourceKey = log.resourceKey,
                    projectId = log.projectId,
                    repoName = log.repoName,
                    description = log.description,
                    userId = log.userId,
                    clientAddress = log.clientAddress
                )
                operateLogDao.save(operateLog)
                operatorLogMigrateDao.removeById(log.id!!)
            }
            if (logs.isEmpty()) break
        }
    }

    private fun notNeedRecord(type: String, projectId: String?, repoName: String?): Boolean {
        val projectRepoKey = "$projectId/$repoName"
        if (match(operateProperties.eventType, type)) {
            return true
        }
        if (match(operateProperties.projectRepoKey, projectRepoKey)) {
            return true
        }
        return false
    }

    private fun match(
        rule: List<String>,
        value: String
    ): Boolean {
        rule.forEach {
            val match = if (it.contains("*")) {
                antPathMatcher.match(it, value)
            } else {
                it == value
            }
            if (match) {
                return true
            }
        }
        return false
    }

    private fun buildOperateLogPageQuery(
        type: String?,
        eventType: List<EventType>?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?
    ): Query {
        val criteria = if (!eventType.isNullOrEmpty()) {
            Criteria.where(TOperateLog::type.name).`in`(eventType)
        } else if (type != null) {
            Criteria.where(TOperateLog::type.name).`in`(getEventList(type))
        } else {
            Criteria.where(TOperateLog::type.name).nin(nodeEvent)
        }
        projectId?.let { criteria.and(TOperateLog::projectId.name).`is`(projectId) }
        repoName?.let { criteria.and(TOperateLog::repoName.name).`is`(repoName) }
        operator?.let { criteria.and(TOperateLog::userId.name).`is`(operator) }

        val localStart = if (!startTime.isNullOrBlank()) {
            TimeUtils.toSystemZoneTime(startTime)
        } else {
            TimeUtils.toSystemZoneTime(LocalDateTime.now().minusMonths(1))
        }
        val localEnd = if (!endTime.isNullOrBlank()) {
            TimeUtils.toSystemZoneTime(endTime)
        } else {
            TimeUtils.toSystemZoneTime(LocalDateTime.now())
        }
        require(localStart.isBefore(localEnd)) { throw IllegalArgumentException("startTime must before endTime") }
        criteria.and(TOperateLog::createdDate.name).gte(localStart).lte(localEnd)
        return Query(criteria).with(Sort.by(TOperateLog::createdDate.name).descending())
    }

    private fun getEventList(resourceType: String): List<EventType> {
        return when (resourceType) {
            "PROJECT" -> repositoryEvent
            "PACKAGE" -> packageEvent
            "ADMIN" -> adminEvent
            "REPO" -> repositoryEvent
            "METADATA" -> metadataEvent
            "VUL_RULE" -> vulRuleEvent
            else -> throw ParameterInvalidException("resource type $resourceType not support")
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
        } else if (projectEvent.contains(tOperateLog.type)) {
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId!!,
                resKey = tOperateLog.projectId!!
            )
        } else if (metadataEvent.contains(tOperateLog.type)) {
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId,
                repoType = RepositoryType.GENERIC.name,
                resKey = "${tOperateLog.repoName}::${tOperateLog.resourceKey}",
                des = tOperateLog.description.toJsonString()
            )
        } else {
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId,
                resKey = tOperateLog.resourceKey,
                des = tOperateLog.description.toJsonString()
            )
        }
        return OperateLogResponse(
            createdDate = tOperateLog.createdDate,
            operate = eventName(tOperateLog.type),
            userId = tOperateLog.userId,
            clientAddress = tOperateLog.clientAddress,
            result = tOperateLog.result,
            content = content,
            failReason = if (tOperateLog.result) null else tOperateLog.description[DESCRIPTION_KEY_FAIL_REASON] as? String
        )
    }

    private fun transfer(tOperateLog: TOperateLog): OperateLog {
        with(tOperateLog) {
            return OperateLog(
                createdDate = createdDate,
                type = type,
                projectId = projectId,
                repoName = repoName,
                resourceKey = resourceKey,
                userId = userId,
                clientAddress = clientAddress,
                description = description
            )
        }
    }

    private fun convert(operateLog: OperateLog) = with(operateLog) {
        TOperateLog(
            type = type,
            resourceKey = resourceKey,
            projectId = projectId,
            repoName = repoName,
            description = description,
            userId = userId,
            clientAddress = clientAddress,
            result = result
        )
    }

    /**
     * 获取事件名称
     *
     * @param type 事件类型
     * @return [type]对应的名称，没有对应名称时返回[type]
     */
    private fun eventName(type: String): String {
        return try {
            LocaleMessageUtils.getLocalizedMessage(EventType.valueOf(type).msgKey)
        } catch (_: IllegalArgumentException) {
            type
        }
    }

    override fun eventName(type: EventType): String {
        return try {
            LocaleMessageUtils.getLocalizedMessage(type.msgKey)
        } catch (_: IllegalArgumentException) {
            type.name
        }
    }

    companion object {
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
        private val projectEvent = listOf(EventType.PROJECT_CREATED)
        private val metadataEvent = listOf(EventType.METADATA_SAVED, EventType.METADATA_DELETED)
        private val vulRuleEvent = listOf(
            EventType.VUL_RULE_ADD, EventType.VUL_RULE_ADD_BATCH,
            EventType.VUL_RULE_REMOVE, EventType.VUL_RULE_REMOVE_BATCH
        )
        private val antPathMatcher = AntPathMatcher()
    }
}
