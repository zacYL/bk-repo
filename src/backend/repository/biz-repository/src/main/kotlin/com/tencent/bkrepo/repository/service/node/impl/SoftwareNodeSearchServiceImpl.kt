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
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.service.node.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.constant.PUBLIC_PROXY_PROJECT
import com.tencent.bkrepo.common.metadata.dao.node.NodeDao
import com.tencent.bkrepo.common.metadata.model.TNode
import com.tencent.bkrepo.common.metadata.search.node.NodeQueryContext
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.repo.RepoListOption
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.pojo.software.NodeOverviewResponse
import com.tencent.bkrepo.repository.search.software.node.SoftwareNodeQueryInterpreter
import com.tencent.bkrepo.repository.service.node.SoftwareNodeSearchService
import com.tencent.bkrepo.repository.service.repo.SoftwareRepositoryService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 节点自定义查询服务实现类
 */
@Suppress("UNCHECKED_CAST")
@Service
class SoftwareNodeSearchServiceImpl(
    private val nodeDao: NodeDao,
    private val softwareNodeQueryInterpreter: SoftwareNodeQueryInterpreter,
    private val softwareRepositoryService: SoftwareRepositoryService
) : SoftwareNodeSearchService {

    override fun search(queryModel: QueryModel): Page<Map<String, Any?>> {
        val rules = (queryModel.rule as Rule.NestedRule).rules
        var repoName: String? = null
        var projectId: String? = null
        var repoType: String? = null
        val otherField = mutableListOf<Rule>()
        for (rule in rules) {
            val queryRule = rule as Rule.QueryRule
            when (queryRule.field) {
                "repoName" -> repoName = queryRule.value as String
                "projectId" -> projectId = queryRule.value as String
                "repoType" -> repoType = queryRule.value as String
                else -> otherField.add(rule)
            }
        }
        val option = RepoListOption(type = repoType)
        if (projectId == null) {
            val projectsRule = mutableListOf<Rule>()
            val allSoftRepo = softwareRepositoryService.listRepo(option = option, includeGeneric = false)
            val projectMap = transRepoTree(allSoftRepo)
            projectMap.map { project ->
                val projectRule = Rule.QueryRule(field = "projectId", value = project.key)
                val reposRule = Rule.QueryRule(field = "repoName", value = project.value, operation = OperationType.IN)
                val targetRule = otherField.apply {
                    this.add(projectRule)
                    this.add(reposRule)
                }
                val rule = Rule.NestedRule(
                    targetRule, Rule.NestedRule.RelationType.AND
                )
                projectsRule.add(rule)
            }
            rules.apply {
                this.clear()
                this.addAll(projectsRule)
            }
        }
        if (projectId != null && repoName == null) {
            val genericRepos =
                softwareRepositoryService.listRepo(projectId, option = option, includeGeneric = false).map { it.name }
            rules.add(Rule.QueryRule(field = "repoName", value = genericRepos, operation = OperationType.IN))
        }
        queryModel.rule = Rule.NestedRule(rules, Rule.NestedRule.RelationType.OR)
        val context = softwareNodeQueryInterpreter.interpret(queryModel) as NodeQueryContext
        return doQuery(context)
    }

    override fun nodeOverview(projectId: String, name: String): NodeOverviewResponse {
        TODO()
    }

    private fun doQuery(context: NodeQueryContext): Page<Map<String, Any?>> {
        val query = context.mongoQuery
        val nodeList = nodeDao.find(query, MutableMap::class.java) as List<MutableMap<String, Any?>>
        // metadata格式转换，并排除id字段
        nodeList.forEach {
            it.remove("_id")
            it[NodeInfo::createdDate.name]?.let { createDate ->
                it[TNode::createdDate.name] = convertDateTime(createDate)
            }
            it[NodeInfo::lastModifiedDate.name]?.let { lastModifiedDate ->
                it[TNode::lastModifiedDate.name] = convertDateTime(lastModifiedDate)
            }
            it[NodeInfo::metadata.name]?.let { metadata ->
                it[NodeInfo::metadata.name] = convert(metadata as List<Map<String, Any>>)
            }
        }
        val pageNumber = if (query.limit == 0) 0 else (query.skip / query.limit).toInt()
        return Page(pageNumber + 1, query.limit, -1, nodeList)
    }

    private fun transRepoTree(repos: List<RepositoryInfo>): Map<String, List<String>> {
        val projectMap = mutableMapOf<String, MutableList<String>>()
        repos.filter { it.projectId != PUBLIC_PROXY_PROJECT }.map {
            if (projectMap.containsKey(it.projectId)) {
                projectMap[it.projectId]?.add(it.name)
            } else {
                projectMap[it.projectId] = mutableListOf(it.name)
            }
        }
        return projectMap
    }

    companion object {
        fun convert(metadataList: List<Map<String, Any>>): Map<String, Any> {
            return metadataList.filter { it.containsKey("key") && it.containsKey("value") }
                .map { it.getValue("key").toString() to it.getValue("value") }
                .toMap()
        }

        fun convertDateTime(value: Any): LocalDateTime? {
            return if (value is Date) {
                LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault())
            } else null
        }
    }
}
