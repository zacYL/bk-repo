package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.RepositoryCleanStrategy
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RepoCleanUtils {

    private val logger: Logger = LoggerFactory.getLogger(RepoCleanUtils::class.java)
    fun flattenRule(cleanStrategy: RepositoryCleanStrategy): Map<Regex, Rule.NestedRule>? {
        val rule = cleanStrategy.rule ?: return null
        val reverseRule = (rule as Rule.NestedRule).rules.filterIsInstance<Rule.NestedRule>().firstOrNull()
            ?: return null
        val pathRules = reverseRule.rules.filterIsInstance<Rule.NestedRule>()
        val flattenMap = pathRules.associateBy {
            val eachRules = it.rules.filterIsInstance<Rule.QueryRule>()
            val path = (eachRules.firstOrNull { eachRule -> eachRule.field == "path" }!!.value) as String
            val pathRegexStr = path.removeSuffix("/") + "/.*"
            Regex(pathRegexStr)
        }
        return flattenMap.toSortedMap(compareByDescending { it.pattern.length })
    }

    fun needReserve(
        nodeInfo: NodeInfo,
        flattenRules: Map<Regex, Rule.NestedRule>,
    ): Boolean {
        // 找到离节点最近的规则
        var matchRule: Rule.NestedRule? = null
        for (pathRule in flattenRules) {
            if (nodeInfo.fullPath.matches(pathRule.key)) {
                matchRule = pathRule.value
                break
            }
        }
        // 取最新时间
        if (logger.isDebugEnabled) {
            logger.debug("nodeInfo:[$nodeInfo]")
            logger.debug("matchRule:[${matchRule?.toJsonString()}]")
        }
        val lastModifiedTimeDate = mutableListOf(
            LocalDateTime.parse(nodeInfo.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME),
            LocalDateTime.parse(nodeInfo.createdDate, DateTimeFormatter.ISO_DATE_TIME)
        ).apply {
            nodeInfo.recentlyUseDate?.let { this.add(LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)) }
        }.maxOf { it }
        val days = Duration.between(lastModifiedTimeDate, LocalDateTime.now()).toDays()
        // todo matchRule 是否可能为空，考虑如果为空附默认值
        val rules = matchRule!!.rules.filterIsInstance<Rule.NestedRule>().first().rules
        if (rules.isNullOrEmpty()) {
            return true
        } else {
            rules.forEach { queryRule ->
                queryRule as Rule.QueryRule
                if (queryRule.field == "name") {
                    val ruleValue = queryRule.value as String
                    val type = queryRule.operation
                    return checkReverseRule(nodeInfo.name, ruleValue, type)
                }
                if (queryRule.field.startsWith("metadata.")) {
                    val key = queryRule.field
                    val ruleValue = queryRule.value as String
                    val type = queryRule.operation
                    nodeInfo.nodeMetadata?.first { it.key == key }?.let { metadata ->
                        val metadataValue = metadata.value as String
                        return checkReverseRule(metadataValue, ruleValue, type)
                    }
                }
            }
            // 该文件夹下的规则都不符合，判断是否过期
            val reverseDays = matchRule.rules.filterIsInstance<Rule.QueryRule>()
                .firstOrNull { it.field == "reserveDays" }?.value as? Long ?: 30L
            return reverseDays >= days
        }
    }

    private fun checkReverseRule(nodeValue: String, ruleValue: String, type: OperationType): Boolean {
        return when (type) {
            OperationType.EQ -> {
                nodeValue == ruleValue
            }

            OperationType.MATCH -> {
                nodeValue.contains(ruleValue)
            }

            OperationType.REGEX -> {
                nodeValue.matches(ruleValue.toRegex())
            }
            else -> false
        }
    }
}
