/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.metadata.search.common

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction.READ
import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.metadata.config.RepositoryProperties
import com.tencent.bkrepo.common.metadata.service.node.NodePermissionService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.interceptor.QueryRuleInterceptor
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Rule.NestedRule
import com.tencent.bkrepo.common.query.model.Rule.NestedRule.RelationType
import com.tencent.bkrepo.common.query.model.Rule.QueryRule
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.UserAuthPathOption
import org.springframework.context.annotation.Conditional
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
@Conditional(SyncCondition::class)
class ProjectIdRuleInterceptor(
    private val nodePermissionService: NodePermissionService,
    private val repositoryService: RepositoryService,
    private val repositoryProperties: RepositoryProperties,
) : QueryRuleInterceptor {

    override fun match(rule: Rule): Boolean {
        return rule is QueryRule && rule.field == PROJECT_ID && isSupportRule(rule)
    }

    override fun intercept(rule: Rule, context: QueryContext): Criteria {
        require(rule is QueryRule)
        with(rule) {
            require(context is CommonQueryContext)
            val projectId = rule.value.toString()
            if (HttpContextHolder.getRequestOrNull() == null ||
                SecurityUtils.isServiceRequest() || projectId in repositoryProperties.excludeProjectLists
            )
                return Criteria.where(PROJECT_ID).isEqualTo(projectId)
            val repoName = context.findRepoName().toMutableList()
            val userId = SecurityUtils.getUserId()
            if (repoName.isEmpty()) {
                repoName.addAll(repositoryService.listRepo(projectId).map { it.name })
            }

            val userAuthPath =
                nodePermissionService.getUserAuthPathCache(UserAuthPathOption(userId, projectId, repoName, READ))

            val projectIdRule = QueryRule(NodeInfo::projectId.name, projectId, OperationType.EQ).toFixed()

            if (userAuthPath.isEmpty()) {
                val neProjectIdRule = QueryRule(NodeInfo::projectId.name, projectId, OperationType.NE).toFixed()
                return context.interpreter.resolveRule(
                    NestedRule(
                        mutableListOf(projectIdRule, neProjectIdRule),
                        RelationType.AND
                    ), context
                )
            }

            val reposNestedRule = userAuthPath.map { (repoName, authPaths) ->
                val allAncestorFolders = mutableSetOf<String>()
                // 授权路径
                val repoAuthPathRules = authPaths.map { authPath ->
                    val ancestorFolder = PathUtils.resolveAncestorFolder(authPath)
                    allAncestorFolders.addAll(ancestorFolder)
                    allAncestorFolders.add(PathUtils.toFullPath(authPath))
                    QueryRule(NodeInfo::path.name, authPath, OperationType.PREFIX).toFixed()
                }

                // 授权路径的祖先文件夹
                val authPathAncestorFolderRules = allAncestorFolders.map { authPathAncestorFolder ->
                    QueryRule(NodeInfo::fullPath.name, authPathAncestorFolder, OperationType.EQ).toFixed()
                }

                val repoAuthPathNestedRule = NestedRule(
                    repoAuthPathRules.plus(authPathAncestorFolderRules).toMutableList(), RelationType.OR
                )
                // 当前仓库查询条件
                val repoNameRule = QueryRule(NodeInfo::repoName.name, repoName, OperationType.EQ)

                NestedRule(
                    mutableListOf(repoNameRule, repoAuthPathNestedRule), RelationType.AND
                )
            }

            val nestedRule = NestedRule(
                reposNestedRule.toMutableList(), RelationType.OR
            )

            return context.interpreter.resolveRule(
                NestedRule(mutableListOf(projectIdRule, nestedRule), RelationType.AND), context
            )
        }
    }

    private fun isSupportRule(rule: QueryRule): Boolean {
        with(rule) {
            return if (operation == OperationType.EQ) {
                value is CharSequence
            } else {
                false
            }
        }
    }

}
