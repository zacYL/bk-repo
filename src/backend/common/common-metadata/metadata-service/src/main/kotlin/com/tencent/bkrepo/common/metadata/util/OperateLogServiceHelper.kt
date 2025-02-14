package com.tencent.bkrepo.common.metadata.util

import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import com.tencent.bkrepo.common.api.util.EscapeUtils
import com.tencent.bkrepo.common.api.util.TimeUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.metadata.model.TOperateLog
import com.tencent.bkrepo.common.metadata.model.TOperateLog.Companion.DESCRIPTION_KEY_FAIL_REASON
import com.tencent.bkrepo.common.metadata.pojo.log.OpLogListOption
import com.tencent.bkrepo.common.metadata.pojo.log.OperateLog
import com.tencent.bkrepo.common.metadata.pojo.log.OperateLogResponse
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.util.AntPathMatcher
import java.time.LocalDateTime

object OperateLogServiceHelper {

    val repositoryEvent = listOf(
        EventType.REPO_CREATED.name, EventType.REPO_UPDATED.name, EventType.REPO_DELETED.name
    )
    val packageEvent = listOf(
        EventType.VERSION_CREATED.name, EventType.VERSION_DELETED.name,
        EventType.VERSION_DOWNLOAD.name, EventType.VERSION_UPDATED.name, EventType.VERSION_STAGED.name
    )
    val nodeEvent = listOf(
        EventType.NODE_CREATED.name, EventType.NODE_DELETED.name, EventType.NODE_MOVED.name,
        EventType.NODE_RENAMED.name, EventType.NODE_COPIED.name
    )
    val adminEvent = listOf(EventType.ADMIN_ADD.name, EventType.ADMIN_DELETE.name)
    val projectEvent = listOf(EventType.PROJECT_CREATED.name)
    val metadataEvent = listOf(EventType.METADATA_SAVED.name, EventType.METADATA_DELETED.name)
    val vulRuleEvent = listOf(
        EventType.VUL_RULE_ADD.name, EventType.VUL_RULE_ADD_BATCH.name,
        EventType.VUL_RULE_REMOVE.name, EventType.VUL_RULE_REMOVE_BATCH.name
    )
    val eventTypes = ArrayList<String>().apply {
        addAll(repositoryEvent)
        addAll(packageEvent)
        addAll(projectEvent)
        addAll(metadataEvent)
        addAll(vulRuleEvent)
        add(EventType.REPOSITORY_CLEAN.name)
    }
    val antPathMatcher = AntPathMatcher()

    fun buildLog(
        event: ArtifactEvent,
        address: String
    ): TOperateLog {
        val log = TOperateLog(
            type = event.type.name,
            resourceKey = event.resourceKey,
            projectId = event.projectId,
            repoName = event.repoName,
            description = event.data,
            userId = event.userId,
            clientAddress = address
        )
        return log
    }

    fun OpLogListOption.buildListQuery(): Query {
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
        return query
    }

    fun buildOperateLogPageQuery(
        type: String?,
        eventType: List<String>?,
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

    fun getEventList(resourceType: String): List<String> {
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

    fun transfer(tOperateLog: TOperateLog): OperateLog {
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

    fun convert(operateLog: OperateLog) = with(operateLog) {
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

    fun convert(tOperateLog: TOperateLog): OperateLogResponse {
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

    /**
     * 获取事件名称
     *
     * @param type 事件类型
     * @return [type]对应的名称，没有对应名称时返回[type]
     */
    fun eventName(type: String): String {
        return try {
            LocaleMessageUtils.getLocalizedMessage(EventType.valueOf(type).msgKey)
        } catch (_: IllegalArgumentException) {
            type
        }
    }

    fun match(
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
}
